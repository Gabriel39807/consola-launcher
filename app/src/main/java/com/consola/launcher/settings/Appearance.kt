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
    val musicReactive: Boolean = false,
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

data class WidgetSettings(
    val nowPlaying: Boolean = true,
    val bigClock: Boolean = false,
)

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
