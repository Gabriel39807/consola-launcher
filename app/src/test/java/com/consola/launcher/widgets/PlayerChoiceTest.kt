package com.consola.launcher.widgets

import com.consola.launcher.settings.LauncherSettings
import com.consola.launcher.settings.PreferredPlayer
import com.consola.launcher.settings.Themes
import com.consola.launcher.settings.WidgetSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerChoiceTest {

    private val spotifyPaused = SessionInfo(Players.SPOTIFY, isPlaying = false, hasTitle = true)
    private val musicPaused = SessionInfo(Players.YOUTUBE_MUSIC, isPlaying = false, hasTitle = true)
    private val musicPlaying = SessionInfo(Players.YOUTUBE_MUSIC, isPlaying = true, hasTitle = true)

    @Test
    fun `lo que esta sonando gana siempre, aunque prefieras otro`() {
        val sessions = listOf(spotifyPaused, musicPlaying)
        assertEquals(1, PlayerChoice.pick(sessions, preferredPackage = Players.SPOTIFY))
    }

    @Test
    fun `si nada suena, se muestra el reproductor que elegiste`() {
        val sessions = listOf(spotifyPaused, musicPaused)
        assertEquals(1, PlayerChoice.pick(sessions, preferredPackage = Players.YOUTUBE_MUSIC))
        assertEquals(0, PlayerChoice.pick(sessions, preferredPackage = null))
    }

    @Test
    fun `las sesiones sin cancion no cuentan`() {
        val empty = SessionInfo("com.android.bluetooth", isPlaying = true, hasTitle = false)
        assertEquals(1, PlayerChoice.pick(listOf(empty, musicPaused), preferredPackage = null))
        assertNull(PlayerChoice.pick(listOf(empty), preferredPackage = null))
    }

    @Test
    fun `sin musica sugiere tu reproductor si esta instalado, si no el primero que haya`() {
        val both = setOf(Players.SPOTIFY, Players.YOUTUBE_MUSIC)
        assertEquals(Players.YOUTUBE_MUSIC, PlayerChoice.suggest({ it in both }, preferredPackage = Players.YOUTUBE_MUSIC))
        assertEquals(Players.SPOTIFY, PlayerChoice.suggest({ it in both }, preferredPackage = null))
        assertEquals(Players.YOUTUBE_MUSIC, PlayerChoice.suggest({ it == Players.YOUTUBE_MUSIC }, preferredPackage = Players.SPOTIFY))
        assertNull(PlayerChoice.suggest({ false }, preferredPackage = null))
    }

    @Test
    fun `cada reproductor tiene su color y los demas usan el de la caratula`() {
        assertEquals("Spotify", Players.brandFor(Players.SPOTIFY)?.name)
        assertEquals("YouTube Music", Players.brandFor(Players.YOUTUBE_MUSIC)?.name)
        assertNull(Players.brandFor("com.netflix.ninja"))
    }

    @Test
    fun `aplicar un tema no te cambia el reproductor elegido`() {
        val mine = LauncherSettings(favorites = emptyList(), hidden = emptySet(), background = null, widgets = WidgetSettings(player = PreferredPlayer.YouTubeMusic))
        val themed = Themes.builtIn.first().applyTo(mine)
        assertEquals(PreferredPlayer.YouTubeMusic, themed.widgets.player)
    }
}
