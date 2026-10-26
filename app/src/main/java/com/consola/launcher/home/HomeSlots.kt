package com.consola.launcher.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.consola.launcher.settings.VisualizerPlace
import com.consola.launcher.weather.WeatherState
import com.consola.launcher.widgets.InfoWidgetsRow
import com.consola.launcher.widgets.MusicVisualizer

/** Lo que los widgets nuevos necesitan y no vive en los ajustes: el sonido, el color del momento y el clima. */
class HomeExtras(
    /** Las barras del sonido. Es un State (no su valor): así solo se redibuja el visualizador, no todo el Inicio. */
    val spectrum: State<FloatArray>,
    /** El color de la app enfocada: el visualizador se tiñe igual que la aurora. */
    val accent: Color,
    val weather: WeatherState,
)

/**
 * "Abajo, a lo ancho": la banda se dibuja DETRÁS de todo, al pie de la pantalla. No ocupa lugar en ningún estilo:
 * las barras suben por detrás de las apps, como el escenario de un ecualizador. Así nunca se pisa con nada.
 */
@Composable
internal fun VisualizerBand(model: HomeModel, modifier: Modifier = Modifier) {
    val widgets = model.settings.widgets
    if (!widgets.drawsVisualizer || widgets.visualizerPlace != VisualizerPlace.AboveApps) return
    MusicVisualizer(model.extras.spectrum, widgets.visualizer, model.extras.accent, modifier)
}

/**
 * "Con la música": las ondas de fondo DENTRO de la tarjeta (o la cápsula) de la música, más tenues para que el texto
 * se lea. No suma alto, así que no empuja a los otros widgets. null si no corresponde.
 */
internal fun musicBackdrop(model: HomeModel): (@Composable (Modifier) -> Unit)? {
    val widgets = model.settings.widgets
    if (!widgets.drawsVisualizer || widgets.visualizerPlace != VisualizerPlace.WithMusic) return null
    return { modifier -> MusicVisualizer(model.extras.spectrum, widgets.visualizer, model.extras.accent, modifier, intensity = 0.45f) }
}

/** Clima, cuenta regresiva y calendario (los que estén activados), en fila, del mismo ancho que la tarjeta de música. */
@Composable
internal fun InfoSlot(model: HomeModel, modifier: Modifier = Modifier) {
    InfoWidgetsRow(model.settings.widgets, model.extras.weather, model.settings.countdown, modifier)
}
