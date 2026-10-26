package com.consola.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.settings.ClockFormat
import com.consola.launcher.settings.ClockSize
import com.consola.launcher.settings.ClockStyle
import com.consola.launcher.settings.ClockWeight
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Dónde va el reloj: en la barra de pestañas (chico) o como protagonista (Minimal, widget, protector). */
/** Bar: la barra de arriba. Hero: la hora protagonista. Compact: grande pero para lugares justos (Xbox). */
enum class ClockPlacement { Bar, Hero, Compact }

/** El único reloj de la app: todos los lugares usan este, así un cambio en Ajustes → Reloj se ve en todos. */
@Composable
fun LiveClock(
    style: ClockStyle,
    placement: ClockPlacement,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    shadow: Shadow? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(style.use24h, style.showSeconds, locale) {
        DateTimeFormatter.ofPattern(ClockFormat.timePattern(style), locale)
    }
    val dateFormat = remember(style.date, locale) {
        ClockFormat.datePattern(style.date)?.let { DateTimeFormatter.ofPattern(it, locale) }
    }
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            // Esperamos hasta el próximo segundo exacto: si no, los segundos se ven "a los saltos".
            delay(1_000 - System.currentTimeMillis() % 1_000)
        }
    }

    val (timeStyle, dateStyle) = clockTextStyles(style, placement)
    Column(modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            text = now.format(timeFormat),
            style = timeStyle.copy(shadow = shadow),
            fontWeight = style.weight.fontWeight,
        )
        dateFormat?.let { format ->
            Spacer(Modifier.height(if (placement == ClockPlacement.Bar) 2.dp else 4.dp))
            Text(
                text = now.format(format).replaceFirstChar { it.uppercase() },
                style = dateStyle.copy(shadow = shadow),
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}

private val ClockWeight.fontWeight: FontWeight
    get() = when (this) {
        ClockWeight.Light -> FontWeight.Light
        ClockWeight.Regular -> FontWeight.Medium
        ClockWeight.Bold -> FontWeight.Bold
    }

@Composable
private fun clockTextStyles(style: ClockStyle, placement: ClockPlacement): Pair<TextStyle, TextStyle> {
    val type = MaterialTheme.typography
    return when (placement) {
        ClockPlacement.Bar -> when (style.size) {
            ClockSize.Small -> type.titleMedium to type.labelSmall
            ClockSize.Medium -> type.titleLarge to type.labelMedium
            ClockSize.Large -> type.headlineMedium to type.labelLarge
        }
        ClockPlacement.Compact -> when (style.size) {
            ClockSize.Small -> type.headlineMedium to type.labelMedium
            ClockSize.Medium -> type.displaySmall to type.labelLarge
            ClockSize.Large -> type.displayMedium to type.titleSmall
        }
        ClockPlacement.Hero -> when (style.size) {
            ClockSize.Small -> type.displaySmall to type.titleMedium
            ClockSize.Medium -> type.displayLarge to type.titleLarge
            ClockSize.Large -> type.displayLarge.copy(fontSize = 96.sp, lineHeight = 104.sp) to type.headlineSmall
        }
    }
}
