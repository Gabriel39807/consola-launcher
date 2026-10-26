package com.consola.launcher.upload

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import com.consola.launcher.R
import com.consola.launcher.apps.CoverStore
import com.consola.launcher.apps.InstalledApp
import com.consola.launcher.apps.InstalledAppsSource
import com.consola.launcher.settings.LauncherSettings
import com.consola.launcher.background.BackgroundCatalog
import com.consola.launcher.background.BackgroundLibrary
import com.consola.launcher.settings.BackgroundChoice
import com.consola.launcher.settings.SettingsRepository
import com.consola.launcher.weather.WeatherSource
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

sealed interface UploadStatus {
    data object Off : UploadStatus
    data object NoNetwork : UploadStatus
    data object Waiting : UploadStatus
    data class Receiving(val percent: Int) : UploadStatus
    data class Optimizing(val percent: Int) : UploadStatus
    data class Done(val fileName: String) : UploadStatus
    data class Failed(val message: String) : UploadStatus
}

data class UploadUiState(
    val url: String? = null,
    val qr: ImageBitmap? = null,
    val status: UploadStatus = UploadStatus.Off,
)

/**
 * Coordina el camino completo de un fondo nuevo: recibir desde el celular → optimizar → guardar → elegirlo.
 * Uno por app (vive en el grafo): si salís de Ajustes a mitad de una conversión, la conversión sigue.
 */
class BackgroundUploads(
    context: Context,
    private val library: BackgroundLibrary,
    private val settings: SettingsRepository,
    private val apps: InstalledAppsSource,
    private val covers: CoverStore,
    private val scope: CoroutineScope,
    private val weather: WeatherSource,
) : UploadHandler {

    private val appContext = context.applicationContext
    private val transcoder = VideoTranscoder(appContext)
    private val busy = AtomicBoolean(false)
    private var server: UploadServer? = null

    private val _state = MutableStateFlow(UploadUiState())
    val state: StateFlow<UploadUiState> = _state.asStateFlow()

    // El servidor corre en sus propios hilos: le dejamos a mano los últimos ajustes en vez de bloquear esperando un Flow.
    @Volatile
    private var current: LauncherSettings = LauncherSettings.Default

    init {
        scope.launch { settings.settings.collect { current = it } }
    }

    private val page: String by lazy {
        appContext.resources.openRawResource(R.raw.upload_page).bufferedReader().use { it.readText() }
    }

    /** Abre el servidor. Toca la red: llamalo fuera del hilo principal. */
    @Synchronized
    fun start() {
        if (server != null) return
        val ip = LanAddress.current()
        if (ip == null) {
            _state.value = UploadUiState(status = UploadStatus.NoNetwork)
            return
        }
        val token = randomToken()
        val (started, port) = bindFirstFreePort(token) ?: run {
            _state.value = UploadUiState(status = UploadStatus.Failed("No se pudo abrir un puerto en la tele"))
            return
        }
        server = started
        val url = "http://$ip:$port/$token"
        // Si había una conversión en curso, mantenemos su estado en vez de pisarlo con "esperando".
        val status = if (busy.get()) _state.value.status else UploadStatus.Waiting
        _state.value = UploadUiState(url = url, qr = QrCode.render(url), status = status)
    }

    @Synchronized
    fun stop() {
        server?.stop()
        server = null
        _state.update { current -> if (busy.get()) current.copy(url = null, qr = null) else UploadUiState() }
    }

    override fun receive(originalName: String, length: Long, body: InputStream): UploadReply {
        if (length <= 0 || length > UploadRules.MAX_UPLOAD_BYTES) {
            return UploadReply.Rejected("El video tiene que pesar menos de 500 MB")
        }
        val requestedName = UploadRules.safeFileName(originalName, library.videos.value.toSet())
            ?: return UploadReply.Rejected("Ese archivo no es un video (mp4, webm, mkv o mov)")
        if (!busy.compareAndSet(false, true)) {
            return UploadReply.Rejected("Ya se está subiendo otro video. Esperá a que termine.")
        }

        val incoming = library.newTempFile(".part")
        if (incoming == null) {
            busy.set(false)
            return UploadReply.Rejected("La tele no tiene el almacenamiento disponible")
        }
        try {
            copyWithProgress(body, incoming, length) { percent -> setStatus(UploadStatus.Receiving(percent)) }
        } catch (e: IOException) {
            incoming.delete()
            busy.set(false)
            setStatus(UploadStatus.Failed("Se cortó la subida. Probá de nuevo."))
            return UploadReply.Rejected("Se cortó la subida")
        }

        // Le respondemos YA al celular; la optimización sigue en la tele y el celular la consulta en /status.
        scope.launch { finish(incoming, requestedName) }
        return UploadReply.Accepted
    }

    private suspend fun finish(incoming: File, requestedName: String) {
        var optimized: File? = null
        try {
            val info = withContext(Dispatchers.IO) { transcoder.probe(incoming) }
                ?: throw IOException("La tele no puede leer ese video")

            val finalName = if (VideoOptimizer.needsOptimization(info)) {
                setStatus(UploadStatus.Optimizing(0))
                val output = library.newTempFile(".mp4") ?: throw IOException("Sin almacenamiento")
                optimized = output
                transcoder.toTvFormat(incoming, output) { percent -> setStatus(UploadStatus.Optimizing(percent)) }
                val name = UploadRules.safeFileName(requestedName.substringBeforeLast('.') + ".mp4", library.videos.value.toSet())!!
                withContext(Dispatchers.IO) {
                    incoming.delete()
                    moveIntoLibrary(output, name)
                }
                name
            } else {
                withContext(Dispatchers.IO) { moveIntoLibrary(incoming, requestedName) }
                requestedName
            }

            withContext(Dispatchers.IO) { library.rescan() }
            // Lo que acabás de subir es lo que querés ver: lo dejamos elegido.
            settings.update { it.copy(background = BackgroundChoice.Video(finalName)) }
            setStatus(UploadStatus.Done(finalName))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            setStatus(UploadStatus.Failed(e.message ?: "No se pudo preparar el video"))
        } finally {
            incoming.delete()
            optimized?.takeIf { it.exists() }?.delete()
            busy.set(false)
        }
    }

    private fun moveIntoLibrary(file: File, name: String) {
        val target = File(file.parentFile, name)
        if (!file.renameTo(target)) throw IOException("No se pudo guardar el video")
    }

    override fun statusJson(): String {
        val json = JSONObject()
        when (val status = _state.value.status) {
            is UploadStatus.Receiving -> json.put("state", "receiving").put("percent", status.percent)
            is UploadStatus.Optimizing -> json.put("state", "optimizing").put("percent", status.percent)
            is UploadStatus.Done -> json.put("state", "done").put("file", status.fileName)
            is UploadStatus.Failed -> json.put("state", "error").put("message", status.message)
            else -> json.put("state", "waiting")
        }
        return json.toString()
    }

    override fun videosJson(): String {
        val videos = library.videos.value
        val active = BackgroundCatalog.resolve(current.background, videos)
        val list = JSONArray()
        videos.forEach { name -> list.put(JSONObject().put("name", name).put("active", name == active)) }
        return list.toString()
    }

    override fun delete(name: String): Boolean = library.delete(name)

    // ---------- Apps a tu gusto ----------

    /** Solo se pueden personalizar apps que están instaladas: un paquete inventado no llega a ningún lado. */
    private fun installedApp(packageName: String): InstalledApp? =
        apps.installedApps().firstOrNull { it.packageName == packageName && it.packageName != appContext.packageName }

    override fun appsJson(): String {
        val names = current.customNames
        val list = JSONArray()
        apps.installedApps()
            .filter { it.packageName != appContext.packageName }
            .distinctBy { it.packageName }
            .sortedBy { (names[it.packageName] ?: it.label).lowercase() }
            .forEach { app ->
                list.put(
                    JSONObject()
                        .put("package", app.packageName)
                        .put("original", app.label)
                        .put("name", names[app.packageName] ?: "")
                        .put("hasCover", covers.fileFor(app.packageName) != null),
                )
            }
        return list.toString()
    }

    override fun rename(packageName: String, name: String): Boolean {
        installedApp(packageName) ?: return false
        val clean = UploadRules.cleanText(name, UploadRules.MAX_NAME_LENGTH)
        scope.launch {
            settings.update { s ->
                // Vacío = volver al nombre original de la app.
                s.copy(customNames = if (clean.isEmpty()) s.customNames - packageName else s.customNames + (packageName to clean))
            }
        }
        return true
    }

    override fun saveCover(packageName: String, length: Long, body: InputStream): UploadReply {
        installedApp(packageName) ?: return UploadReply.Rejected("Esa app no está en la tele")
        if (length <= 0 || length > UploadRules.MAX_COVER_BYTES) return UploadReply.Rejected("La imagen tiene que pesar menos de 10 MB")
        val bytes = try {
            readExactly(body, length.toInt())
        } catch (e: IOException) {
            return UploadReply.Rejected("Se cortó la subida. Probá de nuevo.")
        }
        return if (covers.save(packageName, bytes)) UploadReply.Accepted else UploadReply.Rejected("Ese archivo no es una imagen")
    }

    override fun resetCover(packageName: String): Boolean {
        installedApp(packageName) ?: return false
        covers.delete(packageName)
        return true
    }

    // ---------- Mensaje del protector ----------

    override fun messageJson(): String = JSONObject().put("text", current.dreamMessage).toString()

    override fun setMessage(text: String): Boolean {
        val clean = UploadRules.cleanText(text, UploadRules.MAX_MESSAGE_LENGTH)
        scope.launch { settings.update { it.copy(dreamMessage = clean) } }
        return true
    }

    // ---------- Widgets: ciudad del clima y cuenta regresiva ----------

    override fun widgetsJson(): String = JSONObject()
        .put("city", current.weatherPlace?.name.orEmpty())
        .put("countdownTitle", current.countdown?.title.orEmpty())
        .put("countdownDate", current.countdown?.date?.toString().orEmpty())
        .toString()

    // Corre en el hilo del servidor (no en el de la pantalla): puede esperar a Open-Meteo sin trabar nada.
    override fun setCity(name: String): String {
        val clean = UploadRules.cleanText(name, UploadRules.MAX_CITY_LENGTH)
        if (clean.isEmpty()) {
            scope.launch { settings.update { it.copy(weatherPlace = null) } }
            return JSONObject().put("ok", true).put("city", "").toString()
        }
        val place = weather.findCity(clean)
            ?: return JSONObject().put("ok", false).put("message", "No encontré \"$clean\". Probá con otro nombre.").toString()
        scope.launch { settings.update { it.copy(weatherPlace = place) } }
        return JSONObject().put("ok", true).put("city", place.name).toString()
    }

    override fun setCountdown(title: String, date: String): Boolean {
        val countdown = when (val input = UploadRules.countdownInput(title, date)) {
            CountdownInput.Clear -> null
            CountdownInput.Invalid -> return false
            is CountdownInput.Valid -> input.countdown
        }
        scope.launch { settings.update { it.copy(countdown = countdown) } }
        return true
    }

    private fun readExactly(input: InputStream, length: Int): ByteArray {
        val bytes = ByteArray(length)
        var read = 0
        while (read < length) {
            val count = input.read(bytes, read, length - read)
            if (count < 0) throw IOException("El celular cortó la conexión")
            read += count
        }
        return bytes
    }

    private fun setStatus(status: UploadStatus) = _state.update { it.copy(status = status) }

    private fun bindFirstFreePort(token: String): Pair<UploadServer, Int>? {
        for (port in PORTS) {
            val candidate = UploadServer(port, token, page, this)
            try {
                candidate.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                return candidate to port
            } catch (e: IOException) {
                // Puerto ocupado por otra app: probamos el siguiente.
            }
        }
        return null
    }

    private fun copyWithProgress(input: InputStream, target: File, length: Long, onPercent: (Int) -> Unit) {
        target.outputStream().buffered().use { output ->
            val buffer = ByteArray(BUFFER_SIZE)
            var copied = 0L
            var lastPercent = -1
            while (copied < length) {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), length - copied).toInt())
                if (read < 0) throw IOException("El celular cortó la conexión")
                output.write(buffer, 0, read)
                copied += read
                val percent = (copied * 100 / length).toInt()
                if (percent != lastPercent) {
                    lastPercent = percent
                    onPercent(percent)
                }
            }
        }
    }

    private fun randomToken(): String {
        val random = SecureRandom()
        return (1..TOKEN_LENGTH).map { TOKEN_ALPHABET[random.nextInt(TOKEN_ALPHABET.length)] }.joinToString("")
    }

    private companion object {
        val PORTS = 8080..8089
        const val BUFFER_SIZE = 64 * 1024
        const val TOKEN_LENGTH = 6
        // Sin 0/o ni 1/l: si alguien tiene que tipear la dirección, no se confunde.
        const val TOKEN_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789"
    }
}
