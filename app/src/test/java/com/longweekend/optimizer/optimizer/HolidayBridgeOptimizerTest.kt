package com.longweekend.optimizer.optimizer

import com.longweekend.optimizer.model.PublicHoliday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidayBridgeOptimizerTest {

    private fun holiday(date: String, name: String, global: Boolean = true) =
        PublicHoliday(date = date, localName = name, name = name, countryCode = "XX", global = global)

    @Test
    fun thursdayHoliday_bridgesWithOneLeaveDay() {
        // Thu 4 Jun 2026: taking Fri 5 Jun gives Thu–Sun off
        val result = HolidayBridgeOptimizer.generateOpportunities(
            holidays = listOf(holiday("2026-06-04", "Corpus Christi")),
            year = 2026,
            leaveBudget = 1
        )
        val bridge = result.single()
        assertEquals(LocalDate.of(2026, 6, 4), bridge.startDate)
        assertEquals(LocalDate.of(2026, 6, 7), bridge.endDate)
        assertEquals(1, bridge.leaveDays)
        assertEquals(4, bridge.totalDays)
        assertEquals(300, bridge.gainPercent)
        assertEquals("Corpus Christi", bridge.mainHoliday)
    }

    @Test
    fun leaveBudget_isNeverExceeded() {
        val result = HolidayBridgeOptimizer.generateOpportunities(
            holidays = listOf(holiday("2026-04-06", "Easter Monday"), holiday("2026-05-01", "Labour Day")),
            year = 2026,
            leaveBudget = 3
        )
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.leaveDays in 1..3 })
    }

    @Test
    fun mondayHoliday_extendsIntoTheWeekWithSmallBudget() {
        // Easter Monday 6 Apr 2026: 3 leave days buy 6 days off (e.g. Thu 2 – Tue 7 or Sat 4 – Thu 9);
        // the old "must end on a day off" rule found nothing here
        val result = HolidayBridgeOptimizer.generateOpportunities(
            holidays = listOf(holiday("2026-04-06", "Easter Monday")),
            year = 2026,
            leaveBudget = 3
        )
        assertTrue(result.any { it.leaveDays == 3 && it.totalDays == 6 })
    }

    @Test
    fun breaks_neverBorderAnotherDayOff() {
        val result = HolidayBridgeOptimizer.generateOpportunities(
            holidays = listOf(holiday("2026-04-06", "Easter Monday"), holiday("2026-06-04", "Corpus Christi")),
            year = 2026,
            leaveBudget = 5
        )
        assertTrue(result.isNotEmpty())
        result.forEach {
            val before = it.startDate.minusDays(1).dayOfWeek.value
            val after = it.endDate.plusDays(1).dayOfWeek.value
            assertTrue("${it.id} could start earlier", before in 1..5)
            assertTrue("${it.id} could end later", after in 1..5)
        }
    }

    @Test
    fun yearBoundary_usesNextYearsHolidaysAndStartsInSelectedYear() {
        val holidays = listOf(
            holiday("2028-12-25", "Christmas Day"),
            holiday("2028-12-26", "St. Stephen's Day"),
            holiday("2029-01-01", "New Year's Day")
        )
        val result = HolidayBridgeOptimizer.generateOpportunities(holidays, 2028, leaveBudget = 5)
        assertTrue(result.all { it.startDate.year == 2028 })
        // New Year's Day 2029 is a holiday, never a leave day
        assertTrue(result.flatMap { it.days }.none { it.date == LocalDate.of(2029, 1, 1) && it.type.name == "ANNUAL_LEAVE" })
    }

    @Test
    fun results_offerSeveralLeaveDayOptionsPerHoliday() {
        // Corpus Christi on Thursday: 1 day (Thu–Sun) and longer options should all survive
        val result = HolidayBridgeOptimizer.generateOpportunities(
            holidays = listOf(holiday("2026-06-04", "Corpus Christi")),
            year = 2026,
            leaveBudget = 5
        )
        val leaveCounts = result.map { it.leaveDays }.toSet()
        assertTrue("leave options: $leaveCounts", 1 in leaveCounts && leaveCounts.size >= 3)
    }

    @Test
    fun results_containNoDominatedBreaks() {
        val result = HolidayBridgeOptimizer.generateOpportunities(
            holidays = listOf(holiday("2026-04-06", "Easter Monday"), holiday("2026-05-01", "Labour Day")),
            year = 2026,
            leaveBudget = 6
        )
        result.forEach { a ->
            val dominator = result.firstOrNull { b ->
                b !== a && !a.startDate.isAfter(b.endDate) && !b.startDate.isAfter(a.endDate) &&
                    b.leaveDays <= a.leaveDays && b.totalDays >= a.totalDays
            }
            assertTrue("${a.id} is dominated by ${dominator?.id}", dominator == null)
        }
    }

    @Test
    fun weekendOnlyHoliday_isNotABridge() {
        // All Saints' Day 2026 falls on Sunday 1 Nov: it saves no leave
        val result = HolidayBridgeOptimizer.generateOpportunities(
            holidays = listOf(holiday("2026-11-01", "All Saints' Day")),
            year = 2026,
            leaveBudget = 5
        )
        assertTrue(result.map { it.id }.toString(), result.isEmpty())
    }

    @Test
    fun regionalHolidays_areIgnored() {
        val result = HolidayBridgeOptimizer.generateOpportunities(
            holidays = listOf(holiday("2026-06-04", "Regional Day", global = false)),
            year = 2026,
            leaveBudget = 5
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun seasonFilter_keepsOnlyMatchingMonths() {
        val holidays = listOf(holiday("2026-04-06", "Easter Monday"), holiday("2026-08-05", "Victory Day"))
        val summer = HolidayBridgeOptimizer.generateOpportunities(holidays, 2026, leaveBudget = 4, seasonFilter = "summer")
        assertTrue(summer.isNotEmpty())
        assertTrue(summer.all { it.startDate.monthValue in 6..8 })
        assertFalse(summer.any { it.mainHoliday == "Easter Monday" })
    }
}
