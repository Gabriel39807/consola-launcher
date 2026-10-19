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

    @Test
    fun `reordenar respeta el orden que se ve en pantalla`() {
        val settings = base.copy(favorites = listOf("a", "b", "c"))

        assertEquals(listOf("c", "a", "b"), settings.withVisibleOrder(listOf("c", "a", "b")).favorites)
    }

    @Test
    fun `una favorita no instalada se queda en su lugar al reordenar las visibles`() {
        // "prime" está en favoritas pero no instalada: en pantalla solo se ven yt, netflix y spotify.
        val settings = base.copy(favorites = listOf("yt", "prime", "netflix", "spotify"))

        val result = settings.withVisibleOrder(listOf("yt", "spotify", "netflix"))

        assertEquals(listOf("yt", "prime", "spotify", "netflix"), result.favorites)
    }

    @Test
    fun `un orden con apps que no son favoritas no cambia nada`() {
        val settings = base.copy(favorites = listOf("a", "b"))

        assertEquals(settings, settings.withVisibleOrder(listOf("b", "x")))
    }
}
