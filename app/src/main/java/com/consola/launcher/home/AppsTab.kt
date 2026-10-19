package com.consola.launcher.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.ui.TvSafeArea
import kotlinx.coroutines.delay

// 5 columnas: (960 dp de ancho − 2×48 de márgenes − 4×16 de separación) / 5 = 160 dp, el tamaño nítido del banner.
private const val Columns = 5
private const val FeedbackMillis = 2_000L

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppsTab(
    apps: List<HomeTile>,
    onFocused: (HomeTile) -> Unit,
    onOpen: (HomeTile) -> Unit,
    onToggleFavorite: (HomeTile) -> Unit,
) {
    // Mantener OK cambia algo que no se ve en esta pestaña (el Inicio): lo confirmamos con un aviso corto.
    var feedback by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(FeedbackMillis)
            feedback = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(Columns),
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(
                start = TvSafeArea.Horizontal,
                end = TvSafeArea.Horizontal,
                top = 16.dp,
                bottom = TvSafeArea.Vertical,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(apps, key = { it.key }) { tile ->
                AppTile(
                    tile = tile,
                    onFocused = { onFocused(tile) },
                    onClick = { onOpen(tile) },
                    onLongClick = {
                        feedback = if (tile.isFavorite) R.string.favorite_removed else R.string.favorite_added
                        onToggleFavorite(tile)
                    },
                    showFavoriteBadge = tile.isFavorite,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    focusedScale = 1.1f,
                )
            }
        }

        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = TvSafeArea.Vertical),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xEE1C2130))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                // Guardamos el último mensaje mientras se va, para que no desaparezca el texto antes que la cápsula.
                var message by remember { mutableStateOf(R.string.favorite_added) }
                feedback?.let { message = it }
                Text(stringResource(message), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
