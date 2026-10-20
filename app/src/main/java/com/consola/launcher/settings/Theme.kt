package com.consola.launcher.settings

/**
 * Un "look" completo: estilo, reloj, widgets, cómo se ve el fondo y la letra.
 * NO incluye tus apps, tus fondos ni tus nombres: cambiar de tema nunca te desordena el Inicio.
 */
data class Theme(
    val id: String,
    val name: String,
    val layout: HomeLayout,
    val clock: ClockStyle,
    val widgets: WidgetSettings,
    val dim: DimLevel,
    val musicReactive: Boolean,
    val font: FontChoice,
) {
    fun applyTo(settings: LauncherSettings): LauncherSettings = settings.copy(
        layout = layout,
        clock = clock,
        widgets = widgets,
        font = font,
        tuning = settings.tuning.copy(dim = dim, musicReactive = musicReactive),
    )

    companion object {
        fun from(settings: LauncherSettings, id: String, name: String) = Theme(
            id = id,
            name = name,
            layout = settings.layout,
            clock = settings.clock,
            widgets = settings.widgets,
            dim = settings.tuning.dim,
            musicReactive = settings.tuning.musicReactive,
            font = settings.font,
        )
    }
}

/** Los temas que vienen con la app: un punto de partida para quien no quiere tocar nada. */
object Themes {
    val builtIn = listOf(
        Theme(
            id = "cine", name = "Cine", layout = HomeLayout.Cinema,
            clock = ClockStyle(), widgets = WidgetSettings(nowPlaying = true),
            dim = DimLevel.None, musicReactive = false, font = FontChoice.Montserrat,
        ),
        Theme(
            id = "gamer", name = "Gamer", layout = HomeLayout.Xbox,
            clock = ClockStyle(weight = ClockWeight.Bold), widgets = WidgetSettings(nowPlaying = true),
            dim = DimLevel.Low, musicReactive = true, font = FontChoice.Orbitron,
        ),
        Theme(
            id = "zen", name = "Zen", layout = HomeLayout.Minimal,
            clock = ClockStyle(weight = ClockWeight.Light, size = ClockSize.Large, date = DateStyle.Weekday),
            widgets = WidgetSettings(nowPlaying = true), dim = DimLevel.Medium, musicReactive = false, font = FontChoice.Playfair,
        ),
        Theme(
            id = "arcade", name = "Arcade", layout = HomeLayout.Ps5,
            clock = ClockStyle(use24h = false, weight = ClockWeight.Bold), widgets = WidgetSettings(nowPlaying = true, bigClock = false),
            dim = DimLevel.None, musicReactive = true, font = FontChoice.Bebas,
        ),
    )
}
