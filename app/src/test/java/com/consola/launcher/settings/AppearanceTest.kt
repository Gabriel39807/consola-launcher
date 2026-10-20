package com.consola.launcher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppearanceTest {

    @Test
    fun `24 horas sin segundos`() {
        assertEquals("HH:mm", ClockFormat.timePattern(ClockStyle(use24h = true, showSeconds = false)))
    }

    @Test
    fun `12 horas lleva am pm al final`() {
        assertEquals("h:mm a", ClockFormat.timePattern(ClockStyle(use24h = false, showSeconds = false)))
    }

    @Test
    fun `los segundos van antes del am pm`() {
        assertEquals("HH:mm:ss", ClockFormat.timePattern(ClockStyle(use24h = true, showSeconds = true)))
        assertEquals("h:mm:ss a", ClockFormat.timePattern(ClockStyle(use24h = false, showSeconds = true)))
    }

    @Test
    fun `cada estilo de fecha tiene su formato`() {
        assertEquals("EEEE d 'de' MMMM", ClockFormat.datePattern(DateStyle.Full))
        assertEquals("dd/MM/yyyy", ClockFormat.datePattern(DateStyle.Short))
        assertEquals("EEEE", ClockFormat.datePattern(DateStyle.Weekday))
    }

    @Test
    fun `la fecha oculta no tiene formato`() {
        assertNull(ClockFormat.datePattern(DateStyle.Hidden))
    }

    @Test
    fun `next recorre las opciones y vuelve a empezar`() {
        assertEquals(ClockSize.Medium, ClockSize.Small.next())
        assertEquals(ClockSize.Small, ClockSize.Large.next())
    }

    @Test
    fun `un estilo guardado se recupera por nombre`() {
        assertEquals(HomeLayout.Xbox, SettingsCodec.decodeEnum("Xbox", HomeLayout.Cinema))
    }

    @Test
    fun `un estilo desconocido o vacio vuelve al de fabrica`() {
        assertEquals(HomeLayout.Cinema, SettingsCodec.decodeEnum("Wii", HomeLayout.Cinema))
        assertEquals(HomeLayout.Cinema, SettingsCodec.decodeEnum(null, HomeLayout.Cinema))
    }
}
