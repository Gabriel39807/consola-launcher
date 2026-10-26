package com.consola.launcher.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.settings.WidgetSettings
import com.consola.launcher.weather.Condition
import com.consola.launcher.weather.Temperature
import com.consola.launcher.weather.WeatherState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val TileShape = RoundedCornerShape(20.dp)
// Casi opaco: las ondas que pasan por detrás no ensucian lo que dice el widget.
private val TileColor = Color(0xFF1C2130).copy(alpha = 0.92f)
private val TileHeight = 120.dp
private val Dim = Color.White.copy(alpha = 0.65f)

/** Los widgets de información (clima, cuenta regresiva, calendario) en fila, del mismo alto. No toman el foco. */
@Composable
fun InfoWidgetsRow(
    widgets: WidgetSettings,
    weather: WeatherState,
    countdown: Countdown?,
    modifier: Modifier = Modifier,
) {
    if (!widgets.hasInfoWidgets) return
    // Cada minuto el Inicio se vuelve a armar (por la hora): "hoy" se actualiza solo a la medianoche.
    val today = LocalDate.now()
    // 136 + 136 + 148 + 2 × 10 = 440: el mismo ancho que la tarjeta de música, así quedan en una columna prolija.
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (widgets.weather) WeatherTile(weather, widgets.fahrenheit)
        if (widgets.countdown) CountdownTile(countdown, today)
        if (widgets.calendar) CalendarTile(today)
    }
}

@Composable
private fun Tile(width: androidx.compose.ui.unit.Dp, padding: androidx.compose.ui.unit.Dp = 12.dp, content: @Composable () -> Unit) {
    Box(
        Modifier
            .width(width)
            .height(TileHeight)
            .clip(TileShape)
            .background(TileColor)
            .padding(padding),
    ) { content() }
}

@Composable
private fun WeatherTile(state: WeatherState, fahrenheit: Boolean) = Tile(136.dp) {
    when (state) {
        is WeatherState.Ready -> Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emojiFor(state.weather.condition, state.weather.isDay), fontSize = 26.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    Temperature.format(state.weather.temperatureC, fahrenheit),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(stringResource(labelFor(state.weather.condition)), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Text(
                "↑${Temperature.format(state.weather.maxC, fahrenheit)}  ↓${Temperature.format(state.weather.minC, fahrenheit)}",
                style = MaterialTheme.typography.labelMedium,
                color = Dim,
            )
            Spacer(Modifier.weight(1f))
            Text(state.place.name, style = MaterialTheme.typography.labelSmall, color = Dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        WeatherState.Loading -> Placeholder("⛅", stringResource(R.string.weather_loading))
        WeatherState.Unavailable -> Placeholder("☁️", stringResource(R.string.weather_unavailable))
    }
}

@Composable
private fun CountdownTile(countdown: Countdown?, today: LocalDate) = Tile(136.dp) {
    if (countdown == null) {
        Placeholder("⏳", stringResource(R.string.countdown_empty))
        return@Tile
    }
    Column {
        Text(
            countdown.title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.5.sp,
            color = Dim,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        when (val status = CountdownStatus.of(today, countdown.date)) {
            is CountdownStatus.Upcoming -> {
                Text(stringResource(R.string.countdown_left), style = MaterialTheme.typography.labelMedium, color = Dim)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${status.days}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.countdown_days), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            CountdownStatus.Tomorrow -> Text(stringResource(R.string.countdown_tomorrow), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            CountdownStatus.Today -> Text(stringResource(R.string.countdown_today), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            is CountdownStatus.Past -> Text(stringResource(R.string.countdown_past, status.days), style = MaterialTheme.typography.titleMedium, color = Dim)
        }
    }
}

@Composable
private fun CalendarTile(today: LocalDate) = Tile(148.dp, padding = 10.dp) {
    val locale = Locale.getDefault()
    // Lunes o domingo primero, según cómo lo usa tu país.
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val weeks = MonthGrid.weeks(YearMonth.from(today), firstDay)
    Column {
        Text(
            today.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Row(Modifier.fillMaxWidth()) {
            (0 until 7).forEach { offset ->
                CalendarCell(firstDay.plus(offset.toLong()).getDisplayName(TextStyle.NARROW_STANDALONE, locale), color = Dim)
            }
        }
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == today.dayOfMonth) {
                        // Hoy: un círculo blanco. Se ve desde el sillón.
                        Box(Modifier.weight(1f).height(12.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(Color.White))
                            Text("$day", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        CalendarCell(day?.toString().orEmpty())
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CalendarCell(text: String, color: Color = Color.White) {
    Text(
        text,
        fontSize = 9.sp,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f).height(12.dp),
    )
}

@Composable
private fun Placeholder(emoji: String, text: String) {
    Column {
        Text(emoji, fontSize = 28.sp)
        Spacer(Modifier.weight(1f))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Dim, maxLines = 3)
    }
}

private fun emojiFor(condition: Condition, isDay: Boolean): String = when (condition) {
    Condition.Clear -> if (isDay) "☀️" else "🌙"
    Condition.PartlyCloudy -> if (isDay) "⛅" else "☁️"
    Condition.Cloudy -> "☁️"
    Condition.Fog -> "🌫️"
    Condition.Drizzle -> "🌦️"
    Condition.Rain -> "🌧️"
    Condition.Snow -> "❄️"
    Condition.Storm -> "⛈️"
}

private fun labelFor(condition: Condition): Int = when (condition) {
    Condition.Clear -> R.string.weather_clear
    Condition.PartlyCloudy -> R.string.weather_partly_cloudy
    Condition.Cloudy -> R.string.weather_cloudy
    Condition.Fog -> R.string.weather_fog
    Condition.Drizzle -> R.string.weather_drizzle
    Condition.Rain -> R.string.weather_rain
    Condition.Snow -> R.string.weather_snow
    Condition.Storm -> R.string.weather_storm
}
