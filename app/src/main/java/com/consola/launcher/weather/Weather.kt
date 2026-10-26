package com.consola.launcher.weather

import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

/** El estado del cielo, agrupado como lo dice una persona (no los 28 códigos de la Organización Meteorológica). */
enum class Condition {
    Clear, PartlyCloudy, Cloudy, Fog, Drizzle, Rain, Snow, Storm;

    companion object {
        /** Códigos WMO que usa Open-Meteo. Uno desconocido se muestra como nublado: nunca un hueco. */
        fun fromWmo(code: Int): Condition = when (code) {
            0 -> Clear
            1, 2 -> PartlyCloudy
            3 -> Cloudy
            45, 48 -> Fog
            in 51..57 -> Drizzle
            in 61..67, in 80..82 -> Rain
            in 71..77, 85, 86 -> Snow
            in 95..99 -> Storm
            else -> Cloudy
        }
    }
}

data class Weather(val temperatureC: Double, val condition: Condition, val isDay: Boolean, val maxC: Double, val minC: Double)

/** Una ciudad con sus coordenadas: lo que necesita el pronóstico. */
data class Place(val name: String, val latitude: Double, val longitude: Double) {
    fun encode(): String = "$name\t$latitude\t$longitude"

    companion object {
        fun decode(raw: String?): Place? {
            val parts = raw?.split('\t') ?: return null
            if (parts.size != 3) return null
            val latitude = parts[1].toDoubleOrNull() ?: return null
            val longitude = parts[2].toDoubleOrNull() ?: return null
            return Place(parts[0], latitude, longitude)
        }
    }
}

object Temperature {
    /** "25°": sin decimales, como en cualquier tele o celular. */
    fun format(celsius: Double, fahrenheit: Boolean): String {
        val value = if (fahrenheit) celsius * 9 / 5 + 32 else celsius
        return "${value.roundToInt()}°"
    }
}

/** Lee las respuestas de los servicios del clima. Un JSON roto o incompleto da null: nunca rompe la app. */
object WeatherJson {

    fun forecast(json: String): Weather? = parse(json) { root ->
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        Weather(
            temperatureC = current.getDouble("temperature_2m"),
            condition = Condition.fromWmo(current.getInt("weather_code")),
            isDay = current.optInt("is_day", 1) == 1,
            maxC = daily.getJSONArray("temperature_2m_max").getDouble(0),
            minC = daily.getJSONArray("temperature_2m_min").getDouble(0),
        )
    }

    /** ipapi.co ("latitude") o ip-api.com ("lat"): aceptamos los dos, por si uno no responde. */
    fun ipPlace(json: String): Place? = parse(json) { root ->
        // ip-api.com avisa el error con "status", ipwho.is con "success".
        if (root.optString("status") == "fail" || !root.optBoolean("success", true)) return@parse null
        Place(
            name = root.getString("city"),
            latitude = if (root.has("latitude")) root.getDouble("latitude") else root.getDouble("lat"),
            longitude = if (root.has("longitude")) root.getDouble("longitude") else root.getDouble("lon"),
        )
    }

    fun geocoding(json: String): Place? = parse(json) { root ->
        val first = root.optJSONArray("results")?.optJSONObject(0) ?: return@parse null
        Place(first.getString("name"), first.getDouble("latitude"), first.getDouble("longitude"))
    }

    private inline fun <T> parse(json: String, read: (JSONObject) -> T?): T? = try {
        read(JSONObject(json))
    } catch (e: JSONException) {
        null
    }
}
