package com.longweekend.optimizer.repository

import android.content.Context
import android.content.SharedPreferences
import com.longweekend.optimizer.model.Country
import com.longweekend.optimizer.model.WorkWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.util.Locale

/** Appearance; SYSTEM follows the phone's dark-theme setting, OLED is a pure-black dark theme. */
enum class ThemeMode { SYSTEM, LIGHT, DARK, OLED }

/** User profile settings, persisted in SharedPreferences. */
class SettingsRepository internal constructor(private val prefs: SharedPreferences) {

    private val _annualAllowance = MutableStateFlow(
        prefs.getInt(KEY_ANNUAL_ALLOWANCE, DEFAULT_ANNUAL_ALLOWANCE).coerceIn(ALLOWANCE_RANGE)
    )
    val annualAllowance: StateFlow<Int> = _annualAllowance.asStateFlow()

    private val _defaultLeaveBudget = MutableStateFlow(
        prefs.getInt(KEY_DEFAULT_BUDGET, DEFAULT_LEAVE_BUDGET).coerceIn(1, _annualAllowance.value)
    )
    val defaultLeaveBudget: StateFlow<Int> = _defaultLeaveBudget.asStateFlow()

    private val _defaultCountryCode = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_COUNTRY, null) ?: deviceCountryCode()
    )
    val defaultCountryCode: StateFlow<String> = _defaultCountryCode.asStateFlow()

    private val _firstWorkday = MutableStateFlow(
        runCatching { DayOfWeek.valueOf(prefs.getString(KEY_FIRST_WORKDAY, null)!!) }
            .getOrNull()?.takeIf { it in WorkWeek.OPTIONS } ?: WorkWeek.defaultFor(_defaultCountryCode.value)
    )
    val firstWorkday: StateFlow<DayOfWeek> = _firstWorkday.asStateFlow()

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, null)!!) }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _onboardingDone = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    fun setDefaultCountryCode(code: String) {
        _defaultCountryCode.value = code
        prefs.edit().putString(KEY_DEFAULT_COUNTRY, code).apply()
    }

    fun setAnnualAllowance(days: Int) {
        val allowance = days.coerceIn(ALLOWANCE_RANGE)
        _annualAllowance.value = allowance
        prefs.edit().putInt(KEY_ANNUAL_ALLOWANCE, allowance).apply()
        if (_defaultLeaveBudget.value > allowance) setDefaultLeaveBudget(allowance)
    }

    fun setDefaultLeaveBudget(days: Int) {
        val budget = days.coerceIn(1, _annualAllowance.value)
        _defaultLeaveBudget.value = budget
        prefs.edit().putInt(KEY_DEFAULT_BUDGET, budget).apply()
    }

    fun setFirstWorkday(day: DayOfWeek) {
        _firstWorkday.value = day
        prefs.edit().putString(KEY_FIRST_WORKDAY, day.name).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setOnboardingDone(done: Boolean) {
        _onboardingDone.value = done
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }

    companion object {
        const val DEFAULT_ANNUAL_ALLOWANCE = 20
        const val DEFAULT_LEAVE_BUDGET = 5
        val ALLOWANCE_RANGE = 1..60

        /** The phone's region when the holiday API covers it, otherwise the UK. */
        fun deviceCountryCode(): String =
            Country.forCode(Locale.getDefault().country)?.countryCode ?: FALLBACK_COUNTRY_CODE

        private const val FALLBACK_COUNTRY_CODE = "GB"
        private const val PREFS_NAME = "user_settings"
        private const val KEY_ANNUAL_ALLOWANCE = "annual_allowance"
        private const val KEY_DEFAULT_BUDGET = "default_leave_budget"
        private const val KEY_DEFAULT_COUNTRY = "default_country"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_FIRST_WORKDAY = "first_workday"
        private const val KEY_THEME_MODE = "theme_mode"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
