package com.consola.launcher.quickpanel

/**
 * Cuánto sube o baja el volumen cada toque del panel. Las teles van de 0 a 15, de 0 a 100... El paso de Android
 * es de a 1: en una de 0 a 100 harían falta 100 toques. Usamos ~20 pasos en todo el recorrido. Puro: se testea sin Android.
 */
object VolumeStep {
    private const val STEPS = 20

    fun next(current: Int, max: Int, up: Boolean): Int {
        val step = (max / STEPS).coerceAtLeast(1)
        return (if (up) current + step else current - step).coerceIn(0, max)
    }
}
