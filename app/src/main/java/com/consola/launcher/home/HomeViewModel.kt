package com.consola.launcher.home

import com.consola.launcher.weather.Place
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.consola.launcher.ConsolaApp
import com.consola.launcher.apps.AppCatalog
import com.consola.launcher.apps.LoadedApp
import com.consola.launcher.background.BackgroundCatalog
import com.consola.launcher.settings.LauncherSettings
import com.consola.launcher.widgets.MediaAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val graph = (application as ConsolaApp).graph
    private val installed = MutableStateFlow<List<LoadedApp>>(emptyList())

    // Un "tic" al empezar cada minuto: con eso los fondos que cambian solos (cada X minutos, día/noche) cambian a tiempo.
    private val minuteTicker = flow {
        while (true) {
            emit(Unit)
            delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }

    // Cuatro fuentes, un estado: si cambian las apps, los ajustes, los videos o el minuto, el inicio se recalcula solo.
    val state: StateFlow<HomeUiState> =
        combine(installed, graph.settings.settings, graph.backgrounds.videos, minuteTicker) { apps, settings, videos, _ ->
            buildState(apps, settings, videos)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    init {
        // Si subís una portada desde el celular, el Inicio la muestra sin tener que salir y volver.
        viewModelScope.launch { graph.covers.version.drop(1).collect { refresh() } }
    }

    /** Leer paquetes y decodificar banners es I/O: fuera del hilo principal, o el D-pad se traba. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            installed.value = graph.apps.loadAll()
            graph.backgrounds.rescan()
        }
    }

    /** "¿Qué está sonando?" para el widget. Solo escucha mientras el Inicio está visible. */
    val nowPlaying = graph.nowPlaying.state

    /** Graves/medios/agudos de lo que suena, para el fondo que baila. */
    val audioLevels = graph.audioPulse.levels

    /** Las barras del sonido, para el visualizador. */
    val audioSpectrum = graph.audioPulse.spectrum

    val weather = graph.weather.state

    /** Casi siempre no hace nada: el clima se pide como mucho cada media hora. */
    fun refreshWeather(place: Place?) = graph.weather.refreshIfStale(place)

    fun startPulse() = graph.audioPulse.start()

    fun stopPulse() = graph.audioPulse.stop()

    fun startMedia() = graph.nowPlaying.start()

    fun stopMedia() = graph.nowPlaying.stop()

    fun mediaAction(action: MediaAction) = graph.nowPlaying.perform(action)

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch { graph.settings.update { it.toggleFavorite(packageName) } }
    }

    fun reorderFavorites(visibleOrder: List<String>) {
        viewModelScope.launch { graph.settings.update { it.withVisibleOrder(visibleOrder) } }
    }

    private fun buildState(apps: List<LoadedApp>, settings: LauncherSettings, videos: List<String>): HomeUiState {
        val byPackage = apps.associateBy { it.app.packageName }
        val selfPackage = getApplication<Application>().packageName
        val installedApps = apps.map { it.app }

        fun tile(packageName: String): HomeTile {
            val loaded = byPackage.getValue(packageName)
            return HomeTile(
                packageName = packageName,
                // Tu nombre propio (Ajustes desde el celular) manda sobre el de la app.
                label = settings.customNames[packageName] ?: loaded.app.label,
                artwork = loaded.visuals.artwork,
                accentArgb = loaded.visuals.accentArgb,
                isFavorite = packageName in settings.favorites,
                category = loaded.app.category,
            )
        }

        val allApps = AppCatalog.arrange(installedApps, selfPackage, favorites = emptyList(), hidden = settings.hidden)
        val favorites = allApps.filter { it.packageName in settings.favorites }
        // Inicio muestra las favoritas en SU orden. Si no hay ninguna, mostramos todo: nunca un inicio vacío.
        val home = if (favorites.isEmpty()) allApps else AppCatalog.arrange(favorites, selfPackage, settings.favorites, emptySet())

        return HomeUiState(
            homeTiles = home.map { tile(it.packageName) },
            allApps = allApps.map { tile(it.packageName) },
            backgroundVideo = BackgroundCatalog.resolveTimed(
                choice = settings.background,
                videos = videos,
                tuning = settings.tuning,
                time = LocalTime.now(),
                epochMinute = System.currentTimeMillis() / 60_000,
            )?.let(graph.backgrounds::uriOf),
            settings = settings,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
