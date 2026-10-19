package com.consola.launcher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsCodecTest {

    @Test
    fun `los favoritos ida y vuelta conservan el orden`() {
        val favorites = listOf("com.netflix.ninja", "com.google.android.youtube.tv")

        assertEquals(favorites, SettingsCodec.decodeFavorites(SettingsCodec.encodeFavorites(favorites)))
    }

    @Test
    fun `sin favoritos guardados devuelve lista vacia`() {
        assertEquals(emptyList<String>(), SettingsCodec.decodeFavorites(""))
    }

    @Test
    fun `la aurora ida y vuelta`() {
        assertEquals(BackgroundChoice.Aurora, SettingsCodec.decodeBackground(SettingsCodec.encodeBackground(BackgroundChoice.Aurora)))
    }

    @Test
    fun `un video ida y vuelta conserva el nombre aunque tenga dos puntos`() {
        val choice = BackgroundChoice.Video("itachi:remix.mp4")

        assertEquals(choice, SettingsCodec.decodeBackground(SettingsCodec.encodeBackground(choice)))
    }

    @Test
    fun `un valor desconocido se interpreta como automatico`() {
        assertNull(SettingsCodec.decodeBackground("cualquier-cosa"))
        assertNull(SettingsCodec.decodeBackground(null))
    }
}
