package com.consola.launcher.settings

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.consola.launcher.ConsolaApp
import com.consola.launcher.apps.AppCatalog
import com.consola.launcher.apps.LoadedApp
import com.consola.launcher.background.BackgroundCatalog
import com.consola.launcher.home.HomeTile
import com.consola.launcher.upload.UploadUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppSettingRow(
    val tile: HomeTile,
    val isHidden: Boolean,
)

/** Una tarjeta de fondo. [fileName] null = la aurora animada. */
data class BackgroundCard(
    val choice: BackgroundChoice,
    val fileName: String?,
    val thumbnail: ImageBitmap?,
    val isActive: Boolean,
)

data class SettingsUiState(
    val apps: List<AppSettingRow> = emptyList(),
    val backgrounds: List<BackgroundCard> = emptyList(),
    val upload: UploadUiState = UploadUiState(),
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val graph = (application as ConsolaApp).graph
    private val installed = MutableStateFlow<List<LoadedApp>>(emptyList())
    private val thumbnails = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    val state: StateFlow<SettingsUiState> =
        combine(installed, graph.settings.settings, graph.backgrounds.videos, thumbnails, graph.uploads.state) { apps, settings, videos, thumbs, upload ->
            SettingsUiState(
                apps = appRows(apps, settings),
                backgrounds = backgroundCards(settings, videos, thumbs),
                upload = upload,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            installed.value = graph.apps.loadAll()
            graph.backgrounds.rescan()
        }
        // Cada vez que cambia la lista de videos (subiste o borraste uno), sacamos las fotos que falten.
        viewModelScope.launch(Dispatchers.IO) {
            graph.backgrounds.videos.collect { names ->
                thumbnails.value = names.mapNotNull { name ->
                    val file = graph.backgrounds.fileOf(name) ?: return@mapNotNull null
                    graph.thumbnails.of(file)?.let { name to it }
                }.toMap()
            }
        }
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch { graph.settings.update { it.toggleFavorite(packageName) } }
    }

    fun toggleHidden(packageName: String) {
        viewModelScope.launch { graph.settings.update { it.toggleHidden(packageName) } }
    }

    fun selectBackground(choice: BackgroundChoice) {
        viewModelScope.launch { graph.settings.update { it.copy(background = choice) } }
    }

    // Abrir y cerrar un socket es I/O. Usamos el scope de la app: si el ViewModel se destruye
    // justo mientras cerramos, el servidor igual tiene que cerrarse.
    fun startUploads() {
        graph.appScope.launch(Dispatchers.IO) { graph.uploads.start() }
    }

    fun stopUploads() {
        graph.appScope.launch(Dispatchers.IO) { graph.uploads.stop() }
    }

    override fun onCleared() = stopUploads()

    private fun appRows(apps: List<LoadedApp>, settings: LauncherSettings): List<AppSettingRow> {
        val byPackage = apps.associateBy { it.app.packageName }
        val selfPackage = getApplication<Application>().packageName
        // Favoritas primero (en su orden) y después el resto. Acá SÍ aparecen las ocultas: es donde se des-ocultan.
        return AppCatalog.arrange(apps.map { it.app }, selfPackage, settings.favorites, hidden = emptySet())
            .map { app ->
                val visuals = byPackage.getValue(app.packageName).visuals
                AppSettingRow(
                    tile = HomeTile(app.packageName, app.label, visuals.artwork, visuals.accentArgb, app.packageName in settings.favorites),
                    isHidden = app.packageName in settings.hidden,
                )
            }
    }

    private fun backgroundCards(
        settings: LauncherSettings,
        videos: List<String>,
        thumbs: Map<String, ImageBitmap>,
    ): List<BackgroundCard> {
        val active = BackgroundCatalog.resolve(settings.background, videos)
        val aurora = BackgroundCard(BackgroundChoice.Aurora, fileName = null, thumbnail = null, isActive = active == null)
        return listOf(aurora) + videos.map { name ->
            BackgroundCard(BackgroundChoice.Video(name), name, thumbs[name], isActive = name == active)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
