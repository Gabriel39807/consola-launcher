package com.consola.launcher.audio

import kotlin.math.max

/**
 * Control automático de ganancia: mide cada valor en relación al pico RECIENTE en vez de contra un número fijo.
 *
 * En esta tele el Visualizer mide el audio después del volumen: con la tele fuerte todo saturaba en 1 (el fondo
 * quedaba "prendido" sin latir) y con la tele baja casi no se movía. Así, late igual a cualquier volumen.
 *
 * Además eleva al cuadrado: lo que está cerca del pico (el golpe) se destaca y lo demás baja. Eso es lo que
 * hace que se SIENTA el beat. Puro: se testea sin audio real.
 */
class AutoGain(
    // ~20 lecturas por segundo: el pico se reduce a la mitad en ~7 segundos si la música baja.
    private val decay: Float = 0.995f,
    // Por debajo de esto es ruido, no música: evita que el silencio "infle" a 1 cualquier soplido.
    private val noiseFloor: Float = 4f,
) {
    private var peak = noiseFloor

    fun normalize(raw: Float): Float {
        peak = max(raw, peak * decay).coerceAtLeast(noiseFloor)
        val relative = (raw / peak).coerceIn(0f, 1f)
        return relative * relative
    }
}
