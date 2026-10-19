package com.consola.launcher.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.apps.AppArtwork
import com.consola.launcher.ui.tvClickable

// 160x90 dp en una tele xhdpi (densidad 2) = 320x180 px: EXACTAMENTE el tamaño de un banner de Android TV.
// Uno a uno, sin estirar: por eso se ve nítido. Antes eran 224 dp = 448 px, y el banner se estiraba un 40 %.
val TileWidth = 160.dp
val TileHeight = 90.dp

private val TileShape = RoundedCornerShape(10.dp)
private val TileBackground = Color(0xFF1C2130)
private val UnfocusedDim = Color.Black.copy(alpha = 0.3f)
private val MovingColor = Color(0xFF3D5AFE)
private const val MovingScale = 1.22f

/** Sin tamaño propio: el que lo usa decide (fijo en la fila de Inicio, flexible en la grilla de Apps). */
@Composable
fun AppTile(
    tile: HomeTile,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusedScale: Float = 1.15f,
    onLongClick: (() -> Unit)? = null,
    isMoving: Boolean = false,
    showFavoriteBadge: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            isMoving -> MovingScale
            isFocused -> focusedScale
            else -> 1f
        },
        label = "tileScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Sin sombra: en esta GPU, una sombra que cambia de tamaño en cada frame de la animación cuesta caro.
            .clip(TileShape)
            .background(TileBackground)
            // Enfocado: borde blanco. Moviéndose: borde azul más grueso. El resto se apaga un poco.
            // Oscurecemos dibujando un velo encima y NO con alpha: alpha < 1 obliga a la GPU a una capa extra por tile.
            .then(
                when {
                    isMoving -> Modifier.border(4.dp, MovingColor, TileShape)
                    isFocused -> Modifier.border(3.dp, Color.White, TileShape)
                    else -> Modifier.drawWithContent {
                        drawContent()
                        drawRect(UnfocusedDim)
                    }
                },
            )
            // El orden IMPORTA: onFocusChanged observa al elemento enfocable que viene DESPUÉS en la cadena.
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) onFocused()
            }
            .tvClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        when (val artwork = tile.artwork) {
            is AppArtwork.Banner -> Image(
                bitmap = artwork.image,
                contentDescription = tile.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            is AppArtwork.Icon -> Image(
                bitmap = artwork.image,
                contentDescription = tile.label,
                modifier = Modifier.size(48.dp),
            )
            AppArtwork.None -> Text(tile.label, style = MaterialTheme.typography.labelLarge)
        }

        if (isMoving) {
            // Las flechas dicen "esto se mueve con ◀ ▶" sin necesidad de explicarlo con texto.
            Row(
                Modifier.fillMaxSize().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoveArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft)
                MoveArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight)
            }
        } else if (showFavoriteBadge) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(2.dp),
            )
        }
    }
}

@Composable
private fun MoveArrow(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(MovingColor),
    )
}
