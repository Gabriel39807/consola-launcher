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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.apps.AppCategory
import com.consola.launcher.ui.TvSafeArea
import com.consola.launcher.settings.VisualizerPlace
import com.consola.launcher.widgets.NowPlayingPill

/**
 * Filas: tus favoritas arriba y después todas tus apps agrupadas por tipo (Video, Música, Juegos...).
 * Lo más pedido en los foros de Android TV: no tener una única tira larguísima.
 */
@Composable
internal fun RowsHome(model: HomeModel, callbacks: HomeCallbacks, reorder: ReorderState) {
    var focused by remember { mutableStateOf<HomeTile?>(null) }
    val onFocused: (HomeTile) -> Unit = { tile ->
        focused = tile
        callbacks.onFocused(tile)
    }
    // Se agrupa solo: cada app ya trae su categoría. Las filas vacías no se muestran.
    val sections = remember(model.allApps) {
        AppCategory.entries
            .map { category -> category to model.allApps.filter { it.category == category } }
            .filter { (_, apps) -> apps.isNotEmpty() }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = TvSafeArea.Horizontal).height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocusedTitle(focused, MaterialTheme.typography.headlineLarge, Modifier.weight(1f))
            if (model.settings.widgets.nowPlaying) NowPlayingPill(model.nowPlaying, callbacks.onMediaAction, backdrop = musicBackdrop(model))
        }
        // Fija debajo del encabezado, fuera de la lista: dentro de la lista se iba medio escondida al enfocar las apps.
        InfoSlot(model, Modifier.padding(start = TvSafeArea.Horizontal, top = 4.dp, bottom = 8.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = TvSafeArea.Vertical)) {
            item(key = "favorites") {
                SectionTitle(stringResource(R.string.row_favorites)) {
                    Spacer(Modifier.width(16.dp))
                    MoveHint(reorder.movingKey != null)
                }
                FavoritesLazyRow(
                    reorder = reorder,
                    model = model,
                    callbacks = callbacks,
                    tileWidth = TileWidth,
                    tileHeight = TileHeight,
                    onFocused = onFocused,
                )
            }
            sections.forEach { (category, apps) ->
                item(key = category.name) {
                    SectionTitle(stringResource(category.title))
                    CategoryRow(apps, callbacks, onFocused)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.padding(start = TvSafeArea.Horizontal, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
        trailing()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CategoryRow(apps: List<HomeTile>, callbacks: HomeCallbacks, onFocused: (HomeTile) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().focusRestorer(),
        contentPadding = PaddingValues(horizontal = TvSafeArea.Horizontal, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(apps, key = { it.key }) { tile ->
            AppTile(
                tile = tile,
                onFocused = { onFocused(tile) },
                onClick = { callbacks.onOpen(tile) },
                // Acá mantener OK agrega o quita de Favoritas (el orden se cambia en la fila de arriba).
                onLongClick = { callbacks.onToggleFavorite(tile) },
                showFavoriteBadge = tile.isFavorite,
                modifier = Modifier.size(TileWidth, TileHeight),
            )
        }
    }
}

private val AppCategory.title: Int
    get() = when (this) {
        AppCategory.Video -> R.string.row_video
        AppCategory.Music -> R.string.row_music
        AppCategory.Games -> R.string.row_games
        AppCategory.Other -> R.string.row_other
    }
