package com.consola.launcher.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.ui.TvSafeArea

/** Estilo cine: arriba el título grande de la app enfocada; abajo, la fila de favoritas. */
@Composable
fun HomeTab(
    tiles: List<HomeTile>,
    onFocused: (HomeTile) -> Unit,
    onOpen: (HomeTile) -> Unit,
) {
    var focusedTile by remember { mutableStateOf<HomeTile?>(null) }

    Box(Modifier.fillMaxSize()) {
        Hero(
            tile = focusedTile,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = TvSafeArea.Horizontal, top = 16.dp)
                .fillMaxWidth(0.6f),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(bottom = TvSafeArea.Vertical)) {
            Text(
                text = stringResource(R.string.home_row_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = TvSafeArea.Horizontal),
            )
            FavoritesRow(
                tiles = tiles,
                onFocused = { tile ->
                    focusedTile = tile
                    onFocused(tile)
                },
                onOpen = onOpen,
            )
        }
    }
}

@Composable
private fun Hero(tile: HomeTile?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = tile,
        // Al refrescar llegan objetos nuevos con los mismos datos: animamos solo si cambia la APP, no el objeto.
        contentKey = { it?.key },
        transitionSpec = {
            (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 }) togetherWith fadeOut(tween(150))
        },
        label = "hero",
        modifier = modifier,
    ) { current ->
        if (current == null) return@AnimatedContent
        Column {
            Text(
                text = stringResource(if (current.isFavorite) R.string.hero_favorite else R.string.hero_app),
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 4.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = current.label,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.press_ok_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FavoritesRow(
    tiles: List<HomeTile>,
    onFocused: (HomeTile) -> Unit,
    onOpen: (HomeTile) -> Unit,
) {
    val firstTile = remember { FocusRequester() }
    val hasTiles = tiles.isNotEmpty()

    // En TV, si nadie tiene el foco, el control no hace NADA. Arrancamos enfocando la primera app.
    LaunchedEffect(hasTiles) {
        if (hasTiles) {
            withFrameNanos { } // LazyRow compone sus ítems en el layout: esperamos un frame a que existan
            firstTile.requestFocus()
        }
    }

    LazyRow(
        // Al bajar desde las pestañas, el foco vuelve a la última app donde estabas.
        modifier = Modifier.focusRestorer(),
        // El padding va ADENTRO del LazyRow (no afuera) para que el tile agrandado no se recorte en los bordes.
        contentPadding = PaddingValues(horizontal = TvSafeArea.Horizontal, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(tiles, key = { _, tile -> tile.key }) { index, tile ->
            AppTile(
                tile = tile,
                onFocused = { onFocused(tile) },
                onClick = { onOpen(tile) },
                modifier = Modifier
                    .size(TileWidth, TileHeight)
                    .then(if (index == 0) Modifier.focusRequester(firstTile) else Modifier),
            )
        }
    }
}
