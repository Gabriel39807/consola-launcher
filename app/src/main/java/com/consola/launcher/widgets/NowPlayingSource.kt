package com.consola.launcher.widgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Track(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val artist: String?,
    val art: ImageBitmap?,
    val accentArgb: Int?,
    val isPlaying: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    /** SystemClock.elapsedRealtime() del momento en que se midió [positionMs]: con eso calculamos el avance. */
    val positionUpdatedAt: Long,
    val speed: Float,
)

sealed interface NowPlayingState {
    /** Falta el acceso a notificaciones: Android no nos deja ver qué suena. */
    data object NoAccess : NowPlayingState

    /** Nada sonando. Si hay una app de música instalada, la sugerimos para abrirla con un clic. */
    data class Idle(val suggestedPackage: String?, val suggestedLabel: String?) : NowPlayingState

    data class Active(val track: Track) : NowPlayingState
}

/**
 * "¿Qué está sonando?" para cualquier app: Spotify, YouTube Music, YouTube... Spotify no le da esa
 * información a otras apps directamente, pero TODAS las apps de música publican una sesión de medios
 * para que el control remoto y el sistema puedan manejarlas. Leemos esa sesión.
 *
 * Todo corre en el hilo principal: los callbacks de MediaController llegan ahí.
 */
class NowPlayingSource(context: Context) {

    private val appContext = context.applicationContext
    private val sessions = appContext.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(appContext, MediaListener::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<NowPlayingState>(idle())
    val state: StateFlow<NowPlayingState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var started = false
    // Inicio y protector de pantalla pueden escuchar a la vez: se apaga cuando lo suelta el último.
    private var users = 0
    private var cachedArtKey: String? = null
    private var cachedArt: Pair<ImageBitmap?, Int?> = null to null

    fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(appContext).contains(appContext.packageName)

    fun start() {
        users++
        if (started) return
        if (!hasAccess()) {
            _state.value = NowPlayingState.NoAccess
            return
        }
        try {
            sessions.addOnActiveSessionsChangedListener(sessionsChanged, listenerComponent, mainHandler)
            started = true
            choose(sessions.getActiveSessions(listenerComponent))
        } catch (e: SecurityException) {
            _state.value = NowPlayingState.NoAccess
        }
    }

    fun stop() {
        users = (users - 1).coerceAtLeast(0)
        if (users > 0 || !started) return
        sessions.removeOnActiveSessionsChangedListener(sessionsChanged)
        attach(null)
        started = false
    }

    fun perform(action: MediaAction) = when (action) {
        MediaAction.PlayPause -> playPause()
        MediaAction.Next -> next()
        MediaAction.Previous -> previous()
        MediaAction.OpenPlayer -> openPlayer()
    }

    fun playPause() {
        val current = controller ?: return
        if (current.playbackState?.state == PlaybackState.STATE_PLAYING) {
            current.transportControls.pause()
        } else {
            current.transportControls.play()
        }
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    /**
     * Abre la app que está sonando, o la sugerida si no suena nada. Abrimos la app directamente en vez de
     * usar su sessionActivity: ese atajo lo crea la app de música estando en segundo plano, y desde
     * Android 14 el sistema puede bloquear que abra una pantalla.
     */
    fun openPlayer() {
        val packageName = controller?.packageName ?: (state.value as? NowPlayingState.Idle)?.suggestedPackage ?: return
        launch(packageName)
    }

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { list -> choose(list.orEmpty()) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() {
            if (started) choose(sessions.getActiveSessions(listenerComponent))
        }
    }

    // Preferimos lo que está sonando; si nada suena, lo último en pausa. Sesiones sin datos
    // (como la de Bluetooth desconectado de esta tele) no cuentan.
    private fun choose(list: List<MediaController>) {
        val usable = list.filter { it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null }
        val best = usable.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: usable.firstOrNull()
        attach(best)
        publish()
    }

    private fun attach(newController: MediaController?) {
        if (newController?.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(callback)
        controller = newController
        newController?.registerCallback(callback, mainHandler)
    }

    private fun publish() {
        val current = controller
        val metadata = current?.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        if (current == null || metadata == null || title == null) {
            _state.value = idle()
            return
        }
        val playback = current.playbackState
        val (art, accent) = artFor(current.packageName, title, metadata)
        _state.value = NowPlayingState.Active(
            Track(
                packageName = current.packageName,
                appLabel = labelOf(current.packageName),
                title = title,
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
                art = art,
                accentArgb = accent,
                isPlaying = playback?.state == PlaybackState.STATE_PLAYING,
                durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
                positionMs = playback?.position ?: 0L,
                positionUpdatedAt = playback?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
                speed = playback?.playbackSpeed ?: 0f,
            ),
        )
    }

    // La carátula llega en cada callback (cada pausa, cada salto): la convertimos una vez por canción.
    private fun artFor(packageName: String, title: String, metadata: MediaMetadata): Pair<ImageBitmap?, Int?> {
        val source = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val key = "$packageName|$title|${source?.generationId}"
        if (key == cachedArtKey) return cachedArt

        // Sin carátula, usamos el ícono de la app: el widget nunca queda con un hueco.
        val bitmap = source ?: runCatching { appContext.packageManager.getApplicationIcon(packageName).toBitmap(ART_SIZE_PX, ART_SIZE_PX) }.getOrNull()
        val small = bitmap?.let { softwareCopy(it) }
        val accent = small?.let { Palette.from(it).maximumColorCount(16).generate() }
            ?.let { (it.vibrantSwatch ?: it.dominantSwatch)?.rgb }
        cachedArtKey = key
        cachedArt = small?.asImageBitmap() to accent
        return cachedArt
    }

    // Palette necesita leer píxeles: un bitmap de hardware no se puede leer, así que copiamos y achicamos.
    private fun softwareCopy(bitmap: Bitmap): Bitmap {
        val readable = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        return Bitmap.createScaledBitmap(readable, ART_SIZE_PX, ART_SIZE_PX, true)
    }

    private fun idle(): NowPlayingState.Idle {
        val player = SUGGESTED_PLAYERS.firstOrNull { appContext.packageManager.getLeanbackLaunchIntentForPackage(it) != null }
        return NowPlayingState.Idle(player, player?.let(::labelOf))
    }

    private fun labelOf(packageName: String): String = runCatching {
        val info = appContext.packageManager.getApplicationInfo(packageName, 0)
        appContext.packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    private fun launch(packageName: String) {
        val intent = appContext.packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: return
        appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private companion object {
        const val ART_SIZE_PX = 192
        val SUGGESTED_PLAYERS = listOf("com.spotify.tv.android", "com.google.android.youtube.tvmusic")
    }
}
