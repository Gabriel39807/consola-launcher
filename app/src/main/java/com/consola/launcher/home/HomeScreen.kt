package com.consola.launcher.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.consola.launcher.background.AnimatedBackground
import com.consola.launcher.search.SearchTab
import com.consola.launcher.settings.SettingsScreen
import com.consola.launcher.ui.TvSafeArea

// Color de la aurora mientras ninguna app tiene el foco (o si no pudimos sacarle el color).
private val DefaultAccent = Color(0xFF2E4BFF)

/** El armazón: fondo animado + pestañas arriba + la pestaña elegida abajo. */
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    var accent by remember { mutableStateOf(DefaultAccent) }
    val tabFocus = remember { Destination.entries.associateWith { FocusRequester() } }
    val gate = remember { TabFocusGate() }
    val focusManager = LocalFocusManager.current

    fun focusSelectedTab() {
        gate.tabsFocusable = true
        tabFocus.getValue(destination).requestFocus()
    }

    // Cada vuelta al inicio recarga: apps instaladas/desinstaladas y fondos nuevos.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    // Back: desde cualquier pestaña vuelve a Inicio. En Inicio no hace nada: si no lo bloqueamos,
    // Back cerraría el launcher y quedarías en negro.
    BackHandler {
        if (destination != Destination.Home) {
            destination = Destination.Home
            focusSelectedTab()
        }
    }

    val openApp: (HomeTile) -> Unit = { tile -> SystemActions.openApp(context, tile) }
    val onAppFocused: (HomeTile) -> Unit = { tile -> accent = tile.accentArgb?.let { Color(it) } ?: DefaultAccent }

    Box(Modifier.fillMaxSize()) {
        AnimatedBackground(video = state.backgroundVideo, accent = accent)

        Column(Modifier.fillMaxSize()) {
            TopBar(
                selected = destination,
                onSelect = { destination = it },
                focusRequesters = tabFocus,
                gate = gate,
                modifier = Modifier.padding(horizontal = TvSafeArea.Horizontal, vertical = TvSafeArea.Vertical),
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Mientras el foco está en el contenido, las pestañas no existen para la búsqueda de foco...
                    .onFocusChanged { if (it.hasFocus) gate.tabsFocusable = false }
                    // ...y ARRIBA primero intenta moverse DENTRO del contenido. Si ya no hay nada más arriba,
                    // vamos a la pestaña donde estabas, no a la que quede más cerca.
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionUp) return@onPreviewKeyEvent false
                        if (!focusManager.moveFocus(FocusDirection.Up)) focusSelectedTab()
                        true
                    },
            ) {
                when (destination) {
                    Destination.Search -> SearchTab(onSearch = { SystemActions.openGoogleSearch(context) })
                    Destination.Home -> HomeTab(tiles = state.homeTiles, onFocused = onAppFocused, onOpen = openApp)
                    Destination.Apps -> AppsTab(apps = state.allApps, onFocused = onAppFocused, onOpen = openApp)
                    Destination.Settings -> SettingsScreen()
                }
            }
        }
    }
}
