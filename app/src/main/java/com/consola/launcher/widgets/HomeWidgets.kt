package com.consola.launcher.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.consola.launcher.settings.ClockStyle
import com.consola.launcher.settings.WidgetSettings
import com.consola.launcher.ui.ClockPlacement
import com.consola.launcher.ui.LiveClock

private val ReadableShadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 12f)

/** Los widgets activados en Ajustes → Widgets. Cada estilo de Inicio decide dónde y en qué dirección van. */
@Composable
fun HomeWidgets(
    widgets: WidgetSettings,
    clock: ClockStyle,
    nowPlaying: NowPlayingState,
    onMediaAction: (MediaAction) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    cardWidth: Dp = 440.dp,
    // Las ondas "con la música" (de fondo en la tarjeta): las arma el Inicio, que tiene el sonido.
    musicBackdrop: (@Composable (Modifier) -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        if (widgets.bigClock) {
            LiveClock(style = clock, placement = ClockPlacement.Hero, horizontalAlignment = horizontalAlignment, shadow = ReadableShadow)
        }
        if (widgets.nowPlaying) NowPlayingCard(nowPlaying, onMediaAction, width = cardWidth, backdrop = musicBackdrop)
    }
    if (vertical) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = horizontalAlignment) { content() }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Bottom) { content() }
    }
}
