package com.consola.launcher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.consola.launcher.settings.VisualizerPlace
import com.consola.launcher.ui.ClockPlacement
import com.consola.launcher.ui.LiveClock
import com.consola.launcher.ui.TvSafeArea
import com.consola.launcher.widgets.NowPlayingCard

/** Cine: el nombre grande arriba a la izquierda, la música arriba a la derecha y la fila abajo. */
@Composable
internal fun CinemaHome(model: HomeModel, callbacks: HomeCallbacks, reorder: ReorderState) {
    var focused by remember { mutableStateOf<HomeTile?>(null) }
    val widgets = model.settings.widgets

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = TvSafeArea.Horizontal, top = 24.dp)
                // 45 %: deja lugar a la tarjeta de música (440 dp) sin que se pisen.
                .fillMaxWidth(0.45f),
        ) {
            FocusedTitle(focused, MaterialTheme.typography.displayLarge)
            if (widgets.bigClock) {
                Spacer(Modifier.height(16.dp))
                LiveClock(
                    style = model.settings.clock,
                    placement = ClockPlacement.Hero,
                    horizontalAlignment = Alignment.Start,
                    shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 12f),
                )
            }
        }
        Column(
            Modifier.align(Alignment.TopEnd).padding(end = TvSafeArea.Horizontal, top = 8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (widgets.nowPlaying) {
                NowPlayingCard(state = model.nowPlaying, onAction = callbacks.onMediaAction, backdrop = musicBackdrop(model))
            }
            InfoSlot(model)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(bottom = TvSafeArea.Vertical)) {
            MoveHint(reorder.movingKey != null, Modifier.padding(start = TvSafeArea.Horizontal).height(24.dp))
            FavoritesLazyRow(
                reorder = reorder,
                model = model,
                callbacks = callbacks,
                tileWidth = TileWidth,
                tileHeight = TileHeight,
                onFocused = { tile ->
                    focused = tile
                    callbacks.onFocused(tile)
                },
            )
        }
    }
}
