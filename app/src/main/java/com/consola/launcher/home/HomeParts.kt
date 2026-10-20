package com.consola.launcher.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.consola.launcher.ui.TvSafeArea

/** El nombre grande de la app enfocada, con una transición suave al cambiar. */
@Composable
internal fun FocusedTitle(tile: HomeTile?, style: TextStyle, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = tile,
        // Al refrescar llegan objetos nuevos con los mismos datos: animamos solo si cambia la APP, no el objeto.
        contentKey = { it?.key },
        transitionSpec = {
            (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 }) togetherWith fadeOut(tween(150))
        },
        label = "focusedTitle",
        modifier = modifier,
    ) { current ->
        if (current == null) return@AnimatedContent
        Text(current.label, style = style, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** La fila de favoritas que usan Cine, PS5 y Minimal (cambia el tamaño, no el comportamiento). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FavoritesLazyRow(
    reorder: ReorderState,
    model: HomeModel,
    callbacks: HomeCallbacks,
    tileWidth: Dp,
    tileHeight: Dp,
    onFocused: (HomeTile) -> Unit,
    modifier: Modifier = Modifier,
    focusedScale: Float = 1.15f,
    arrangement: Arrangement.Horizontal = Arrangement.spacedBy(20.dp),
) {
    val first = remember { FocusRequester() }
    val listState = rememberLazyListState()

    InitialFocus(first, enabled = model.requestInitialFocus, ready = reorder.order.isNotEmpty(), onDone = callbacks.onInitialFocusDone)

    // Si la app que movés se va del borde de la pantalla, la seguimos.
    LaunchedEffect(reorder.movedToIndex) {
        val to = reorder.movedToIndex ?: return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
        if (to !in visible.drop(1).dropLast(1)) listState.animateScrollToItem((to - 1).coerceAtLeast(0))
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            // Al bajar desde las pestañas, el foco vuelve a la última app donde estabas.
            .focusRestorer()
            .onPreviewKeyEvent(reorder::handleKey),
        // El padding va ADENTRO de la fila (no afuera) para que el tile agrandado no se recorte en los bordes.
        contentPadding = PaddingValues(horizontal = TvSafeArea.Horizontal, vertical = 20.dp),
        horizontalArrangement = arrangement,
    ) {
        itemsIndexed(reorder.order, key = { _, tile -> tile.key }) { index, tile ->
            AppTile(
                tile = tile,
                onFocused = { onFocused(tile) },
                onClick = { callbacks.onOpen(tile) },
                onLongClick = { reorder.longPress(tile, callbacks.onToggleFavorite) },
                isMoving = reorder.isMoving(tile),
                focusedScale = focusedScale,
                modifier = Modifier
                    // Desliza las apps a su nuevo lugar en vez de teletransportarlas.
                    .animateItem()
                    .size(tileWidth, tileHeight)
                    .then(if (index == 0) Modifier.focusRequester(first) else Modifier),
            )
        }
    }
}
