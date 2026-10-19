package com.consola.launcher.background

import android.content.Context
import android.net.Uri

/**
 * Los videos de fondo viven en la carpeta privada de la app:
 *   /sdcard/Android/data/com.consola.launcher/files/backgrounds/
 * No pide permisos y se llena por ADB (ver scripts/push-background.ps1).
 */
class BackgroundLibrary(context: Context) {

    private val appContext = context.applicationContext

    /** El primer video en orden alfabético, o null si la carpeta está vacía. */
    fun currentVideo(): Uri? =
        appContext.getExternalFilesDir(FOLDER_NAME)
            ?.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in VIDEO_EXTENSIONS }
            ?.minByOrNull { it.name }
            ?.let(Uri::fromFile)

    private companion object {
        const val FOLDER_NAME = "backgrounds"
        val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv")
    }
}
