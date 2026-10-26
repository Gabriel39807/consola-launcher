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
    val font: FontChoice,
) {
    /**
     * Del tema se toma el LOOK de los widgets: música, reloj grande y estilo del visualizador. Lo tuyo no se toca:
     * tu reproductor, dónde va el visualizador (y si se ve en el protector), el clima, la cuenta regresiva y el calendario.
     */
    fun applyTo(settings: LauncherSettings): LauncherSettings = settings.copy(
        layout = layout,
        clock = clock,
        widgets = settings.widgets.copy(nowPlaying = widgets.nowPlaying, bigClock = widgets.bigClock, visualizer = widgets.visualizer),
        font = font,
        tuning = settings.tuning.copy(dim = dim),
    )

    companion object {
        fun from(settings: LauncherSettings, id: String, name: String) = Theme(
            id = id,
            name = name,
            layout = settings.layout,
            clock = settings.clock,
            widgets = WidgetSettings(
                nowPlaying = settings.widgets.nowPlaying,
                bigClock = settings.widgets.bigClock,
                visualizer = settings.widgets.visualizer,
            ),
            dim = settings.tuning.dim,
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
            dim = DimLevel.None, font = FontChoice.Montserrat,
        ),
        Theme(
            id = "gamer", name = "Gamer", layout = HomeLayout.Xbox,
            clock = ClockStyle(weight = ClockWeight.Bold),
            widgets = WidgetSettings(nowPlaying = true, visualizer = VisualizerStyle.Bars),
            dim = DimLevel.Low, font = FontChoice.Orbitron,
        ),
        Theme(
            id = "zen", name = "Zen", layout = HomeLayout.Minimal,
            clock = ClockStyle(weight = ClockWeight.Light, size = ClockSize.Large, date = DateStyle.Weekday),
            widgets = WidgetSettings(nowPlaying = true, visualizer = VisualizerStyle.Wave), dim = DimLevel.Medium, font = FontChoice.Playfair,
        ),
        Theme(
            id = "arcade", name = "Arcade", layout = HomeLayout.Ps5,
            clock = ClockStyle(use24h = false, weight = ClockWeight.Bold),
            widgets = WidgetSettings(nowPlaying = true, bigClock = false, visualizer = VisualizerStyle.Pulse),
            dim = DimLevel.None, font = FontChoice.Bebas,
        ),
    )
}
