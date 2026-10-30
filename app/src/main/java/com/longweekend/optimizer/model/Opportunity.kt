package com.longweekend.optimizer.model

import java.time.LocalDate

/**
 * A suggested break. Holds data only: titles, badges and date ranges are formatted by the UI
 * in the current language (see ui/text/BreakTexts.kt).
 */
data class Opportunity(
    val id: String,
    val totalDays: Int,
    val leaveDays: Int,
    /** Holidays on working days, i.e. the ones that save leave (a holiday on a Sunday does not). */
    val publicHolidaysCount: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: List<DayStripItem>,
    val isSaved: Boolean = false,
    // Nullable: plans saved before these fields existed are restored without them
    val countryCode: String? = null,
    val countryName: String? = null
) {
    /** Extra days off gained per leave day invested, e.g. take 4 get 10 -> +150%. */
    val gainPercent: Int
        get() = if (leaveDays > 0) (totalDays - leaveDays) * 100 / leaveDays else 0

    val efficiency: Double
        get() = if (leaveDays > 0) totalDays.toDouble() / leaveDays else 0.0

    val holidayDays: List<DayStripItem>
        get() = days.filter { it.type == DayType.PUBLIC_HOLIDAY }

    /** English name of the first holiday in the break (stable, language-independent key). */
    val mainHoliday: String
        get() = holidayDays.firstOrNull()?.holidayName.orEmpty()
}
