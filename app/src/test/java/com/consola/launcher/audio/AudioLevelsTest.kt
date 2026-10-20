package com.consola.launcher.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioLevelsTest {

    // 256 bytes de FFT a 44,1 kHz: 128 bandas de 172 Hz cada una.
    private val captureSize = 256
    private val samplingRateMilliHz = 44_100_000

    private fun fftWith(vararg bins: Pair<Int, Int>): ByteArray {
        val fft = ByteArray(captureSize)
        bins.forEach { (bin, magnitude) -> fft[2 * bin] = magnitude.toByte() }
        return fft
    }

    @Test
    fun `el silencio no mueve nada`() {
        val levels = AudioLevels.bands(ByteArray(captureSize), samplingRateMilliHz)

        assertEquals(Bands(0f, 0f, 0f), levels)
    }

    @Test
    fun `un golpe de bajo sube los graves y no los agudos`() {
        // La banda 1 está en 172 Hz: zona de graves.
        val levels = AudioLevels.bands(fftWith(1 to 100), samplingRateMilliHz)

        assertTrue(levels.bass > 0.5f)
        assertEquals(0f, levels.treble)
    }

    @Test
    fun `un platillo sube los agudos`() {
        // La banda 30 está en ~5,2 kHz: zona de agudos.
        val levels = AudioLevels.bands(fftWith(30 to 100), samplingRateMilliHz)

        assertTrue(levels.treble > 0f)
        assertEquals(0f, levels.bass)
    }

    @Test
    fun `los niveles nunca pasan de 1`() {
        val levels = AudioLevels.bands(fftWith(1 to 127, 5 to 127, 30 to 127), samplingRateMilliHz)

        assertTrue(levels.bass <= 1f && levels.mid <= 1f && levels.treble <= 1f)
    }

    @Test
    fun `suavizado sube rapido y baja lento`() {
        val up = AudioLevels.smooth(previous = 0f, target = 1f)
        val down = AudioLevels.smooth(previous = 1f, target = 0f)

        // Sube más de la mitad de golpe (se siente el beat)...
        assertTrue(up > 0.5f)
        // ...y baja de a poco (no parpadea).
        assertTrue(down > 0.7f)
    }
}
