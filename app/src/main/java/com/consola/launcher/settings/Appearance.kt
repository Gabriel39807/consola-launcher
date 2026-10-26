package com.consola.launcher.settings

/** Cómo se arma la pantalla de Inicio. */
enum class HomeLayout { Cinema, Ps5, Xbox, Minimal, Rows }

enum class DateStyle { Full, Short, Weekday, Hidden }
enum class ClockWeight { Light, Regular, Bold }
enum class ClockSize { Small, Medium, Large }

data class ClockStyle(
    val use24h: Boolean = true,
    val showSeconds: Boolean = false,
    val date: DateStyle = DateStyle.Full,
    val weight: ClockWeight = ClockWeight.Regular,
    val size: ClockSize = ClockSize.Medium,
)

/** Cada cuánto cambia el fondo solo. DayNight usa un fondo de día (7 a 19 h) y otro de noche. */
enum class Rotation { Off, Every15, Every30, Every60, DayNight }

/** Cuánto se oscurece el fondo. Se hornea en el mismo degradé: no cuesta GPU extra. */
enum class DimLevel(val alpha: Float) { None(0f), Low(0.2f), Medium(0.35f), High(0.5f) }

data class BackgroundTuning(
    val rotation: Rotation = Rotation.Off,
    val dim: DimLevel = DimLevel.None,
    val dayVideo: String? = null,
    val nightVideo: String? = null,
) {
    /** Mantener OK sobre un fondo: sin marca → de día → de noche → sin marca. */
    fun cycleDayNight(name: String): BackgroundTuning = when (name) {
        dayVideo -> copy(dayVideo = null, nightVideo = name)
        nightVideo -> copy(nightVideo = null)
        else -> copy(dayVideo = name)
    }
}

/** Las letras incluidas en la app (todas con licencia libre OFL). System = la de la tele. */
enum class FontChoice { System, Montserrat, Orbitron, Playfair, Bebas }

/**
 * Tu reproductor de música: el que se sugiere cuando no suena nada y el que se muestra si hay varios en pausa.
 * Automático = el primero instalado (Spotify, después YouTube Music).
 */
enum class PreferredPlayer(val packageName: String?) {
    Auto(null),
    Spotify("com.spotify.tv.android"),
    YouTubeMusic("com.google.android.youtube.tvmusic"),
}

/**
 * Cómo se ve la música. Barras, Onda y Espejo dibujan el sonido; Pulse es el fondo que late con los graves
 * (lo que antes era "el fondo que baila").
 */
enum class VisualizerStyle { Off, Bars, Wave, Mirror, Pulse }

/** Dónde se dibuja el visualizador: una banda abajo a lo ancho (detrás de las apps), o dentro de la tarjeta de la música. */
enum class VisualizerPlace { AboveApps, WithMusic }

data class WidgetSettings(
    val nowPlaying: Boolean = true,
    val bigClock: Boolean = false,
    val player: PreferredPlayer = PreferredPlayer.Auto,
    val visualizer: VisualizerStyle = VisualizerStyle.Off,
    val visualizerPlace: VisualizerPlace = VisualizerPlace.AboveApps,
    val weather: Boolean = false,
    val fahrenheit: Boolean = false,
    val countdown: Boolean = false,
    val calendar: Boolean = false,
    /** Que las ondas también se dibujen en el protector de pantalla. */
    val visualizerInDream: Boolean = false,
) {
    /** El visualizador necesita escuchar el audio (todos los estilos, incluido el fondo que late). */
    val listensToAudio: Boolean get() = visualizer != VisualizerStyle.Off

    /** Barras, Onda o Espejo: algo que se dibuja como widget (el fondo que late no). */
    val drawsVisualizer: Boolean get() = visualizer != VisualizerStyle.Off && visualizer != VisualizerStyle.Pulse

    val hasInfoWidgets: Boolean get() = weather || countdown || calendar

    /** El protector escucha el audio solo si tiene algo que hacer con él: el fondo que late, o las ondas si las pediste. */
    val dreamListensToAudio: Boolean get() = visualizer == VisualizerStyle.Pulse || (drawsVisualizer && visualizerInDream)
}

/** Los patrones de java.time para cada opción del reloj. Puro: se testea sin Android. */
object ClockFormat {

    fun timePattern(style: ClockStyle): String = buildString {
        append(if (style.use24h) "HH:mm" else "h:mm")
        if (style.showSeconds) append(":ss")
        if (!style.use24h) append(" a")
    }

    fun datePattern(date: DateStyle): String? = when (date) {
        DateStyle.Full -> "EEEE d 'de' MMMM"
        DateStyle.Short -> "dd/MM/yyyy"
        DateStyle.Weekday -> "EEEE"
        DateStyle.Hidden -> null
    }
}

/** La opción siguiente, y después de la última, la primera: así se cambian con OK, una por vez. */
inline fun <reified T : Enum<T>> T.next(): T {
    val values = enumValues<T>()
    return values[(ordinal + 1) % values.size]
}
