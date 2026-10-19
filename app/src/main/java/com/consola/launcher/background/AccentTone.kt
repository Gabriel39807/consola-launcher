package com.consola.launcher.background

import kotlin.math.roundToInt

/**
 * Convierte el color de una app (el rojo de Netflix, el azul de Disney+) en un tono de FONDO:
 * mismo matiz, pero lo bastante oscuro para que el texto blanco se lea encima.
 * Trabaja con enteros ARGB a propósito: sin Android, se testea con JUnit plano.
 */
object AccentTone {

    private const val OPAQUE = 0xFF shl 24

    fun forBackground(argb: Int, maxBrightness: Float = 0.45f): Int {
        val red = argb shr 16 and 0xFF
        val green = argb shr 8 and 0xFF
        val blue = argb and 0xFF

        val brightest = maxOf(red, green, blue)
        if (brightest == 0) return OPAQUE

        // Escalar los tres canales por el MISMO factor oscurece sin cambiar el matiz.
        val factor = minOf(1f, maxBrightness * 255 / brightest)
        fun scale(channel: Int) = (channel * factor).roundToInt()

        return OPAQUE or (scale(red) shl 16) or (scale(green) shl 8) or scale(blue)
    }
}
