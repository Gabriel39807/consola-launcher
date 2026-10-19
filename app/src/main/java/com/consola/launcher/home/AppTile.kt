package com.consola.launcher.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.apps.AppArtwork

// 160x90 dp en una tele xhdpi (densidad 2) = 320x180 px: EXACTAMENTE el tamaño de un banner de Android TV.
// Uno a uno, sin estirar: por eso se ve nítido. Antes eran 224 dp = 448 px, y el banner se estiraba un 40 %.
val TileWidth = 160.dp
val TileHeight = 90.dp

private val TileShape = RoundedCornerShape(10.dp)
private val TileBackground = Color(0xFF1C2130)
private val UnfocusedDim = Color.Black.copy(alpha = 0.3f)

/** Sin tamaño propio: el que lo usa decide (fijo en la fila de Inicio, flexible en la grilla de Apps). */
@Composable
fun AppTile(
    tile: HomeTile,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusedScale: Float = 1.15f,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) focusedScale else 1f, label = "tileScale")

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Sin sombra: en esta GPU, una sombra que cambia de tamaño en cada frame de la animación cuesta caro.
            .clip(TileShape)
            .background(TileBackground)
            // Enfocado: borde blanco. El resto se apaga un poco para que el foco resalte.
            // Oscurecemos dibujando un velo encima y NO con alpha: alpha < 1 obliga a la GPU a una capa extra por tile.
            .then(
                if (isFocused) {
                    Modifier.border(3.dp, Color.White, TileShape)
                } else {
                    Modifier.drawWithContent {
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
            // clickable ya es enfocable con el D-pad y responde al botón OK del control.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // el foco lo dibujamos nosotros: escala + borde
                onClick = onClick,
            ),
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
    }
}
