package com.consola.launcher.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.consola.launcher.ConsolaApp
import com.consola.launcher.R
import com.consola.launcher.home.SystemActions
import com.consola.launcher.widgets.NowPlayingCard
import com.consola.launcher.widgets.NowPlayingState

private val PreferredPlayer.label: Int
    get() = when (this) {
        PreferredPlayer.Auto -> R.string.player_auto
        PreferredPlayer.Spotify -> R.string.player_spotify
        PreferredPlayer.YouTubeMusic -> R.string.player_youtube_music
    }

@Composable
fun WidgetsPanel(widgets: WidgetSettings, onChange: ((WidgetSettings) -> WidgetSettings) -> Unit) {
    val context = LocalContext.current
    val nowPlaying = remember { (context.applicationContext as ConsolaApp).graph.nowPlaying }
    val playing by nowPlaying.state.collectAsStateWithLifecycle()
    var hasAccess by remember { mutableStateOf(nowPlaying.hasAccess()) }

    // Al volver de la pantalla de permisos de la tele, se ve el cambio sin tener que salir y entrar.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasAccess = nowPlaying.hasAccess() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PanelTitle(stringResource(R.string.settings_widgets))
        SettingsRow(
            icon = Icons.Default.PlayArrow,
            title = stringResource(R.string.widget_now_playing),
            description = stringResource(R.string.widget_now_playing_desc),
            switchState = widgets.nowPlaying,
            onClick = { onChange { it.copy(nowPlaying = !it.nowPlaying) } },
        )
        // Sin acceso, Android no nos deja ver qué suena. La tele tiene su pantalla para darlo: la abrimos.
        if (widgets.nowPlaying && !hasAccess) {
            SettingsRow(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.widget_now_playing_access),
                description = stringResource(R.string.widget_now_playing_access_desc),
                onClick = { SystemActions.openNotificationAccess(context) },
            )
        }
        if (widgets.nowPlaying) {
            SettingsRow(
                icon = Icons.Default.Favorite,
                title = stringResource(R.string.widget_player),
                description = stringResource(R.string.widget_player_desc),
                value = stringResource(widgets.player.label),
                onClick = { onChange { it.copy(player = it.player.next()) } },
            )
        }
        SettingsRow(
            icon = Icons.Default.DateRange,
            title = stringResource(R.string.widget_big_clock),
            description = stringResource(R.string.widget_big_clock_desc),
            switchState = widgets.bigClock,
            onClick = { onChange { it.copy(bigClock = !it.bigClock) } },
        )
        // Vista previa real: si estás escuchando algo, lo ves acá tal cual va a aparecer en el Inicio.
        if (widgets.nowPlaying && playing is NowPlayingState.Active) {
            Spacer(Modifier.height(6.dp))
            NowPlayingCard(state = playing, onAction = nowPlaying::perform)
        }
    }
}
