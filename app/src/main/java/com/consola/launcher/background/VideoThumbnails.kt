package com.consola.launcher.background

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Una foto de cada video para las tarjetas de Ajustes → Fondos. Es I/O: llamalo fuera del hilo principal. */
class VideoThumbnails {

    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun of(file: File): ImageBitmap? {
        // La clave incluye la fecha: si reemplazás un video con el mismo nombre, la foto se actualiza.
        val key = "${file.name}:${file.lastModified()}"
        cache[key]?.let { return it }
        return extract(file)?.also { cache[key] = it }
    }

    private fun extract(file: File): ImageBitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val frame = retriever.getFrameAtTime(FRAME_AT_MICROS, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            Bitmap.createScaledBitmap(frame, WIDTH_PX, HEIGHT_PX, true).asImageBitmap()
        } catch (e: RuntimeException) {
            // Un video que la tele no puede abrir: la tarjeta se muestra sin foto.
            null
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val FRAME_AT_MICROS = 1_000_000L
        const val WIDTH_PX = 320
        const val HEIGHT_PX = 180
    }
}
