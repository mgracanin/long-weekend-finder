package com.longweekend.optimizer.model

import java.time.DayOfWeek

/** A five-day working week starting on a given day; the other two days are the weekend. */
object WorkWeek {
    /** First working days offered in settings: Mon–Fri (most countries) or Sun–Thu (e.g. Israel). */
    val OPTIONS = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)

    /**
     * Supported countries whose standard working week is Sunday–Thursday (weekend Friday–Saturday),
     * per Wikipedia "Workweek and weekend" (checked October 2026). Everywhere else defaults to Mon–Fri.
     */
    val SUNDAY_FIRST_COUNTRIES = setOf("EG", "BH", "DZ", "IQ", "BD", "SD", "YE", "SY", "LY")

    fun defaultFor(countryCode: String): DayOfWeek =
        if (countryCode.uppercase() in SUNDAY_FIRST_COUNTRIES) DayOfWeek.SUNDAY else DayOfWeek.MONDAY

    fun workdays(firstWorkday: DayOfWeek): Set<DayOfWeek> =
        (0L..4L).map { firstWorkday.plus(it) }.toSet()

    fun weekend(firstWorkday: DayOfWeek): Set<DayOfWeek> =
        DayOfWeek.values().toSet() - workdays(firstWorkday)

    /** The seven days in calendar order, starting with [firstWorkday]. */
    fun daysFrom(firstWorkday: DayOfWeek): List<DayOfWeek> =
        (0L..6L).map { firstWorkday.plus(it) }
}
