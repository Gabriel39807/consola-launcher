package com.consola.launcher.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        layout = SettingsCodec.decodeEnum(prefs[LAYOUT], HomeLayout.Cinema),
        clock = ClockStyle(
            use24h = prefs[CLOCK_24H] ?: true,
            showSeconds = prefs[CLOCK_SECONDS] ?: false,
            date = SettingsCodec.decodeEnum(prefs[CLOCK_DATE], DateStyle.Full),
            weight = SettingsCodec.decodeEnum(prefs[CLOCK_WEIGHT], ClockWeight.Regular),
            size = SettingsCodec.decodeEnum(prefs[CLOCK_SIZE], ClockSize.Medium),
        ),
        widgets = WidgetSettings(
            nowPlaying = prefs[WIDGET_NOW_PLAYING] ?: true,
            bigClock = prefs[WIDGET_BIG_CLOCK] ?: false,
            player = SettingsCodec.decodeEnum(prefs[WIDGET_PLAYER], PreferredPlayer.Auto),
        ),
        tuning = BackgroundTuning(
            rotation = SettingsCodec.decodeEnum(prefs[BG_ROTATION], Rotation.Off),
            dim = SettingsCodec.decodeEnum(prefs[BG_DIM], DimLevel.None),
            musicReactive = prefs[BG_MUSIC] ?: false,
            dayVideo = prefs[BG_DAY],
            nightVideo = prefs[BG_NIGHT],
        ),
        font = SettingsCodec.decodeEnum(prefs[FONT], FontChoice.System),
        customNames = prefs[CUSTOM_NAMES]?.let(SettingsCodec::decodeNames).orEmpty(),
        dreamMessage = prefs[DREAM_MESSAGE].orEmpty(),
        savedThemes = prefs[SAVED_THEMES]?.let(SettingsCodec::decodeThemes).orEmpty(),
    )

    private fun write(prefs: MutablePreferences, settings: LauncherSettings) {
        prefs[FAVORITES] = SettingsCodec.encodeFavorites(settings.favorites)
        prefs[HIDDEN] = settings.hidden
        val background = settings.background
        if (background == null) prefs.remove(BACKGROUND) else prefs[BACKGROUND] = SettingsCodec.encodeBackground(background)
        prefs[LAYOUT] = settings.layout.name
        prefs[CLOCK_24H] = settings.clock.use24h
        prefs[CLOCK_SECONDS] = settings.clock.showSeconds
        prefs[CLOCK_DATE] = settings.clock.date.name
        prefs[CLOCK_WEIGHT] = settings.clock.weight.name
        prefs[CLOCK_SIZE] = settings.clock.size.name
        prefs[WIDGET_NOW_PLAYING] = settings.widgets.nowPlaying
        prefs[WIDGET_BIG_CLOCK] = settings.widgets.bigClock
        prefs[WIDGET_PLAYER] = settings.widgets.player.name
        prefs[BG_ROTATION] = settings.tuning.rotation.name
        prefs[BG_DIM] = settings.tuning.dim.name
        prefs[BG_MUSIC] = settings.tuning.musicReactive
        prefs.putOrRemove(BG_DAY, settings.tuning.dayVideo)
        prefs.putOrRemove(BG_NIGHT, settings.tuning.nightVideo)
        prefs[FONT] = settings.font.name
        prefs[CUSTOM_NAMES] = SettingsCodec.encodeNames(settings.customNames)
        prefs[DREAM_MESSAGE] = settings.dreamMessage
        prefs[SAVED_THEMES] = SettingsCodec.encodeThemes(settings.savedThemes)
    }

    private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val FAVORITES = stringPreferencesKey("favorites")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val BACKGROUND = stringPreferencesKey("background")
        val LAYOUT = stringPreferencesKey("layout")
        val CLOCK_24H = booleanPreferencesKey("clock_24h")
        val CLOCK_SECONDS = booleanPreferencesKey("clock_seconds")
        val CLOCK_DATE = stringPreferencesKey("clock_date")
        val CLOCK_WEIGHT = stringPreferencesKey("clock_weight")
        val CLOCK_SIZE = stringPreferencesKey("clock_size")
        val WIDGET_NOW_PLAYING = booleanPreferencesKey("widget_now_playing")
        val WIDGET_BIG_CLOCK = booleanPreferencesKey("widget_big_clock")
        val WIDGET_PLAYER = stringPreferencesKey("widget_player")
        val BG_ROTATION = stringPreferencesKey("bg_rotation")
        val BG_DIM = stringPreferencesKey("bg_dim")
        val BG_MUSIC = booleanPreferencesKey("bg_music")
        val BG_DAY = stringPreferencesKey("bg_day")
        val BG_NIGHT = stringPreferencesKey("bg_night")
        val FONT = stringPreferencesKey("font")
        val CUSTOM_NAMES = stringPreferencesKey("custom_names")
        val DREAM_MESSAGE = stringPreferencesKey("dream_message")
        val SAVED_THEMES = stringPreferencesKey("saved_themes")
    }
}
