package com.consola.launcher.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStore exige UNA sola instancia por archivo: el delegado a nivel de archivo lo garantiza.
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<LauncherSettings> = store.data
        // Un archivo dañado no puede dejar la tele sin inicio: arrancamos con los valores por defecto.
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map(::read)

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        store.edit { prefs -> write(prefs, transform(read(prefs))) }
    }

    private fun read(prefs: Preferences) = LauncherSettings(
        favorites = prefs[FAVORITES]?.let(SettingsCodec::decodeFavorites) ?: LauncherSettings.Default.favorites,
        hidden = prefs[HIDDEN] ?: LauncherSettings.Default.hidden,
        background = SettingsCodec.decodeBackground(prefs[BACKGROUND]),
    )

    private fun write(prefs: MutablePreferences, settings: LauncherSettings) {
        prefs[FAVORITES] = SettingsCodec.encodeFavorites(settings.favorites)
        prefs[HIDDEN] = settings.hidden
        val background = settings.background
        if (background == null) prefs.remove(BACKGROUND) else prefs[BACKGROUND] = SettingsCodec.encodeBackground(background)
    }

    private companion object {
        val FAVORITES = stringPreferencesKey("favorites")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val BACKGROUND = stringPreferencesKey("background")
    }
}
