package com.consola.launcher.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.ui.TvSafeArea
import com.consola.launcher.ui.isConfirm
import kotlinx.coroutines.launch

/** Estilo cine: arriba el nombre grande de la app enfocada; abajo, la fila de favoritas. */
@Composable
fun HomeTab(
    tiles: List<HomeTile>,
    requestInitialFocus: Boolean,
    onInitialFocusDone: () -> Unit,
    onFocused: (HomeTile) -> Unit,
    onOpen: (HomeTile) -> Unit,
    onToggleFavorite: (HomeTile) -> Unit,
    onReorderFavorites: (List<String>) -> Unit,
) {
    var focusedTile by remember { mutableStateOf<HomeTile?>(null) }
    var movingKey by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Hero(
            tile = focusedTile,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = TvSafeArea.Horizontal, top = 24.dp)
                .fillMaxWidth(0.6f),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(bottom = TvSafeArea.Vertical)) {
            // La única ayuda en pantalla, y solo mientras movés: es cuando de verdad hace falta.
            val hintAlpha by animateFloatAsState(if (movingKey != null) 1f else 0f, label = "moveHint")
            Text(
                text = stringResource(R.string.move_hint),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                modifier = Modifier
                    .padding(start = TvSafeArea.Horizontal)
                    .height(24.dp)
                    .graphicsLayer { alpha = hintAlpha },
            )
            FavoritesRow(
                tiles = tiles,
                movingKey = movingKey,
                onMovingChange = { movingKey = it },
                requestInitialFocus = requestInitialFocus,
                onInitialFocusDone = onInitialFocusDone,
                onFocused = { tile ->
                    focusedTile = tile
                    onFocused(tile)
                },
                onOpen = onOpen,
                // Mantener OK: si es favorita, la movés; si no (inicio sin favoritas), la agregás.
                onLongPress = { tile -> if (tile.isFavorite) movingKey = tile.key else onToggleFavorite(tile) },
                onReorder = onReorderFavorites,
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
        Text(
            text = current.label,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FavoritesRow(
    tiles: List<HomeTile>,
    movingKey: String?,
    onMovingChange: (String?) -> Unit,
    requestInitialFocus: Boolean,
    onInitialFocusDone: () -> Unit,
    onFocused: (HomeTile) -> Unit,
    onOpen: (HomeTile) -> Unit,
    onLongPress: (HomeTile) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val firstTile = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Copia local del orden: al mover, la app cambia de lugar AL INSTANTE, sin esperar a que se guarde.
    var order by remember(tiles) { mutableStateOf(tiles) }
    // Al soltar con OK, el KeyUp de ese OK no tiene que abrir la app.
    val swallowNextConfirmUp = remember { BooleanHolder() }

    // Solo la PRIMERA vez que se abre el launcher: si lo hiciéramos cada vez que entrás a Inicio,
    // el foco saltaría solo desde las pestañas a la fila mientras navegás.
    LaunchedEffect(requestInitialFocus, order.isNotEmpty()) {
        if (requestInitialFocus && order.isNotEmpty()) {
            withFrameNanos { } // LazyRow compone sus ítems en el layout: esperamos un frame a que existan
            firstTile.requestFocus()
            onInitialFocusDone()
        }
    }

    fun moveBy(delta: Int) {
        val from = order.indexOfFirst { it.key == movingKey }
        val to = from + delta
        if (from < 0 || to !in order.indices) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        onReorder(order.map { it.packageName })
        // Si la app se va del borde de la pantalla, la seguimos.
        val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
        if (to !in visible.drop(1).dropLast(1)) scope.launch { listState.animateScrollToItem((to - 1).coerceAtLeast(0)) }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            // Al bajar desde las pestañas, el foco vuelve a la última app donde estabas.
            .focusRestorer()
            // Mientras movés, la fila se queda con TODAS las teclas: ◀ ▶ mueven, OK o Atrás sueltan,
            // y arriba/abajo no hacen nada (así no te salís de la fila con una app "en el aire").
            .onPreviewKeyEvent { event ->
                if (movingKey == null) {
                    if (swallowNextConfirmUp.value && event.type == KeyEventType.KeyUp && event.isConfirm()) {
                        swallowNextConfirmUp.value = false
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent false
                }
                if (event.type == KeyEventType.KeyDown) {
                    val isFreshPress = event.nativeKeyEvent.repeatCount == 0
                    when {
                        event.key == Key.DirectionLeft -> moveBy(-1)
                        event.key == Key.DirectionRight -> moveBy(+1)
                        // Las repeticiones del OK que mantuviste para empezar a mover NO sueltan la app.
                        event.isConfirm() && isFreshPress -> {
                            onMovingChange(null)
                            swallowNextConfirmUp.value = true
                        }
                        event.key == Key.Back -> onMovingChange(null)
                    }
                }
                true
            },
        // El padding va ADENTRO del LazyRow (no afuera) para que el tile agrandado no se recorte en los bordes.
        contentPadding = PaddingValues(horizontal = TvSafeArea.Horizontal, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(order, key = { _, tile -> tile.key }) { index, tile ->
            AppTile(
                tile = tile,
                onFocused = { onFocused(tile) },
                onClick = { onOpen(tile) },
                onLongClick = { onLongPress(tile) },
                isMoving = tile.key == movingKey,
                modifier = Modifier
                    // Desliza las apps a su nuevo lugar en vez de teletransportarlas.
                    .animateItem()
                    .size(TileWidth, TileHeight)
                    .then(if (index == 0) Modifier.focusRequester(firstTile) else Modifier),
            )
        }
    }
}

private class BooleanHolder {
    var value = false
}
