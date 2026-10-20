package com.consola.launcher.home

import androidx.compose.runtime.Composable
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
    when (model.settings.layout) {
        HomeLayout.Cinema -> CinemaHome(model, callbacks, reorder)
        HomeLayout.Ps5 -> Ps5Home(model, callbacks, reorder)
        HomeLayout.Xbox -> XboxHome(model, callbacks, reorder)
        HomeLayout.Minimal -> MinimalHome(model, callbacks, reorder)
        HomeLayout.Rows -> RowsHome(model, callbacks, reorder)
    }
}
