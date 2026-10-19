package com.consola.launcher.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.consola.launcher.LauncherConfig
import com.consola.launcher.apps.AppCatalog
import com.consola.launcher.apps.InstalledAppsSource
import com.consola.launcher.background.BackgroundLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appsSource = InstalledAppsSource(application)
    private val backgrounds = BackgroundLibrary(application)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Leer paquetes y decodificar banners es I/O: fuera del hilo principal, o el D-pad se traba. */
    fun refresh() {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.IO) {
                HomeUiState(tiles = loadTiles(), backgroundVideo = backgrounds.currentVideo())
            }
        }
    }

    private fun loadTiles(): List<HomeTile> {
        val apps = AppCatalog.arrange(
            apps = appsSource.installedApps(),
            selfPackage = getApplication<Application>().packageName,
            favorites = LauncherConfig.FAVORITES,
            hidden = LauncherConfig.HIDDEN,
        )
        val appTiles: List<HomeTile> = apps.map { app ->
            val visuals = appsSource.visualsFor(app.packageName)
            HomeTile.App(
                packageName = app.packageName,
                label = app.label,
                artwork = visuals.artwork,
                accentArgb = visuals.accentArgb,
                isFavorite = app.packageName in LauncherConfig.FAVORITES,
            )
        }
        return appTiles + HomeTile.Settings
    }
}
