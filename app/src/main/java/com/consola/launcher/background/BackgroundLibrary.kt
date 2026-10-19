package com.consola.launcher.background

import android.content.Context
import android.net.Uri
import com.consola.launcher.upload.UploadRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Los videos de fondo viven en la carpeta privada de la app:
 *   /sdcard/Android/data/com.consola.launcher/files/backgrounds/
 * No pide permisos. Se llena desde el celular (Ajustes → Fondos) o por ADB (scripts/push-background.ps1).
 */
class BackgroundLibrary(context: Context) {

    private val appContext = context.applicationContext
    private val _videos = MutableStateFlow<List<String>>(emptyList())

    /** Nombres de los videos, del más nuevo al más viejo. */
    val videos: StateFlow<List<String>> = _videos.asStateFlow()

    fun folder(): File? = appContext.getExternalFilesDir(FOLDER_NAME)

    /** Lee la carpeta de nuevo. Es I/O: llamalo fuera del hilo principal. */
    fun rescan() {
        _videos.value = folder()?.listFiles().orEmpty()
            // Los que empiezan con "." son archivos a medio subir o a medio convertir.
            .filter { it.isFile && !it.name.startsWith(".") && it.extension.lowercase() in UploadRules.VIDEO_EXTENSIONS }
            .sortedByDescending { it.lastModified() }
            .map { it.name }
    }

    fun fileOf(name: String): File? = folder()?.let { File(it, name) }?.takeIf { it.isFile }

    fun uriOf(name: String): Uri? = fileOf(name)?.let(Uri::fromFile)

    /** Solo borra nombres que ESTÁN en la lista: un nombre armado a mano no puede borrar otra cosa. */
    fun delete(name: String): Boolean {
        if (name !in _videos.value) return false
        val deleted = fileOf(name)?.delete() == true
        rescan()
        return deleted
    }

    /** Un archivo temporal oculto dentro de la misma carpeta: así renombrarlo al final es instantáneo. */
    fun newTempFile(suffix: String): File? =
        folder()?.let { File(it, ".incoming-${System.nanoTime()}$suffix") }

    private companion object {
        const val FOLDER_NAME = "backgrounds"
    }
}
