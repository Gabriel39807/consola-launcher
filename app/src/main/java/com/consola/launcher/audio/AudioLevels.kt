package com.consola.launcher.audio

import kotlin.math.hypot

/** Cuánta energía hay en graves, medios y agudos, de 0 a 1. */
data class Bands(val bass: Float, val mid: Float, val treble: Float)

/**
 * Convierte el análisis de frecuencias (FFT) que entrega el Visualizer de Android en tres niveles.
 * Puro: se testea sin Android ni audio real.
 */
object AudioLevels {

    private const val BASS_MAX_HZ = 250f
    private const val MID_MAX_HZ = 2_000f
    private const val TREBLE_MAX_HZ = 8_000f

    // Una banda con magnitud 64 (de ~181 posibles en 8 bits) ya es "a fondo": la música real rara vez satura.
    private const val FULL_SCALE = 64f

    /**
     * [fft] viene en el formato del Visualizer: [real0, realN/2, real1, imag1, real2, imag2, ...].
     * [samplingRateMilliHz] es la frecuencia de muestreo en miliHertz, como la reporta el Visualizer.
     */
    fun bands(fft: ByteArray, samplingRateMilliHz: Int): Bands {
        val raw = rawBands(fft, samplingRateMilliHz)
        return Bands(scale(raw.bass), scale(raw.mid), scale(raw.treble))
    }

    /** La magnitud promedio de cada zona, SIN escalar: para pasarla por un [AutoGain]. */
    fun rawBands(fft: ByteArray, samplingRateMilliHz: Int): Bands {
        val bins = fft.size / 2
        if (bins < 2) return Bands(0f, 0f, 0f)
        val binWidthHz = samplingRateMilliHz / 1000f / fft.size

        var bass = 0f
        var mid = 0f
        var treble = 0f
        var bassCount = 0
        var midCount = 0
        var trebleCount = 0
        for (k in 1 until bins) {
            val frequency = k * binWidthHz
            val magnitude = hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat())
            when {
                frequency <= BASS_MAX_HZ -> { bass += magnitude; bassCount++ }
                frequency <= MID_MAX_HZ -> { mid += magnitude; midCount++ }
                frequency <= TREBLE_MAX_HZ -> { treble += magnitude; trebleCount++ }
            }
        }
        return Bands(average(bass, bassCount), average(mid, midCount), average(treble, trebleCount))
    }

    /** Sube rápido (se siente el golpe) y baja lento (no parpadea). */
    fun smooth(previous: Float, target: Float, attack: Float = 0.6f, release: Float = 0.15f): Float {
        val speed = if (target > previous) attack else release
        return previous + (target - previous) * speed
    }

    private fun average(sum: Float, count: Int): Float = if (count == 0) 0f else sum / count

    private fun scale(raw: Float): Float = (raw / FULL_SCALE).coerceIn(0f, 1f)
}
