package com.consola.launcher.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SpectrumTest {

    // 1024 bytes de FFT a 48 kHz: 512 bandas de ~47 Hz cada una.
    private val captureSize = 1024
    private val rate = 48_000_000
    private val binHz = 48_000f / captureSize

    private fun fftAt(vararg tones: Pair<Float, Int>): ByteArray {
        val fft = ByteArray(captureSize)
        tones.forEach { (hz, magnitude) ->
            val bin = (hz / binHz).toInt()
            fft[2 * bin] = magnitude.toByte()
        }
        return fft
    }

    @Test
    fun `el silencio deja todas las barras en cero`() {
        val raw = Spectrum.raw(ByteArray(captureSize), rate)
        assertEquals(Spectrum.BARS, raw.size)
        assertArrayEquals(FloatArray(Spectrum.BARS), SpectrumGain().normalize(raw), 0f)
    }

    @Test
    fun `un bajo sube las barras de la izquierda y un platillo las de la derecha`() {
        val bass = Spectrum.raw(fftAt(80f to 100), rate)
        val cymbal = Spectrum.raw(fftAt(9_000f to 100), rate)

        assertTrue(bass.take(4).max() > 0f)
        assertEquals(0f, bass.takeLast(4).max())
        assertTrue(cymbal.takeLast(6).max() > 0f)
        assertEquals(0f, cymbal.take(4).max())
    }

    @Test
    fun `la misma cancion fuerte o baja se dibuja igual`() {
        val loudGain = SpectrumGain()
        val quietGain = SpectrumGain()
        val loud = Spectrum.raw(fftAt(80f to 120, 1_000f to 60, 6_000f to 30), rate)
        val quiet = Spectrum.raw(fftAt(80f to 40, 1_000f to 20, 6_000f to 10), rate)

        // Unas cuantas lecturas para que cada uno encuentre su pico.
        var a = FloatArray(0)
        var b = FloatArray(0)
        repeat(10) {
            a = loudGain.normalize(loud)
            b = quietGain.normalize(quiet)
        }

        for (i in a.indices) assertTrue("barra $i: ${a[i]} vs ${b[i]}", abs(a[i] - b[i]) < 0.1f)
    }

    @Test
    fun `el ruido de fondo con la musica en pausa no mueve las barras`() {
        // La mezcla de la tele nunca está en cero absoluto: un soplido de magnitud 2-3 no es música.
        val gain = SpectrumGain()
        val hiss = Spectrum.raw(fftAt(80f to 3, 1_000f to 2, 6_000f to 2), rate)
        repeat(20) { assertArrayEquals(FloatArray(Spectrum.BARS), gain.normalize(hiss), 0f) }
    }

    @Test
    fun `nada pasa de 1`() {
        val gain = SpectrumGain()
        val values = gain.normalize(Spectrum.raw(fftAt(80f to 127, 500f to 127, 5_000f to 127), rate))
        assertTrue(values.all { it in 0f..1f })
    }
}
