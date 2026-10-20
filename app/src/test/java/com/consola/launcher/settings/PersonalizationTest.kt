package com.consola.launcher.settings

import com.consola.launcher.background.BackgroundCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class PersonalizationTest {

    private val videos = listOf("nuevo.mp4", "medio.mp4", "viejo.mp4")

    // ---------- Fondos que cambian solos ----------

    @Test
    fun `sin rotacion manda lo elegido`() {
        val tuning = BackgroundTuning(rotation = Rotation.Off)

        assertEquals("medio.mp4", BackgroundCatalog.resolveTimed(BackgroundChoice.Video("medio.mp4"), videos, tuning, LocalTime.NOON, epochMinute = 0))
    }

    @Test
    fun `cada 30 minutos pasa al siguiente video`() {
        val tuning = BackgroundTuning(rotation = Rotation.Every30)

        assertEquals("nuevo.mp4", BackgroundCatalog.resolveTimed(null, videos, tuning, LocalTime.NOON, epochMinute = 0))
        assertEquals("medio.mp4", BackgroundCatalog.resolveTimed(null, videos, tuning, LocalTime.NOON, epochMinute = 30))
        assertEquals("nuevo.mp4", BackgroundCatalog.resolveTimed(null, videos, tuning, LocalTime.NOON, epochMinute = 90))
    }

    @Test
    fun `de dia y de noche usa el fondo marcado para cada momento`() {
        val tuning = BackgroundTuning(rotation = Rotation.DayNight, dayVideo = "nuevo.mp4", nightVideo = "viejo.mp4")

        assertEquals("nuevo.mp4", BackgroundCatalog.resolveTimed(null, videos, tuning, LocalTime.of(10, 0), epochMinute = 0))
        assertEquals("viejo.mp4", BackgroundCatalog.resolveTimed(null, videos, tuning, LocalTime.of(22, 0), epochMinute = 0))
        assertEquals("viejo.mp4", BackgroundCatalog.resolveTimed(null, videos, tuning, LocalTime.of(3, 0), epochMinute = 0))
    }

    @Test
    fun `de dia y de noche sin marcar el de noche usa la aurora de noche`() {
        val tuning = BackgroundTuning(rotation = Rotation.DayNight, dayVideo = "nuevo.mp4", nightVideo = null)

        assertNull(BackgroundCatalog.resolveTimed(null, videos, tuning, LocalTime.of(22, 0), epochMinute = 0))
    }

    @Test
    fun `con un solo video la rotacion no hace nada raro`() {
        val tuning = BackgroundTuning(rotation = Rotation.Every15)

        assertEquals("solo.mp4", BackgroundCatalog.resolveTimed(null, listOf("solo.mp4"), tuning, LocalTime.NOON, epochMinute = 45))
    }

    // ---------- Apps a tu gusto ----------

    @Test
    fun `los nombres propios ida y vuelta`() {
        val overrides = mapOf("com.netflix.ninja" to "Pelis", "com.spotify.tv.android" to "Música")

        assertEquals(overrides, SettingsCodec.decodeNames(SettingsCodec.encodeNames(overrides)))
    }

    @Test
    fun `un nombre con tabuladores o saltos de linea se limpia`() {
        val encoded = SettingsCodec.encodeNames(mapOf("a" to "Mi\tapp\nfavorita"))

        assertEquals(mapOf("a" to "Mi app favorita"), SettingsCodec.decodeNames(encoded))
    }

    @Test
    fun `sin nombres guardados no hay nada`() {
        assertEquals(emptyMap<String, String>(), SettingsCodec.decodeNames(""))
    }

    // ---------- Temas ----------

    @Test
    fun `un tema aplica estilo reloj widgets fondo y letra pero no toca tus apps`() {
        val mine = LauncherSettings(favorites = listOf("a"), hidden = setOf("b"), background = BackgroundChoice.Aurora)
        val zen = Themes.builtIn.first { it.id == "zen" }

        val result = zen.applyTo(mine)

        assertEquals(zen.layout, result.layout)
        assertEquals(zen.font, result.font)
        assertEquals(zen.clock, result.clock)
        assertEquals(listOf("a"), result.favorites)
        assertEquals(setOf("b"), result.hidden)
        assertEquals(BackgroundChoice.Aurora, result.background)
    }

    @Test
    fun `guardar mi tema copia lo que tengo puesto`() {
        val settings = LauncherSettings.Default.copy(layout = HomeLayout.Xbox, font = FontChoice.Orbitron)

        val theme = Theme.from(settings, id = "mio-1", name = "Mío")

        assertEquals(HomeLayout.Xbox, theme.layout)
        assertEquals(FontChoice.Orbitron, theme.font)
    }

    @Test
    fun `un tema guardado ida y vuelta`() {
        val theme = Theme.from(
            LauncherSettings.Default.copy(
                layout = HomeLayout.Minimal,
                font = FontChoice.Playfair,
                clock = ClockStyle(use24h = false, showSeconds = true, date = DateStyle.Weekday, weight = ClockWeight.Light, size = ClockSize.Large),
                widgets = WidgetSettings(nowPlaying = false, bigClock = true),
                tuning = BackgroundTuning(dim = DimLevel.High, musicReactive = true),
            ),
            id = "mio-1",
            name = "Noche zen",
        )

        assertEquals(theme, SettingsCodec.decodeTheme(SettingsCodec.encodeTheme(theme)))
    }

    @Test
    fun `un tema roto no rompe la app`() {
        assertNull(SettingsCodec.decodeTheme("cualquier cosa"))
    }

    @Test
    fun `marcar un fondo recorre dia noche y sin marca`() {
        val start = BackgroundTuning()

        val day = start.cycleDayNight("a.mp4")
        val night = day.cycleDayNight("a.mp4")
        val none = night.cycleDayNight("a.mp4")

        assertEquals("a.mp4", day.dayVideo)
        assertEquals(null, night.dayVideo)
        assertEquals("a.mp4", night.nightVideo)
        assertEquals(BackgroundTuning(), none)
    }
}
