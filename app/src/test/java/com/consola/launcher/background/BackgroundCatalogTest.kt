package com.consola.launcher.background

import com.consola.launcher.settings.BackgroundChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackgroundCatalogTest {

    // Más nuevo primero, como los devuelve la biblioteca.
    private val videos = listOf("nuevo.mp4", "viejo.mp4")

    @Test
    fun `la aurora elegida no usa video aunque haya`() {
        assertNull(BackgroundCatalog.resolve(BackgroundChoice.Aurora, videos))
    }

    @Test
    fun `un video elegido que existe se usa`() {
        assertEquals("viejo.mp4", BackgroundCatalog.resolve(BackgroundChoice.Video("viejo.mp4"), videos))
    }

    @Test
    fun `un video elegido que se borro cae al automatico`() {
        assertEquals("nuevo.mp4", BackgroundCatalog.resolve(BackgroundChoice.Video("borrado.mp4"), videos))
    }

    @Test
    fun `sin eleccion usa el video mas nuevo`() {
        assertEquals("nuevo.mp4", BackgroundCatalog.resolve(null, videos))
    }

    @Test
    fun `sin eleccion y sin videos queda la aurora`() {
        assertNull(BackgroundCatalog.resolve(null, emptyList()))
    }
}
