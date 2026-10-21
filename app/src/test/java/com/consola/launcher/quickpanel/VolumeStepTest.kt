package com.consola.launcher.quickpanel

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeStepTest {

    @Test
    fun `en una tele de 0 a 100 cada toque mueve 5`() {
        assertEquals(35, VolumeStep.next(current = 30, max = 100, up = true))
        assertEquals(25, VolumeStep.next(current = 30, max = 100, up = false))
    }

    @Test
    fun `en una tele de 0 a 15 cada toque mueve 1`() {
        assertEquals(8, VolumeStep.next(current = 7, max = 15, up = true))
    }

    @Test
    fun `no se pasa de los limites`() {
        assertEquals(100, VolumeStep.next(current = 98, max = 100, up = true))
        assertEquals(0, VolumeStep.next(current = 2, max = 100, up = false))
    }
}
