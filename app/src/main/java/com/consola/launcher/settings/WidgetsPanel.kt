package com.consola.launcher.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.consola.launcher.ConsolaApp
import com.consola.launcher.R
import com.consola.launcher.audio.AudioPulse
import com.consola.launcher.home.SystemActions
import com.consola.launcher.widgets.NowPlayingCard
import com.consola.launcher.widgets.NowPlayingState
import java.time.format.DateTimeFormatter

private val PreferredPlayer.label: Int
    get() = when (this) {
        PreferredPlayer.Auto -> R.string.player_auto
        PreferredPlayer.Spotify -> R.string.player_spotify
        PreferredPlayer.YouTubeMusic -> R.string.player_youtube_music
    }

private val VisualizerStyle.label: Int
    get() = when (this) {
        VisualizerStyle.Off -> R.string.visualizer_off
        VisualizerStyle.Bars -> R.string.visualizer_bars
        VisualizerStyle.Wave -> R.string.visualizer_wave
        VisualizerStyle.Mirror -> R.string.visualizer_mirror
        VisualizerStyle.Pulse -> R.string.visualizer_pulse
    }

private val VisualizerPlace.label: Int
    get() = when (this) {
        VisualizerPlace.AboveApps -> R.string.place_above_apps
        VisualizerPlace.WithMusic -> R.string.place_with_music
    }

/**
 * Todos los widgets, cada uno se prende y se apaga acá. Los que necesitan escribir algo (la ciudad del clima,
 * qué contar en la cuenta regresiva) se cargan desde el celular: con el control remoto sería un martirio.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WidgetsPanel(
    settings: LauncherSettings,
    audioStatus: AudioPulse.Status,
    hasAudioPermission: () -> Boolean,
    onChange: ((WidgetSettings) -> WidgetSettings) -> Unit,
) {
    val widgets = settings.widgets
    val context = LocalContext.current
    val nowPlaying = remember { (context.applicationContext as ConsolaApp).graph.nowPlaying }
    val playing by nowPlaying.state.collectAsStateWithLifecycle()
    var hasAccess by remember { mutableStateOf(nowPlaying.hasAccess()) }

    // Al volver de la pantalla de permisos de la tele, se ve el cambio sin tener que salir y entrar.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasAccess = nowPlaying.hasAccess() }

    // El micrófono se pide recién al prender el visualizador: nadie ve un pedido de permiso sin saber para qué es.
    // Android lo exige para ANALIZAR el audio que suena; no se graba nada.
    var pendingStyle by remember { mutableStateOf<VisualizerStyle?>(null) }
    val askMicrophone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val style = pendingStyle
        if (granted && style != null) onChange { it.copy(visualizer = style) }
        pendingStyle = null
    }
    fun cycleVisualizer() {
        val next = widgets.visualizer.next()
        if (next == VisualizerStyle.Off || hasAudioPermission()) {
            onChange { it.copy(visualizer = next) }
        } else {
            pendingStyle = next
            askMicrophone.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().focusRestorer(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item { PanelTitle(stringResource(R.string.settings_widgets)) }

        // ---------- Música ----------
        item {
            SettingsRow(
                icon = Icons.Default.PlayArrow,
                title = stringResource(R.string.widget_now_playing),
                description = stringResource(R.string.widget_now_playing_desc),
                switchState = widgets.nowPlaying,
                onClick = { onChange { it.copy(nowPlaying = !it.nowPlaying) } },
            )
        }
        // Sin acceso, Android no nos deja ver qué suena. La tele tiene su pantalla para darlo: la abrimos.
        if (widgets.nowPlaying && !hasAccess) {
            item {
                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.widget_now_playing_access),
                    description = stringResource(R.string.widget_now_playing_access_desc),
                    onClick = { SystemActions.openNotificationAccess(context) },
                )
            }
        }
        if (widgets.nowPlaying) {
            item {
                SettingsRow(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.widget_player),
                    description = stringResource(R.string.widget_player_desc),
                    value = stringResource(widgets.player.label),
                    onClick = { onChange { it.copy(player = it.player.next()) } },
                )
            }
        }
        item {
            SettingsRow(
                icon = Icons.Default.ThumbUp,
                title = stringResource(R.string.widget_visualizer),
                description = stringResource(
                    if (audioStatus == AudioPulse.Status.Unavailable) R.string.bg_music_unavailable else R.string.widget_visualizer_desc,
                ),
                value = stringResource(widgets.visualizer.label),
                onClick = ::cycleVisualizer,
            )
        }
        if (widgets.drawsVisualizer) {
            item {
                SettingsRow(
                    title = stringResource(R.string.widget_visualizer_place),
                    value = stringResource(widgets.visualizerPlace.label),
                    onClick = { onChange { it.copy(visualizerPlace = it.visualizerPlace.next()) } },
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.widget_visualizer_dream),
                    description = stringResource(R.string.widget_visualizer_dream_desc),
                    switchState = widgets.visualizerInDream,
                    onClick = { onChange { it.copy(visualizerInDream = !it.visualizerInDream) } },
                )
            }
        }

        // ---------- Información ----------
        item {
            val city = settings.weatherPlace?.name ?: stringResource(R.string.weather_city_auto)
            SettingsRow(
                icon = Icons.Default.Place,
                title = stringResource(R.string.widget_weather),
                description = stringResource(R.string.widget_weather_desc, city),
                switchState = widgets.weather,
                onClick = { onChange { it.copy(weather = !it.weather) } },
            )
        }
        if (widgets.weather) {
            item {
                SettingsRow(
                    title = stringResource(R.string.widget_units),
                    value = stringResource(if (widgets.fahrenheit) R.string.units_f else R.string.units_c),
                    onClick = { onChange { it.copy(fahrenheit = !it.fahrenheit) } },
                )
            }
        }
        item {
            val countdown = settings.countdown
            SettingsRow(
                icon = Icons.Default.Star,
                title = stringResource(R.string.widget_countdown),
                description = if (countdown == null) {
                    stringResource(R.string.countdown_empty)
                } else {
                    stringResource(R.string.widget_countdown_desc, countdown.title, countdown.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                },
                switchState = widgets.countdown,
                onClick = { onChange { it.copy(countdown = !it.countdown) } },
            )
        }
        item {
            SettingsRow(
                icon = Icons.Default.DateRange,
                title = stringResource(R.string.widget_calendar),
                description = stringResource(R.string.widget_calendar_desc),
                switchState = widgets.calendar,
                onClick = { onChange { it.copy(calendar = !it.calendar) } },
            )
        }
        item {
            SettingsRow(
                icon = Icons.Default.DateRange,
                title = stringResource(R.string.widget_big_clock),
                description = stringResource(R.string.widget_big_clock_desc),
                switchState = widgets.bigClock,
                onClick = { onChange { it.copy(bigClock = !it.bigClock) } },
            )
        }
        // Vista previa real: si estás escuchando algo, lo ves acá tal cual va a aparecer en el Inicio.
        if (widgets.nowPlaying && playing is NowPlayingState.Active) {
            item {
                Spacer(Modifier.height(6.dp))
                NowPlayingCard(state = playing, onAction = nowPlaying::perform)
            }
        }
    }
}
