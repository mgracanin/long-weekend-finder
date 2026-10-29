@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.longweekend.optimizer.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import com.longweekend.optimizer.model.Country
import com.longweekend.optimizer.model.DayStripItem
import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.model.Opportunity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Formatting for breaks and holidays in the app's current language. */

@Composable
@ReadOnlyComposable
fun appLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
@ReadOnlyComposable
fun Country.localName(): String = displayName(appLocale())

@Composable
@ReadOnlyComposable
fun formatDate(date: LocalDate, patternRes: Int): String =
    date.format(DateTimeFormatter.ofPattern(stringResource(patternRes), appLocale()))

@Composable
@ReadOnlyComposable
fun dateRange(start: LocalDate, end: LocalDate): String =
    stringResource(
        R.string.date_range,
        formatDate(start, R.string.pattern_day_month),
        formatDate(end, R.string.pattern_day_month_year)
    )

@Composable
@ReadOnlyComposable
fun Opportunity.dateRangeText(): String = dateRange(startDate, endDate)

@Composable
@ReadOnlyComposable
fun daysText(days: Int): String = pluralStringResource(R.plurals.days_count, days, days)

@Composable
@ReadOnlyComposable
fun shortWeekday(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, appLocale()).replaceFirstChar { it.titlecase(appLocale()) }

/**
 * Holiday name to show: the country's own name (e.g. "Božić") when the app language is one
 * spoken in that country, otherwise the English name from the API.
 */
@Composable
@ReadOnlyComposable
fun holidayLabel(englishName: String?, localName: String?, countryCode: String?): String {
    val language = appLocale().language
    val useLocal = localName != null && language != "en" && countryCode != null &&
        Locale.getAvailableLocales().any { it.country.equals(countryCode, true) && it.language == language }
    return (if (useLocal) localName else englishName).orEmpty()
}

@Composable
@ReadOnlyComposable
fun DayStripItem.holidayLabel(countryCode: String?): String =
    holidayLabel(holidayName, holidayLocalName, countryCode)

@Composable
@ReadOnlyComposable
fun Opportunity.mainHolidayLabel(): String =
    holidayDays.firstOrNull()?.holidayLabel(countryCode).orEmpty()

/** "Christmas Day (25 Dec)" for each holiday in the break. */
@Composable
@ReadOnlyComposable
fun Opportunity.includedHolidayLabels(): List<String> = holidayDays.map {
    stringResource(R.string.holiday_with_date, it.holidayLabel(countryCode), formatDate(it.date, R.string.pattern_day_month))
}

@Composable
@ReadOnlyComposable
fun Opportunity.title(): String {
    // Matched on the stable English names from the API
    val names = holidayDays.mapNotNull { it.holidayName }
    fun includes(vararg keywords: String) = names.any { n -> keywords.any { n.contains(it, ignoreCase = true) } }
    return when {
        includes("Easter", "Good Friday") -> stringResource(R.string.title_easter)
        includes("Christmas", "New Year", "Boxing", "Stephen") -> stringResource(R.string.title_festive)
        includes("Labour", "Labor", "Workers", "May Day") -> stringResource(R.string.title_may_day)
        includes("All Saints") -> stringResource(R.string.title_all_saints)
        else -> stringResource(R.string.title_generic, mainHolidayLabel())
    }
}

@Composable
@ReadOnlyComposable
fun Opportunity.badge(): String = when {
    efficiency >= 2.5 -> stringResource(R.string.badge_best_ratio, leaveDays, totalDays)
    startDate.monthValue in 6..8 -> stringResource(R.string.badge_summer, totalDays)
    leaveDays == 1 -> stringResource(R.string.badge_one_day)
    else -> stringResource(R.string.badge_smart, leaveDays, totalDays)
}

@Composable
@ReadOnlyComposable
fun Opportunity.gainTag(): String = stringResource(R.string.gain_tag, gainPercent)

@Composable
@ReadOnlyComposable
fun DayType.badgeText(): String = when (this) {
    DayType.PUBLIC_HOLIDAY -> stringResource(R.string.day_badge_holiday)
    DayType.WEEKEND -> stringResource(R.string.day_badge_weekend)
    DayType.ANNUAL_LEAVE -> stringResource(R.string.day_badge_leave)
    DayType.WORKDAY -> ""
}

/** Monday-first single-letter weekday headers, e.g. M T W T F S S / P U S Č P S N. */
@Composable
@ReadOnlyComposable
fun weekdayInitials(): List<String> {
    val locale = appLocale()
    return java.time.DayOfWeek.values().map { it.getDisplayName(TextStyle.NARROW, locale).uppercase(locale) }
}
