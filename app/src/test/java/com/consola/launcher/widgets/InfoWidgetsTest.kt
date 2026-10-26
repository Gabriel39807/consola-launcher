package com.consola.launcher.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class InfoWidgetsTest {

    private val today = LocalDate.of(2026, 10, 20)

    @Test
    fun `cuenta regresiva en dias`() {
        assertEquals(CountdownStatus.Upcoming(12), CountdownStatus.of(today, LocalDate.of(2026, 11, 1)))
        assertEquals(CountdownStatus.Tomorrow, CountdownStatus.of(today, LocalDate.of(2026, 10, 21)))
        assertEquals(CountdownStatus.Today, CountdownStatus.of(today, today))
        assertEquals(CountdownStatus.Past(3), CountdownStatus.of(today, LocalDate.of(2026, 10, 17)))
    }

    @Test
    fun `la cuenta regresiva viaja ida y vuelta en los ajustes`() {
        val countdown = Countdown("Vacaciones", LocalDate.of(2026, 12, 20))
        assertEquals(countdown, Countdown.decode(countdown.encode()))
        assertNull(Countdown.decode("sin fecha"))
        assertNull(Countdown.decode(""))
    }

    @Test
    fun `octubre de 2026 empieza un jueves y tiene 5 semanas`() {
        val weeks = MonthGrid.weeks(YearMonth.of(2026, 10), firstDay = DayOfWeek.MONDAY)

        assertEquals(5, weeks.size)
        assertEquals(listOf(null, null, null, 1, 2, 3, 4), weeks.first())
        assertEquals(listOf(26, 27, 28, 29, 30, 31, null), weeks.last())
    }

    @Test
    fun `cada semana tiene siempre 7 casillas`() {
        val weeks = MonthGrid.weeks(YearMonth.of(2026, 2), firstDay = DayOfWeek.SUNDAY)
        assertEquals(true, weeks.all { it.size == 7 })
        assertEquals(28, weeks.flatten().count { it != null })
    }
}
