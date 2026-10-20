package com.consola.launcher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.consola.launcher.ui.TvSafeArea
import com.consola.launcher.widgets.HomeWidgets

// La destacada mide exactamente dos filas de la grilla: 2 × 76,5 + 9 de separación = 162 dp de alto.
private val FeaturedWidth = 288.dp
private val FeaturedHeight = 162.dp
private val GridTileWidth = 136.dp
private val GridTileHeight = 76.5.dp
private val GridRowGap = 9.dp

/** Xbox: tu primera favorita en grande y el resto en una grilla de dos filas. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun XboxHome(model: HomeModel, callbacks: HomeCallbacks, reorder: ReorderState) {
    var focused by remember { mutableStateOf<HomeTile?>(null) }
    val featuredFocus = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val featured = reorder.order.firstOrNull()
    val rest = reorder.order.drop(1)
    val onFocused: (HomeTile) -> Unit = { tile ->
        focused = tile
        callbacks.onFocused(tile)
    }

    InitialFocus(featuredFocus, enabled = model.requestInitialFocus, ready = featured != null, onDone = callbacks.onInitialFocusDone)

    // La grilla empieza en la SEGUNDA favorita: su índice es uno menos que en el orden completo.
    LaunchedEffect(reorder.movedToIndex) {
        val to = reorder.movedToIndex ?: return@LaunchedEffect
        if (to > 0) gridState.animateScrollToItem((to - 2).coerceAtLeast(0))
    }

    Column(Modifier.fillMaxSize()) {
        MoveHint(reorder.movingKey != null, Modifier.padding(start = TvSafeArea.Horizontal).height(24.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .focusRestorer()
                // Toda la zona es una sola lista: ◀ ▶ mueven entre la destacada y la grilla.
                .onPreviewKeyEvent(reorder::handleKey)
                .padding(start = TvSafeArea.Horizontal, top = 12.dp),
        ) {
            if (featured != null) {
                AppTile(
                    tile = featured,
                    onFocused = { onFocused(featured) },
                    onClick = { callbacks.onOpen(featured) },
                    onLongClick = { reorder.longPress(featured, callbacks.onToggleFavorite) },
                    isMoving = reorder.isMoving(featured),
                    focusedScale = 1.06f,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(FeaturedWidth, FeaturedHeight)
                        .focusRequester(featuredFocus),
                )
                Spacer(Modifier.width(20.dp))
            }
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.height(FeaturedHeight + 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = TvSafeArea.Horizontal),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(GridRowGap),
            ) {
                itemsIndexed(rest, key = { _, tile -> tile.key }) { _, tile ->
                    AppTile(
                        tile = tile,
                        onFocused = { onFocused(tile) },
                        onClick = { callbacks.onOpen(tile) },
                        onLongClick = { reorder.longPress(tile, callbacks.onToggleFavorite) },
                        isMoving = reorder.isMoving(tile),
                        focusedScale = 1.12f,
                        modifier = Modifier.animateItem().size(GridTileWidth, GridTileHeight),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = TvSafeArea.Horizontal).padding(bottom = TvSafeArea.Vertical),
            verticalAlignment = Alignment.Bottom,
        ) {
            FocusedTitle(focused, MaterialTheme.typography.headlineLarge, Modifier.weight(1f))
            HomeWidgets(
                widgets = model.settings.widgets,
                clock = model.settings.clock,
                nowPlaying = model.nowPlaying,
                onMediaAction = callbacks.onMediaAction,
                vertical = false,
            )
        }
    }
}
