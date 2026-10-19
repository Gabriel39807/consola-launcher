package com.consola.launcher.background

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DeepViolet = Color(0xFF5B2BBF)

private const val TwoPi = (2 * PI).toFloat()
private const val CycleMillis = 24_000L
private const val AccentFadeMillis = 700

// El movimiento es tan lento que a 30 fps el ojo no nota diferencia, y la GPU trabaja la mitad.
private const val FrameIntervalMillis = 33L

// La aurora se pinta en una imagen CHIQUITA (1/8 de 1080p) y se estira a pantalla completa.
// Un degradé es suave por naturaleza: al agrandarlo no se pierde nada visible.
private const val RenderWidth = 240
private const val RenderHeight = 135

/**
 * Fondo animado sin video: luces del color de la app enfocada que orbitan lento.
 *
 * Antes pintábamos 3 degradés radiales a 1920x1080 en cada frame: la GPU Mali-G52 tardaba ~55 ms
 * por frame (el presupuesto a 60 fps es 16 ms). Ahora la GPU solo estira una textura ya hecha.
 */
@Composable
fun AuroraBackground(accent: Color, modifier: Modifier = Modifier) {
    val frame = remember {
        ImageBitmap(RenderWidth, RenderHeight).also {
            // Imagen opaca: la GPU la copia directo, sin mezclar con lo que hay detrás.
            it.asAndroidBitmap().setHasAlpha(false)
        }
    }
    val frameCanvas = remember(frame) { androidx.compose.ui.graphics.Canvas(frame) }
    val frameScope = remember { CanvasDrawScope() }
    var phase by remember { mutableFloatStateOf(0f) }
    val tint = animateColorAsState(accent, tween(AccentFadeMillis), label = "auroraAccent")

    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { now -> phase = (now - start) % CycleMillis / CycleMillis.toFloat() * TwoPi }
            delay(FrameIntervalMillis)
        }
    }

    Canvas(modifier.fillMaxSize()) {
        // Leemos phase y tint ACÁ, en la fase de dibujo: cada cambio solo redibuja, no recompone.
        val t = phase
        val color = tint.value
        frameScope.draw(this, layoutDirection, frameCanvas, Size(RenderWidth.toFloat(), RenderHeight.toFloat())) {
            paintAurora(t, color)
            // Horneado en la misma imagen chiquita: así la GPU no pinta otra capa de pantalla completa encima.
            paintCinemaScrim()
        }
        drawImage(
            image = frame,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.Low, // interpolación bilineal: suaviza el estirado
        )
    }
}

private fun DrawScope.paintAurora(t: Float, accent: Color) {
    drawRect(Color(AccentTone.forBackground(accent.toArgb(), maxBrightness = 0.12f)))
    // La luz principal va a la derecha: la izquierda queda libre para el título.
    glow(accent, alpha = 0.70f, x = 0.78f + 0.10f * cos(t), y = 0.30f + 0.10f * sin(t), radius = 0.85f)
    glow(lerp(accent, Color.Black, 0.35f), alpha = 0.50f, x = 0.45f + 0.15f * sin(2 * t), y = 0.75f + 0.08f * cos(t), radius = 0.70f)
    glow(DeepViolet, alpha = 0.25f, x = 0.20f + 0.10f * cos(t + PI.toFloat()), y = 0.20f + 0.08f * sin(3 * t), radius = 0.60f)
}

// Los múltiplos enteros de t (t, 2t, 3t) hacen que el loop empalme sin salto al volver a 0.
private fun DrawScope.glow(color: Color, alpha: Float, x: Float, y: Float, radius: Float) {
    val center = Offset(size.width * x, size.height * y)
    val radiusPx = size.minDimension * radius
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radiusPx,
        ),
        radius = radiusPx,
        center = center,
    )
}
