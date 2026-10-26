package com.consola.launcher

import android.content.Context
import com.consola.launcher.audio.AudioPulse
import com.consola.launcher.apps.CoverStore
import com.consola.launcher.apps.InstalledAppsSource
import com.consola.launcher.background.BackgroundLibrary
import com.consola.launcher.background.VideoThumbnails
import com.consola.launcher.dream.ScreensaverSetting
import com.consola.launcher.settings.SettingsRepository
import com.consola.launcher.settings.settingsDataStore
import com.consola.launcher.upload.BackgroundUploads
import com.consola.launcher.weather.WeatherSource
import com.consola.launcher.widgets.NowPlayingSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Inyección de dependencias a mano: UNA instancia de cada cosa para toda la app.
 * Inicio y Ajustes tienen que ver la MISMA biblioteca de fondos: si subís un video desde el celular,
 * el Inicio se entera porque los dos observan el mismo StateFlow.
 */
class LauncherGraph(context: Context) {

    private val appContext = context.applicationContext

    // Sobrevive a las pantallas: una conversión de video no se corta porque saliste de Ajustes.
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val covers = CoverStore(appContext)
    val apps = InstalledAppsSource(appContext, covers)
    val settings = SettingsRepository(appContext.settingsDataStore)
    val backgrounds = BackgroundLibrary(appContext)
    val thumbnails = VideoThumbnails()
    // Antes que uploads: el celular puede elegir la ciudad del clima, y eso la busca en Open-Meteo.
    val weather = WeatherSource(CoroutineScope(SupervisorJob() + Dispatchers.Main))
    val uploads = BackgroundUploads(appContext, backgrounds, settings, apps, covers, appScope, weather)
    val screensaver = ScreensaverSetting(appContext)
    val nowPlaying = NowPlayingSource(appContext)
    val audioPulse = AudioPulse(appContext)

    init {
        // El widget sigue tu reproductor elegido. En el hilo principal: ahí corre todo NowPlayingSource.
        appScope.launch(Dispatchers.Main) {
            settings.settings.map { it.widgets.player.packageName }.distinctUntilChanged().collect { nowPlaying.preferredPackage = it }
        }
    }
}
