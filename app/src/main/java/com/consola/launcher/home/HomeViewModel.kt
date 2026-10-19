package com.consola.launcher.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.consola.launcher.ConsolaApp
import com.consola.launcher.apps.AppCatalog
import com.consola.launcher.apps.LoadedApp
import com.consola.launcher.background.BackgroundCatalog
import com.consola.launcher.settings.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val graph = (application as ConsolaApp).graph
    private val installed = MutableStateFlow<List<LoadedApp>>(emptyList())

    // Tres fuentes, un estado: si cambian las apps, los ajustes o los videos, el inicio se recalcula solo.
    val state: StateFlow<HomeUiState> =
        combine(installed, graph.settings.settings, graph.backgrounds.videos) { apps, settings, videos ->
            buildState(apps, settings, videos)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    /** Leer paquetes y decodificar banners es I/O: fuera del hilo principal, o el D-pad se traba. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            installed.value = graph.apps.loadAll()
            graph.backgrounds.rescan()
        }
    }

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
                label = loaded.app.label,
                artwork = loaded.visuals.artwork,
                accentArgb = loaded.visuals.accentArgb,
                isFavorite = packageName in settings.favorites,
            )
        }

        val allApps = AppCatalog.arrange(installedApps, selfPackage, favorites = emptyList(), hidden = settings.hidden)
        val favorites = allApps.filter { it.packageName in settings.favorites }
        // Inicio muestra las favoritas en SU orden. Si no hay ninguna, mostramos todo: nunca un inicio vacío.
        val home = if (favorites.isEmpty()) allApps else AppCatalog.arrange(favorites, selfPackage, settings.favorites, emptySet())

        return HomeUiState(
            homeTiles = home.map { tile(it.packageName) },
            allApps = allApps.map { tile(it.packageName) },
            backgroundVideo = BackgroundCatalog.resolve(settings.background, videos)?.let(graph.backgrounds::uriOf),
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
