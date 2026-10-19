package com.consola.launcher.background

import org.junit.Assert.assertEquals
import org.junit.Test

class AccentToneTest {

    @Test
    fun `oscurece un color brillante hasta el brillo maximo`() {
        assertEquals(0xFF730000.toInt(), AccentTone.forBackground(0xFFFF0000.toInt()))
    }

    @Test
    fun `mantiene el tono escalando todos los canales por igual`() {
        // (255, 128, 0) * 0.45 = (115, 58, 0)
        assertEquals(0xFF733A00.toInt(), AccentTone.forBackground(0xFFFF8000.toInt()))
    }

    @Test
    fun `un color que ya es oscuro no se toca`() {
        assertEquals(0xFF202020.toInt(), AccentTone.forBackground(0xFF202020.toInt()))
    }

    @Test
    fun `el negro sigue siendo negro`() {
        assertEquals(0xFF000000.toInt(), AccentTone.forBackground(0xFF000000.toInt()))
    }

    @Test
    fun `el resultado siempre es opaco`() {
        assertEquals(0xFF730000.toInt(), AccentTone.forBackground(0x80FF0000.toInt()))
    }
}
