package com.consola.launcher.weather

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed interface WeatherState {
    data object Loading : WeatherState
    data class Ready(val weather: Weather, val place: Place) : WeatherState

    /** Sin internet, o el servicio no respondió: el widget lo dice en vez de quedar vacío. */
    data object Unavailable : WeatherState
}

/**
 * El clima para el widget. Gratis y sin cuenta: Open-Meteo para el pronóstico y para buscar ciudades.
 * Si no elegiste ciudad, la deducimos de tu conexión a internet (ipapi.co, o ipwho.is si ese no responde).
 *
 * Se actualiza como mucho cada 30 minutos: el clima no cambia tan rápido y no hace falta gastar red.
 */
class WeatherSource(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    private var lastFetchAt = 0L
    private var lastPlaceKey: String? = null
    private var fetching = false

    /** [place] null = automática. Lo llama el Inicio al mostrarse y cada minuto; casi siempre no hace nada. */
    fun refreshIfStale(place: Place?) {
        val key = place?.encode() ?: AUTO
        val fresh = SystemClock.elapsedRealtime() - lastFetchAt < REFRESH_MS
        if (fetching || (key == lastPlaceKey && fresh && _state.value is WeatherState.Ready)) return
        // Si cambiaste de ciudad, que se vea que está cargando (y no el clima de la ciudad anterior).
        if (key != lastPlaceKey) _state.value = WeatherState.Loading
        fetching = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { fetch(place) }.getOrNull() }
            _state.value = result ?: WeatherState.Unavailable
            lastPlaceKey = key
            // Si falló, reintentamos en el próximo minuto, no en media hora.
            lastFetchAt = if (result != null) SystemClock.elapsedRealtime() else 0L
            fetching = false
        }
    }

    /** "Medellín" → la ciudad con sus coordenadas, o null si no existe. Bloquea: llamar fuera del hilo principal. */
    fun findCity(name: String): Place? = runCatching {
        WeatherJson.geocoding(get("$GEOCODING?name=${URLEncoder.encode(name, "UTF-8")}&count=1&language=es"))
    }.getOrNull()

    private fun fetch(chosen: Place?): WeatherState.Ready? {
        val place = chosen ?: autoPlace() ?: return null
        val url = "$FORECAST?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
        val weather = WeatherJson.forecast(get(url)) ?: return null
        return WeatherState.Ready(weather, place)
    }

    private fun autoPlace(): Place? =
        IP_SERVICES.firstNotNullOfOrNull { url -> runCatching { WeatherJson.ipPlace(get(url)) }.getOrNull() }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "Consola-Launcher")
        try {
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val AUTO = "auto"
        const val REFRESH_MS = 30 * 60 * 1000L
        const val TIMEOUT_MS = 6_000
        const val FORECAST = "https://api.open-meteo.com/v1/forecast"
        const val GEOCODING = "https://geocoding-api.open-meteo.com/v1/search"
        val IP_SERVICES = listOf("https://ipapi.co/json/", "https://ipwho.is/")
    }
}
