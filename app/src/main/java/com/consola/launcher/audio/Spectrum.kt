package com.consola.launcher.audio

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * El sonido repartido en [BARS] barras, de graves (izquierda) a agudos (derecha), para el visualizador.
 *
 * Las barras están repartidas en escala LOGARÍTMICA, como el oído: de 50 a 100 Hz hay tanta "música" como de
 * 5 a 10 kHz. Con barras lineales, casi todo el movimiento quedaría amontonado en las dos primeras.
 * Puro: se testea sin Android ni audio real.
 */
object Spectrum {
    const val BARS = 24
    private const val MIN_HZ = 50f
    private const val MAX_HZ = 12_000f

    /** [fft] en el formato del Visualizer; [samplingRateMilliHz] como lo reporta el Visualizer. Sin normalizar. */
    fun raw(fft: ByteArray, samplingRateMilliHz: Int, bars: Int = BARS): FloatArray {
        val bins = fft.size / 2
        val out = FloatArray(bars)
        if (bins < 2) return out
        val binHz = samplingRateMilliHz / 1000f / fft.size
        var previousEnd = 0
        for (bar in 0 until bars) {
            val lowHz = MIN_HZ * (MAX_HZ / MIN_HZ).pow(bar.toFloat() / bars)
            val highHz = MIN_HZ * (MAX_HZ / MIN_HZ).pow((bar + 1).toFloat() / bars)
            // Cada barra toma al menos un "casillero" del FFT y nunca repite el de la anterior.
            val start = max((lowHz / binHz).roundToInt(), max(previousEnd, 1)).coerceAtMost(bins - 1)
            val end = max((highHz / binHz).roundToInt(), start + 1).coerceAtMost(bins)
            previousEnd = end
            var sum = 0f
            for (k in start until end) sum += hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat())
            // Los agudos traen mucha menos energía que los graves: los realzamos para que también se vean.
            val tilt = 1f + 1.5f * bar / (bars - 1)
            out[bar] = sum / (end - start) * tilt
        }
        return out
    }
}

/**
 * Como [AutoGain], pero para todas las barras juntas: mide contra el pico RECIENTE de la canción, así se dibuja igual
 * con la tele fuerte o baja, y conserva la forma (si los graves son el doble que los agudos, se ven el doble).
 */
class SpectrumGain(
    // ~20 lecturas por segundo: el pico baja a la mitad en ~7 s si la música se hace más suave.
    private val decay: Float = 0.995f,
    // Por debajo de esto es ruido, no música. Con la música en pausa la mezcla de la tele trae un soplido de 2-3:
    // si lo normalizáramos, las barras se moverían solas y la pantalla se redibujaría 20 veces por segundo (medido).
    private val noiseFloor: Float = 4f,
) {
    private var peak = noiseFloor

    fun normalize(raw: FloatArray): FloatArray {
        val frameMax = raw.maxOrNull() ?: 0f
        if (frameMax < noiseFloor) {
            peak = (peak * decay).coerceAtLeast(noiseFloor)
            return FloatArray(raw.size)
        }
        peak = max(frameMax, peak * decay).coerceAtLeast(noiseFloor)
        // Un poco por debajo de lineal: las barras bajas se ven, sin que todas lleguen arriba.
        return FloatArray(raw.size) { i -> (raw[i] / peak).coerceIn(0f, 1f).pow(0.85f) }
    }
}
