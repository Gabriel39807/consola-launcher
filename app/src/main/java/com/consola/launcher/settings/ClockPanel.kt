package com.consola.launcher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.consola.launcher.R
import com.consola.launcher.ui.ClockPlacement
import com.consola.launcher.ui.LiveClock

/** Cada opción se cambia con OK, una por vez, y arriba se ve el resultado al instante. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClockPanel(style: ClockStyle, onChange: ((ClockStyle) -> ClockStyle) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().focusRestorer(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item { PanelTitle(stringResource(R.string.settings_clock)) }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                LiveClock(style = style, placement = ClockPlacement.Hero, horizontalAlignment = Alignment.CenterHorizontally)
            }
        }
        item {
            SettingsRow(
                title = stringResource(R.string.clock_format),
                value = stringResource(if (style.use24h) R.string.clock_format_24 else R.string.clock_format_12),
                onClick = { onChange { it.copy(use24h = !it.use24h) } },
            )
        }
        item {
            SettingsRow(
                title = stringResource(R.string.clock_seconds),
                value = stringResource(if (style.showSeconds) R.string.yes else R.string.no),
                onClick = { onChange { it.copy(showSeconds = !it.showSeconds) } },
            )
        }
        item {
            SettingsRow(
                title = stringResource(R.string.clock_date),
                value = stringResource(style.date.label),
                onClick = { onChange { it.copy(date = it.date.next()) } },
            )
        }
        item {
            SettingsRow(
                title = stringResource(R.string.clock_weight),
                value = stringResource(style.weight.label),
                onClick = { onChange { it.copy(weight = it.weight.next()) } },
            )
        }
        item {
            SettingsRow(
                title = stringResource(R.string.clock_size),
                value = stringResource(style.size.label),
                onClick = { onChange { it.copy(size = it.size.next()) } },
            )
        }
    }
}

private val DateStyle.label: Int
    get() = when (this) {
        DateStyle.Full -> R.string.clock_date_full
        DateStyle.Short -> R.string.clock_date_short
        DateStyle.Weekday -> R.string.clock_date_weekday
        DateStyle.Hidden -> R.string.clock_date_hidden
    }

private val ClockWeight.label: Int
    get() = when (this) {
        ClockWeight.Light -> R.string.clock_weight_light
        ClockWeight.Regular -> R.string.clock_weight_regular
        ClockWeight.Bold -> R.string.clock_weight_bold
    }

private val ClockSize.label: Int
    get() = when (this) {
        ClockSize.Small -> R.string.clock_size_small
        ClockSize.Medium -> R.string.clock_size_medium
        ClockSize.Large -> R.string.clock_size_large
    }
