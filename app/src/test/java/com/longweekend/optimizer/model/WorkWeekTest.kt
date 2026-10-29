package com.longweekend.optimizer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class WorkWeekTest {

    @Test
    fun sundayToThursdayCountries_defaultToSunday() {
        listOf("EG", "BH", "DZ", "IQ", "BD", "SD", "YE", "SY", "LY").forEach {
            assertEquals(it, DayOfWeek.SUNDAY, WorkWeek.defaultFor(it))
        }
    }

    @Test
    fun otherCountries_defaultToMonday() {
        listOf("HR", "GB", "US", "MR", "JP").forEach {
            assertEquals(it, DayOfWeek.MONDAY, WorkWeek.defaultFor(it))
        }
    }

    @Test
    fun sundayFirstCountries_areAllSupportedByTheApi() {
        assertTrue(WorkWeek.SUNDAY_FIRST_COUNTRIES.all { Country.forCode(it) != null })
    }
}
