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
import com.consola.launcher.widgets.NowPlayingSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    val uploads = BackgroundUploads(appContext, backgrounds, settings, apps, covers, appScope)
    val screensaver = ScreensaverSetting(appContext)
    val nowPlaying = NowPlayingSource(appContext)
    val audioPulse = AudioPulse(appContext)
}
