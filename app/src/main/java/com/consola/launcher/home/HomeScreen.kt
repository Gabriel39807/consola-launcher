package com.consola.launcher.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.background.AnimatedBackground
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

// Overscan: muchas teles "se comen" los bordes de la imagen. Estos son los márgenes seguros de Android TV.
private val SafeHorizontal = 48.dp
private val SafeVertical = 27.dp

// Color de la aurora mientras ninguna app tiene el foco (o si no pudimos sacarle el color).
private val DefaultAccent = Color(0xFF2E4BFF)

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Cada vuelta al inicio recarga: apps instaladas/desinstaladas y un fondo nuevo si lo subiste.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    // El inicio no tiene "atrás": sin esto, Back cerraría el launcher y quedarías en negro.
    BackHandler {}

    HomeContent(state = state, onOpen = { tile -> TileLauncher.open(context, tile) })
}

/**
 * Estilo cine: arriba a la izquierda, el título grande de la app enfocada; abajo, la fila de apps.
 * El fondo toma el color de la app enfocada.
 */
@Composable
private fun HomeContent(state: HomeUiState, onOpen: (HomeTile) -> Unit) {
    var focusedTile by remember { mutableStateOf<HomeTile?>(null) }
    val accent = focusedTile?.accentArgb?.let { Color(it) } ?: DefaultAccent

    Box(Modifier.fillMaxSize()) {
        AnimatedBackground(video = state.backgroundVideo, accent = accent)

        Clock(Modifier.align(Alignment.TopEnd).padding(horizontal = SafeHorizontal, vertical = SafeVertical))

        Hero(
            tile = focusedTile,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = SafeHorizontal, top = 110.dp)
                .fillMaxWidth(0.55f),
        )

        AppRow(
            tiles = state.tiles,
            onFocused = { focusedTile = it },
            onOpen = onOpen,
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = SafeVertical),
        )
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEEE d MMMM") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(now.format(timeFormat), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = now.format(dayFormat).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
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
                text = stringResource(eyebrowFor(current)),
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

private fun eyebrowFor(tile: HomeTile): Int = when (tile) {
    is HomeTile.App -> if (tile.isFavorite) R.string.hero_favorite else R.string.hero_app
    HomeTile.Settings -> R.string.hero_system
}

@Composable
private fun AppRow(
    tiles: List<HomeTile>,
    onFocused: (HomeTile) -> Unit,
    onOpen: (HomeTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstTile = remember { FocusRequester() }
    val hasTiles = tiles.isNotEmpty()

    // En TV, si nadie tiene el foco, el control no hace NADA. Arrancamos enfocando el primer tile.
    LaunchedEffect(hasTiles) {
        if (hasTiles) {
            withFrameNanos { } // LazyRow compone sus ítems en el layout: esperamos un frame a que existan
            firstTile.requestFocus()
        }
    }

    LazyRow(
        modifier = modifier,
        // El padding va ADENTRO del LazyRow (no afuera) para que el tile agrandado no se recorte en los bordes.
        contentPadding = PaddingValues(horizontal = SafeHorizontal, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(tiles, key = { _, tile -> tile.key }) { index, tile ->
            AppTile(
                tile = tile,
                onFocused = { onFocused(tile) },
                onClick = { onOpen(tile) },
                modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
            )
        }
    }
}
