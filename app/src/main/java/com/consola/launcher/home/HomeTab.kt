package com.consola.launcher.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.consola.launcher.ui.TvSafeArea
import com.consola.launcher.settings.HomeLayout
import com.consola.launcher.settings.LauncherSettings
import com.consola.launcher.widgets.MediaAction
import com.consola.launcher.widgets.NowPlayingState

/** Todo lo que un estilo de Inicio necesita para dibujarse. */
data class HomeModel(
    val tiles: List<HomeTile>,
    /** Todas las apps visibles (para el estilo Filas, que las agrupa por tipo). */
    val allApps: List<HomeTile>,
    val settings: LauncherSettings,
    val nowPlaying: NowPlayingState,
    val requestInitialFocus: Boolean,
    val extras: HomeExtras,
)

/** Todo lo que un estilo de Inicio puede pedir que pase. */
class HomeCallbacks(
    val onFocused: (HomeTile) -> Unit,
    val onOpen: (HomeTile) -> Unit,
    val onToggleFavorite: (HomeTile) -> Unit,
    val onReorder: (List<String>) -> Unit,
    val onInitialFocusDone: () -> Unit,
    val onMediaAction: (MediaAction) -> Unit,
)

/** El estilo elegido en Ajustes → Estilo. Todos comparten el orden, el "mantener OK" y los widgets. */
@Composable
fun HomeTab(model: HomeModel, callbacks: HomeCallbacks) {
    val reorder = rememberReorderState(model.tiles, callbacks.onReorder)
    Box(Modifier.fillMaxSize()) {
        // La banda va primero: queda DETRÁS de las apps y los widgets, en todos los estilos. No ocupa lugar.
        VisualizerBand(
            model,
            // 190 dp: los golpes fuertes asoman por encima de la fila de apps; los suaves quedan entre los tiles.
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(190.dp).padding(horizontal = TvSafeArea.Horizontal),
        )
        when (model.settings.layout) {
            HomeLayout.Cinema -> CinemaHome(model, callbacks, reorder)
            HomeLayout.Ps5 -> Ps5Home(model, callbacks, reorder)
            HomeLayout.Xbox -> XboxHome(model, callbacks, reorder)
            HomeLayout.Minimal -> MinimalHome(model, callbacks, reorder)
            HomeLayout.Rows -> RowsHome(model, callbacks, reorder)
        }
    }
}
