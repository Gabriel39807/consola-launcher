package com.consola.launcher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import com.consola.launcher.settings.VisualizerPlace
import com.consola.launcher.ui.ClockPlacement
import com.consola.launcher.ui.LiveClock
import com.consola.launcher.ui.TvSafeArea
import com.consola.launcher.widgets.NowPlayingPill

private val Shadowed = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 16f)

/** Minimal: la hora en grande en el centro y las apps chiquitas abajo. Lo demás es tu fondo. */
@Composable
internal fun MinimalHome(model: HomeModel, callbacks: HomeCallbacks, reorder: ReorderState) {
    // TODO en UNA columna con espacios flexibles arriba y abajo: reloj, música, widgets y apps nunca se pisan,
    // estén prendidos los widgets que estén. Si sobra lugar, el reloj queda centrado como siempre.
    Column(Modifier.fillMaxSize().padding(bottom = TvSafeArea.Vertical), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LiveClock(
                style = model.settings.clock,
                placement = ClockPlacement.Hero,
                horizontalAlignment = Alignment.CenterHorizontally,
                shadow = Shadowed,
            )
            if (model.settings.widgets.nowPlaying) {
                Spacer(Modifier.height(16.dp))
                NowPlayingPill(state = model.nowPlaying, onAction = callbacks.onMediaAction, backdrop = musicBackdrop(model))
            }
            InfoSlot(model, Modifier.padding(top = 16.dp))
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MoveHint(reorder.movingKey != null, Modifier.height(24.dp))
            FavoritesLazyRow(
                reorder = reorder,
                model = model,
                callbacks = callbacks,
                tileWidth = 128.dp,
                tileHeight = 72.dp,
                onFocused = callbacks.onFocused,
                // Centradas: con pocas apps, quedan en el medio y no pegadas a la izquierda.
                arrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            )
        }
    }
}
