package com.consola.launcher.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.consola.launcher.ui.TvSafeArea
import com.consola.launcher.widgets.HomeWidgets

// Íconos más chicos que en Cine, y el enfocado crece bastante más: así se siente la barra de la PS5.
private val Ps5TileWidth = 128.dp
private val Ps5TileHeight = 72.dp

/** PS5: la fila de íconos arriba, el nombre grande debajo y los widgets al pie. */
@Composable
internal fun Ps5Home(model: HomeModel, callbacks: HomeCallbacks, reorder: ReorderState) {
    var focused by remember { mutableStateOf<HomeTile?>(null) }

    Column(Modifier.fillMaxSize()) {
        MoveHint(reorder.movingKey != null, Modifier.padding(start = TvSafeArea.Horizontal).height(24.dp))
        FavoritesLazyRow(
            reorder = reorder,
            model = model,
            callbacks = callbacks,
            tileWidth = Ps5TileWidth,
            tileHeight = Ps5TileHeight,
            focusedScale = 1.3f,
            onFocused = { tile ->
                focused = tile
                callbacks.onFocused(tile)
            },
        )
        Spacer(Modifier.height(8.dp))
        FocusedTitle(focused, MaterialTheme.typography.displayMedium, Modifier.padding(start = TvSafeArea.Horizontal))
        Spacer(Modifier.weight(1f))
        HomeWidgets(
            widgets = model.settings.widgets,
            clock = model.settings.clock,
            nowPlaying = model.nowPlaying,
            onMediaAction = callbacks.onMediaAction,
            vertical = false,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(start = TvSafeArea.Horizontal, bottom = TvSafeArea.Vertical),
        )
    }
}
