package com.consola.launcher.widgets

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** "Vacaciones" el 20/12. Se carga desde el celular. */
data class Countdown(val title: String, val date: LocalDate) {
    fun encode(): String = "$title\t$date"

    companion object {
        fun decode(raw: String?): Countdown? {
            val parts = raw?.split('\t') ?: return null
            if (parts.size != 2 || parts[0].isBlank()) return null
            val date = try {
                LocalDate.parse(parts[1])
            } catch (e: DateTimeParseException) {
                return null
            }
            return Countdown(parts[0], date)
        }
    }
}

/** Cuánto falta, en palabras de persona: "faltan 12 días", "¡es mañana!", "¡es hoy!", "fue hace 3 días". */
sealed interface CountdownStatus {
    data class Upcoming(val days: Long) : CountdownStatus
    data object Tomorrow : CountdownStatus
    data object Today : CountdownStatus
    data class Past(val days: Long) : CountdownStatus

    companion object {
        fun of(today: LocalDate, date: LocalDate): CountdownStatus {
            val days = ChronoUnit.DAYS.between(today, date)
            return when {
                days > 1 -> Upcoming(days)
                days == 1L -> Tomorrow
                days == 0L -> Today
                else -> Past(-days)
            }
        }
    }
}

/** Las semanas del mes para dibujar el calendario: 7 casillas por semana, null = casilla vacía. Puro. */
object MonthGrid {
    fun weeks(month: YearMonth, firstDay: DayOfWeek): List<List<Int?>> {
        val offset = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
        val cells = List(offset) { null } + (1..month.lengthOfMonth()).map { it }
        return cells.chunked(7).map { week -> week + List(7 - week.size) { null } }
    }
}
