package com.consola.launcher.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.first
import androidx.compose.ui.unit.dp
import com.consola.launcher.audio.Spectrum
import com.consola.launcher.settings.VisualizerStyle
import kotlin.math.abs

private const val FOLLOW = 0.35f
private const val STILL = 0.002f
private const val SILENT_ALPHA = 0.18f
private const val FRAME_NANOS = 33_000_000L

/**
 * Dibuja la música: barras, una onda o barras en espejo. El color sale de la app enfocada (el mismo de la aurora).
 *
 * El audio llega ~20 veces por segundo; acá se interpola a cada cuadro de la pantalla para que se mueva suave.
 * Solo se REDIBUJA el lienzo (el valor se lee en la fase de dibujo), no se recompone nada. En silencio se queda
 * quieto y no pide cuadros nuevos.
 */
@Composable
fun MusicVisualizer(
    spectrum: State<FloatArray>,
    style: VisualizerStyle,
    accent: Color,
    modifier: Modifier = Modifier,
    /** 1 = a pleno; de fondo de una tarjeta va más tenue para que el texto se siga leyendo. */
    intensity: Float = 1f,
) {
    val shown = remember { FloatArray(Spectrum.BARS) }
    var frame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(spectrum) {
        var lastDrawn = 0L
        while (true) {
            var moved = true
            withFrameNanos { now ->
                // 30 cuadros por segundo alcanzan y sobran: el audio llega a 20. 60 era trabajo tirado.
                if (now - lastDrawn < FRAME_NANOS) return@withFrameNanos
                lastDrawn = now
                val target = spectrum.value
                moved = false
                for (i in shown.indices) {
                    val next = shown[i] + (target.getOrElse(i) { 0f } - shown[i]) * FOLLOW
                    if (abs(next - shown[i]) > STILL) moved = true
                    shown[i] = next
                }
                if (moved) frame = now
            }
            if (!moved) {
                // Quieto (silencio o pausa): en vez de pedir cuadros, esperamos el próximo dato del audio.
                val current = spectrum.value
                snapshotFlow { spectrum.value }.first { it !== current }
            }
        }
    }

    // Su propia capa de dibujo: así redibujar las ondas NO vuelve a grabar todo el Inicio (medido: 86 % de CPU sin esto).
    Canvas(modifier.graphicsLayer()) {
        // Leer el cuadro acá (y no arriba) hace que solo se vuelva a dibujar el lienzo.
        if (frame < 0) return@Canvas
        // En silencio casi no se ve (una línea punteada fuerte parecería un error); con música se enciende.
        val energy = (shown.maxOrNull() ?: 0f).coerceIn(0f, 1f)
        val alpha = (SILENT_ALPHA + (1f - SILENT_ALPHA) * (energy * 4f).coerceAtMost(1f)) * intensity
        val brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), accent.copy(alpha = 0.85f)))
        when (style) {
            VisualizerStyle.Bars -> drawBars(shown, brush, alpha)
            VisualizerStyle.Wave -> drawWave(shown, accent, alpha)
            VisualizerStyle.Mirror -> drawMirror(shown, brush, alpha)
            VisualizerStyle.Off, VisualizerStyle.Pulse -> Unit
        }
    }
}

private fun DrawScope.drawBars(values: FloatArray, brush: Brush, alpha: Float) {
    val slot = size.width / values.size
    val barWidth = slot * 0.62f
    val minHeight = 3.dp.toPx()
    values.forEachIndexed { i, value ->
        val height = (value * size.height).coerceAtLeast(minHeight)
        drawRoundRect(
            brush = brush,
            topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - height),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(barWidth / 2),
            alpha = alpha,
        )
    }
}

/** Barras desde el centro hacia arriba y abajo, con los graves en el medio: simétrico, como un ecualizador de estudio. */
private fun DrawScope.drawMirror(values: FloatArray, brush: Brush, alpha: Float) {
    val mirrored = values.reversedArray() + values
    val slot = size.width / mirrored.size
    val barWidth = slot * 0.6f
    val minHeight = 2.dp.toPx()
    val middle = size.height / 2
    mirrored.forEachIndexed { i, value ->
        val half = (value * middle).coerceAtLeast(minHeight)
        drawRoundRect(
            brush = brush,
            topLeft = Offset(i * slot + (slot - barWidth) / 2, middle - half),
            size = Size(barWidth, half * 2),
            cornerRadius = CornerRadius(barWidth / 2),
            alpha = alpha,
        )
    }
}

/** Una línea suave que pasa por la punta de cada barra, con un degradé debajo. */
private fun DrawScope.drawWave(values: FloatArray, accent: Color, alpha: Float) {
    val step = size.width / (values.size - 1)
    val baseline = size.height - 2.dp.toPx()
    fun y(i: Int) = baseline - values[i] * (size.height * 0.92f)

    val line = Path().apply {
        moveTo(0f, y(0))
        for (i in 1 until values.size) {
            // Curva por los puntos medios: sin picos, como una onda de verdad.
            val previousX = (i - 1) * step
            val x = i * step
            quadraticTo(previousX, y(i - 1), (previousX + x) / 2, (y(i - 1) + y(i)) / 2)
        }
        lineTo(size.width, y(values.size - 1))
    }
    val fill = Path().apply {
        addPath(line)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(fill, Brush.verticalGradient(listOf(accent.copy(alpha = 0.45f), Color.Transparent)), alpha = alpha)
    drawPath(line, Color.White.copy(alpha = 0.95f), alpha = alpha, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
}
