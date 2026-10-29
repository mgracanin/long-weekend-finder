package com.longweekend.optimizer.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OfflineHolidayCalendarTest {

    @Test
    fun easterSunday_matchesKnownDates() {
        val known = mapOf(
            2019 to LocalDate.of(2019, 4, 21),
            2024 to LocalDate.of(2024, 3, 31),
            2025 to LocalDate.of(2025, 4, 20),
            2026 to LocalDate.of(2026, 4, 5),
            2027 to LocalDate.of(2027, 3, 28),
            2030 to LocalDate.of(2030, 4, 21)
        )
        known.forEach { (year, expected) ->
            assertEquals("Easter $year", expected, OfflineHolidayCalendar.easterSunday(year))
        }
    }

    @Test
    fun croatia2026_movableFeastsFollowEaster() {
        val byName = OfflineHolidayCalendar.holidays("HR", 2026).associateBy({ it.name }, { it.date })
        assertEquals("2026-04-06", byName["Easter Monday"])
        assertEquals("2026-06-04", byName["Corpus Christi"])
    }

    @Test
    fun everySupportedCountry_hasItsOwnCalendar() {
        val generic = OfflineHolidayCalendar.holidays("ZZ", 2026).size
        listOf("HR", "GB", "DE", "FR", "IT", "ES", "US", "CA", "AU", "IE").forEach { code ->
            assertTrue("$code falls back to the generic list", OfflineHolidayCalendar.holidays(code, 2026).size > generic)
        }
    }

    @Test
    fun countryWithoutBuiltInCalendar_getsNoInventedHolidays() {
        // Japan has no built-in calendar; a "New Year + Christmas" guess would be wrong data
        assertTrue(OfflineHolidayCalendar.holidays("JP", 2026).isEmpty())
    }

    @Test
    fun northAmerica2026_movableHolidays() {
        val us = OfflineHolidayCalendar.holidays("US", 2026).associateBy({ it.name }, { it.date })
        assertEquals("2026-01-19", us["Martin Luther King, Jr. Day"])
        assertEquals("2026-05-25", us["Memorial Day"])
        assertEquals("2026-11-26", us["Thanksgiving Day"])
        val ca = OfflineHolidayCalendar.holidays("CA", 2026).associateBy({ it.name }, { it.date })
        assertEquals("2026-05-18", ca["Victoria Day"])
        assertEquals("2026-10-12", ca["Thanksgiving"])
    }

    @Test
    fun unitedKingdom2026_bankHolidaysUseCorrectMondays() {
        val byName = OfflineHolidayCalendar.holidays("GB", 2026).associateBy({ it.name }, { it.date })
        assertEquals("2026-04-03", byName["Good Friday"])
        assertEquals("2026-05-04", byName["Early May Bank Holiday"])
        assertEquals("2026-05-25", byName["Spring Bank Holiday"])
        assertEquals("2026-08-31", byName["Summer Bank Holiday"])
    }
}
