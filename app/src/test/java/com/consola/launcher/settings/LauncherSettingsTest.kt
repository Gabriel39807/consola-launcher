package com.consola.launcher.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherSettingsTest {

    private val base = LauncherSettings(favorites = listOf("a.youtube"), hidden = emptySet(), background = null)

    @Test
    fun `marcar favorita la agrega al final para respetar el orden`() {
        val result = base.toggleFavorite("a.netflix")

        assertEquals(listOf("a.youtube", "a.netflix"), result.favorites)
    }

    @Test
    fun `desmarcar favorita la saca`() {
        val result = base.toggleFavorite("a.youtube")

        assertEquals(emptyList<String>(), result.favorites)
    }

    @Test
    fun `una app oculta que se marca favorita deja de estar oculta`() {
        val result = base.copy(hidden = setOf("a.netflix")).toggleFavorite("a.netflix")

        assertEquals(emptySet<String>(), result.hidden)
        assertEquals(listOf("a.youtube", "a.netflix"), result.favorites)
    }

    @Test
    fun `ocultar una favorita la saca de favoritas`() {
        val result = base.toggleHidden("a.youtube")

        assertEquals(setOf("a.youtube"), result.hidden)
        assertEquals(emptyList<String>(), result.favorites)
    }

    @Test
    fun `volver a ocultar una app oculta la muestra`() {
        val result = base.copy(hidden = setOf("a.netflix")).toggleHidden("a.netflix")

        assertEquals(emptySet<String>(), result.hidden)
    }
}
