package com.consola.launcher.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoGainTest {

    @Test
    fun `el silencio sigue siendo cero`() {
        val gain = AutoGain()

        assertEquals(0f, gain.normalize(0f))
    }

    @Test
    fun `el golpe mas fuerte reciente vale 1`() {
        val gain = AutoGain()

        assertEquals(1f, gain.normalize(80f))
    }

    @Test
    fun `despues de un golpe fuerte, lo mas suave queda abajo y hay contraste`() {
        val gain = AutoGain()
        gain.normalize(80f)

        val soft = gain.normalize(20f)

        assertTrue("un sonido a 1/4 del pico tiene que quedar bastante abajo: $soft", soft < 0.3f)
    }

    @Test
    fun `con la tele fuerte o bajita late igual`() {
        val loud = AutoGain()
        val quiet = AutoGain()

        // La misma canción, una al cuádruple de volumen que la otra.
        listOf(80f, 40f, 80f, 40f).forEach { loud.normalize(it) }
        listOf(20f, 10f, 20f, 10f).forEach { quiet.normalize(it) }

        assertEquals(loud.normalize(40f), quiet.normalize(10f), 0.01f)
    }

    @Test
    fun `si la musica baja, con el tiempo el pico se adapta`() {
        val gain = AutoGain()
        gain.normalize(100f)

        // ~20 lecturas por segundo: tras unos 15 segundos de música más suave, esa música vuelve a "llenar".
        repeat(300) { gain.normalize(30f) }

        assertTrue(gain.normalize(30f) > 0.9f)
    }
}
