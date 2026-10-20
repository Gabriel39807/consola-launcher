package com.consola.launcher.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.consola.launcher.audio.Bands

private const val ScrimWidth = 240
private const val ScrimHeight = 135

// Cuánto se "aclara" el degradé en un golpe de graves: el video parece latir con la música.
private const val PulseLift = 0.45f

/** Oscurece la izquierda (título), abajo (apps) y, si lo pediste, todo el fondo un poco más ([dim]). */
internal fun DrawScope.paintCinemaScrim(dim: Float = 0f) {
    drawRect(Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.65f), 0.6f to Color.Transparent))
    drawRect(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f)))
    if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
}

/**
 * El degradé oscuro como capa aparte, para ponerlo encima del video.
 *
 * Pintar dos degradés a 1920x1080 en cada frame le cuesta caro a esta GPU. Lo pintamos UNA vez
 * en una imagen chiquita y la estiramos: la GPU solo copia una textura.
 */
@Composable
fun CinemaScrim(modifier: Modifier = Modifier, dim: Float = 0f, pulse: State<Bands>? = null) {
    // El oscurecido va horneado en la misma imagen: cambiarlo no suma ninguna capa de GPU.
    val scrim = remember(dim) {
        ImageBitmap(ScrimWidth, ScrimHeight).also { image ->
            CanvasDrawScope().draw(
                density = Density(1f),
                layoutDirection = LayoutDirection.Ltr,
                canvas = androidx.compose.ui.graphics.Canvas(image),
                size = Size(ScrimWidth.toFloat(), ScrimHeight.toFloat()),
            ) { paintCinemaScrim(dim) }
        }
    }

    Canvas(modifier.fillMaxSize()) {
        // Leemos el pulso ACÁ (fase de dibujo): 20 veces por segundo se redibuja, no se recompone.
        val bass = pulse?.value?.bass ?: 0f
        drawImage(
            image = scrim,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            alpha = 1f - PulseLift * bass,
            filterQuality = FilterQuality.Low,
        )
    }
}
