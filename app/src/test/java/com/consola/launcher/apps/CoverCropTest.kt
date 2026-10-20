package com.consola.launcher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverCropTest {

    @Test
    fun `una imagen 16 9 no se recorta`() {
        assertEquals(CropArea(0, 0, 1920, 1080), CoverCrop.centerCrop(1920, 1080))
    }

    @Test
    fun `una foto vertical se recorta arriba y abajo, centrada`() {
        // 1080 de ancho → 607 de alto en 16:9; sobran 1313 → 656 arriba.
        assertEquals(CropArea(0, 656, 1080, 607), CoverCrop.centerCrop(1080, 1920))
    }

    @Test
    fun `una imagen muy ancha se recorta a los costados`() {
        // 900 de alto → 1600 de ancho en 16:9; sobran 1400 → 700 a cada lado.
        assertEquals(CropArea(700, 0, 1600, 900), CoverCrop.centerCrop(3000, 900))
    }

    @Test
    fun `un cuadrado se recorta arriba y abajo`() {
        assertEquals(CropArea(0, 109, 500, 281), CoverCrop.centerCrop(500, 500))
    }
}
