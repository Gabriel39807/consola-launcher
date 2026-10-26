package com.consola.launcher.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherTest {

    @Test
    fun `los codigos del clima se traducen a algo que se entiende`() {
        assertEquals(Condition.Clear, Condition.fromWmo(0))
        assertEquals(Condition.PartlyCloudy, Condition.fromWmo(2))
        assertEquals(Condition.Cloudy, Condition.fromWmo(3))
        assertEquals(Condition.Fog, Condition.fromWmo(45))
        assertEquals(Condition.Drizzle, Condition.fromWmo(53))
        assertEquals(Condition.Rain, Condition.fromWmo(63))
        assertEquals(Condition.Rain, Condition.fromWmo(81))
        assertEquals(Condition.Snow, Condition.fromWmo(73))
        assertEquals(Condition.Storm, Condition.fromWmo(95))
        assertEquals(Condition.Cloudy, Condition.fromWmo(1234))
    }

    @Test
    fun `lee el pronostico de Open-Meteo`() {
        val json = """
            {"current":{"temperature_2m":24.3,"weather_code":2,"is_day":1},
             "daily":{"temperature_2m_max":[27.1,26.0],"temperature_2m_min":[15.2,14.0]}}
        """.trimIndent()

        assertEquals(Weather(temperatureC = 24.3, condition = Condition.PartlyCloudy, isDay = true, maxC = 27.1, minC = 15.2), WeatherJson.forecast(json))
    }

    @Test
    fun `un pronostico roto no rompe nada`() {
        assertNull(WeatherJson.forecast("{}"))
        assertNull(WeatherJson.forecast("no es json"))
    }

    @Test
    fun `la ciudad sale de la direccion de internet, en cualquiera de los dos servicios`() {
        assertEquals(Place("Bogotá", 4.61, -74.08), WeatherJson.ipPlace("""{"city":"Bogotá","latitude":4.61,"longitude":-74.08}"""))
        assertEquals(Place("Bogotá", 4.61, -74.08), WeatherJson.ipPlace("""{"status":"success","city":"Bogotá","lat":4.61,"lon":-74.08}"""))
        assertNull(WeatherJson.ipPlace("""{"status":"fail"}"""))
    }

    @Test
    fun `buscar una ciudad por nombre`() {
        val json = """{"results":[{"name":"Medellín","latitude":6.25,"longitude":-75.56,"country":"Colombia"}]}"""
        assertEquals(Place("Medellín", 6.25, -75.56), WeatherJson.geocoding(json))
        assertNull(WeatherJson.geocoding("""{"generationtime_ms":0.5}"""))
    }

    @Test
    fun `la temperatura se muestra redondeada en la unidad elegida`() {
        assertEquals("25°", Temperature.format(24.6, fahrenheit = false))
        assertEquals("76°", Temperature.format(24.6, fahrenheit = true))
        assertEquals("-3°", Temperature.format(-2.6, fahrenheit = false))
    }

    @Test
    fun `una ciudad elegida viaja ida y vuelta en los ajustes`() {
        val place = Place("Medellín", 6.25, -75.56)
        assertEquals(place, Place.decode(place.encode()))
        assertNull(Place.decode("basura"))
    }
}
