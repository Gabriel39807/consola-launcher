package com.consola.launcher.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class CropArea(val left: Int, val top: Int, val width: Int, val height: Int)

/** El recorte 16:9 centrado de cualquier imagen. Puro: se testea sin Android. */
object CoverCrop {
    fun centerCrop(width: Int, height: Int): CropArea {
        val targetHeight = width * 9 / 16
        return if (targetHeight <= height) {
            CropArea(0, (height - targetHeight) / 2, width, targetHeight)
        } else {
            val targetWidth = height * 16 / 9
            CropArea((width - targetWidth) / 2, 0, targetWidth, height)
        }
    }
}

/**
 * Las portadas propias que subís desde el celular. Viven en la memoria privada de la app (no en la
 * carpeta de fondos): son pocas, chicas, y no tienen por qué verse desde afuera.
 */
class CoverStore(context: Context) {

    private val folder = File(context.applicationContext.filesDir, "covers").apply { mkdirs() }
    private val _version = MutableStateFlow(0)

    /** Cambia cada vez que se guarda o borra una portada: quien muestra apps se entera y recarga. */
    val version: StateFlow<Int> = _version.asStateFlow()

    fun fileFor(packageName: String): File? = File(folder, "$packageName.png").takeIf { it.isFile }

    /**
     * Guarda [bytes] como portada de [packageName]: recortada a 16:9 y achicada a 640×360 (el doble del
     * banner de Android TV: nítida aunque el tile se agrande con el foco). false si no es una imagen.
     */
    fun save(packageName: String, bytes: ByteArray): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

        // Una foto de celular de 12 MP decodificada entera ocupa ~48 MB: la achicamos YA al leerla.
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= WIDTH_PX && bounds.outHeight / (sample * 2) >= HEIGHT_PX) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return false

        val crop = CoverCrop.centerCrop(decoded.width, decoded.height)
        val cropped = Bitmap.createBitmap(decoded, crop.left, crop.top, crop.width, crop.height)
        val scaled = Bitmap.createScaledBitmap(cropped, WIDTH_PX, HEIGHT_PX, true)
        File(folder, "$packageName.png").outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        _version.value++
        return true
    }

    fun delete(packageName: String) {
        if (File(folder, "$packageName.png").delete()) _version.value++
    }

    private companion object {
        const val WIDTH_PX = 640
        const val HEIGHT_PX = 360
    }
}
