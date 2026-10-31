package com.longweekend.optimizer.repository

import com.longweekend.optimizer.testing.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class SettingsRepositoryTest {

    private val prefs = FakeSharedPreferences()

    @Test
    fun allowanceStaysWithinRangeAndCapsTheDefaultBudget() {
        val settings = SettingsRepository(prefs)
        settings.setDefaultLeaveBudget(15)

        settings.setAnnualAllowance(100)
        assertEquals(SettingsRepository.ALLOWANCE_RANGE.last, settings.annualAllowance.value)

        settings.setAnnualAllowance(10)
        assertEquals(10, settings.defaultLeaveBudget.value)

        settings.setAnnualAllowance(0)
        assertEquals(1, settings.annualAllowance.value)
        assertEquals(1, settings.defaultLeaveBudget.value)
    }

    @Test
    fun settingsSurviveARestart() {
        SettingsRepository(prefs).apply {
            setDefaultCountryCode("AE")
            setAnnualAllowance(30)
            setDefaultLeaveBudget(7)
            setFirstWorkday(DayOfWeek.SUNDAY)
            setThemeMode(ThemeMode.OLED)
            setOnboardingDone(true)
        }

        val restored = SettingsRepository(prefs)
        assertEquals("AE", restored.defaultCountryCode.value)
        assertEquals(30, restored.annualAllowance.value)
        assertEquals(7, restored.defaultLeaveBudget.value)
        assertEquals(DayOfWeek.SUNDAY, restored.firstWorkday.value)
        assertEquals(ThemeMode.OLED, restored.themeMode.value)
        assertTrue(restored.onboardingDone.value)
    }

    @Test
    fun damagedValuesFallBackToDefaults() {
        prefs.edit()
            .putString("theme_mode", "NEON")
            .putString("first_workday", "WEDNESDAY")
            .putString("default_country", "HR")
            .putInt("annual_allowance", 500)
            .apply()

        val settings = SettingsRepository(prefs)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode.value)
        assertEquals(DayOfWeek.MONDAY, settings.firstWorkday.value)
        assertEquals(SettingsRepository.ALLOWANCE_RANGE.last, settings.annualAllowance.value)
    }
}
