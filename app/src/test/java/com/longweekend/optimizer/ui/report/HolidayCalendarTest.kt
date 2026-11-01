package com.longweekend.optimizer.ui.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class HolidayCalendarTest {

    private fun entry(date: String, name: String = date, national: Boolean = true) = CalendarHoliday(
        date = LocalDate.parse(date),
        name = name,
        otherName = null,
        national = national,
        tag = null,
        regions = null
    )

    private val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    @Test
    fun monthNotesListOnlyThatMonthByDateWithNationalFirst() {
        val holidays = listOf(
            entry("2027-01-06", "Epiphany, regional", national = false),
            entry("2027-02-01"),
            entry("2027-01-06", "Epiphany"),
            entry("2027-01-01", "New Year")
        )

        val (notes, more) = monthNotes(holidays, YearMonth.of(2027, 1), maxLines = 5)

        assertEquals(listOf("New Year", "Epiphany", "Epiphany, regional"), notes.map { it.name })
        assertEquals(0, more)
    }

    @Test
    fun aBusyMonthKeepsTheLastLineForHowManyMore() {
        val holidays = (1..7).map { entry("2027-05-0$it") }

        val (fits, none) = monthNotes(holidays.take(5), YearMonth.of(2027, 5), maxLines = 5)
        assertEquals(5, fits.size)
        assertEquals(0, none)

        val (notes, more) = monthNotes(holidays, YearMonth.of(2027, 5), maxLines = 5)
        assertEquals(4, notes.size)
        assertEquals(3, more)
    }

    @Test
    fun onlyNationalHolidaysOnWorkingDaysCountAsDaysOff() {
        val dates = mapOf(
            LocalDate.of(2027, 1, 1) to true,   // Friday
            LocalDate.of(2027, 1, 6) to false,  // Wednesday, regional only
            LocalDate.of(2027, 5, 1) to true,   // Saturday
            LocalDate.of(2027, 12, 24) to true  // Friday
        )

        assertEquals(2, weekdayHolidayCount(dates, weekend))
        assertEquals("a Sunday–Thursday week has Friday off anyway", 0, weekdayHolidayCount(dates, setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)))
    }

    @Test
    fun regionsDropTheCountryPrefix() {
        assertEquals("BW, BY", regionsText(listOf("DE-BW", "DE-BY")))
        assertNull(regionsText(emptyList()))
        assertNull(regionsText(null))
    }
}
