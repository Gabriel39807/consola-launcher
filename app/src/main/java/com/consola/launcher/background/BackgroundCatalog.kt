package com.consola.launcher.background

import com.consola.launcher.settings.BackgroundChoice

object BackgroundCatalog {

    /**
     * Qué video reproducir según lo elegido y lo que hay en la carpeta, o null para la aurora.
     * [videos] viene del más nuevo al más viejo: "automático" = lo último que subiste.
     */
    fun resolve(choice: BackgroundChoice?, videos: List<String>): String? = when (choice) {
        BackgroundChoice.Aurora -> null
        // Si el video elegido se borró desde el celular, no dejamos la pantalla negra: caemos al automático.
        is BackgroundChoice.Video -> choice.fileName.takeIf { it in videos } ?: videos.firstOrNull()
        null -> videos.firstOrNull()
    }
}
