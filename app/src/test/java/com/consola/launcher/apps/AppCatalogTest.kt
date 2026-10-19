package com.consola.launcher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCatalogTest {

    private val self = "com.consola.launcher"

    private fun arrange(
        apps: List<InstalledApp>,
        favorites: List<String> = emptyList(),
        hidden: Set<String> = emptySet(),
    ): List<String> =
        AppCatalog.arrange(apps, selfPackage = self, favorites = favorites, hidden = hidden)
            .map { it.packageName }

    @Test
    fun `no se muestra a si mismo`() {
        val result = arrange(
            listOf(InstalledApp(self, "Consola"), InstalledApp("com.netflix.ninja", "Netflix")),
        )

        assertEquals(listOf("com.netflix.ninja"), result)
    }

    @Test
    fun `oculta las apps de la lista de ocultas`() {
        val result = arrange(
            listOf(
                InstalledApp("com.google.android.videos", "Google TV"),
                InstalledApp("com.netflix.ninja", "Netflix"),
            ),
            hidden = setOf("com.google.android.videos"),
        )

        assertEquals(listOf("com.netflix.ninja"), result)
    }

    @Test
    fun `los favoritos van primero y en el orden configurado`() {
        val result = arrange(
            listOf(
                InstalledApp("a.spotify", "Spotify"),
                InstalledApp("a.alpha", "Alpha"),
                InstalledApp("a.netflix", "Netflix"),
                InstalledApp("a.youtube", "YouTube"),
            ),
            favorites = listOf("a.youtube", "a.netflix"),
        )

        assertEquals(listOf("a.youtube", "a.netflix", "a.alpha", "a.spotify"), result)
    }

    @Test
    fun `el resto va alfabetico sin importar mayusculas`() {
        val result = arrange(
            listOf(
                InstalledApp("a.z", "zeta"),
                InstalledApp("a.b", "Beta"),
                InstalledApp("a.a", "alfa"),
            ),
        )

        assertEquals(listOf("a.a", "a.b", "a.z"), result)
    }

    @Test
    fun `una app con varias actividades de TV aparece una sola vez`() {
        val result = arrange(
            listOf(
                InstalledApp("a.youtube", "YouTube"),
                InstalledApp("a.youtube", "YouTube (otra actividad)"),
            ),
        )

        assertEquals(listOf("a.youtube"), result)
    }

    @Test
    fun `un favorito que no esta instalado se ignora`() {
        val result = arrange(
            listOf(InstalledApp("a.netflix", "Netflix")),
            favorites = listOf("no.instalada", "a.netflix"),
        )

        assertEquals(listOf("a.netflix"), result)
    }
}
