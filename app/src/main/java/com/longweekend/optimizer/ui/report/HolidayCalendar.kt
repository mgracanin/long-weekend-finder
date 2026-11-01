package com.longweekend.optimizer.ui.report

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** One public holiday as the printable calendar shows it; all text is already translated. */
data class CalendarHoliday(
    val date: LocalDate,
    val name: String,
    /** The name in the other language (local or English) when it differs. */
    val otherName: String?,
    val national: Boolean,
    /** "Regional" or "Weekend", when the holiday doesn't give a working day off everywhere. */
    val tag: String?,
    /** Where a regional holiday applies, e.g. "BW, BY". */
    val regions: String?
)

/** Everything the printable holiday calendar shows; all text is already translated. */
data class HolidayCalendar(
    val year: Int,
    val locale: Locale,
    val fileName: String,
    val title: String,
    val subtitle: String,
    /** Shown in a box under the title, e.g. that only the built-in calendar was available. */
    val notice: String?,
    val legend: Triple<String, String, String>,
    val listTitle: String,
    val holidays: List<CalendarHoliday>,
    val weekend: Set<DayOfWeek>,
    val firstWorkday: DayOfWeek,
    val moreLabel: (Int) -> String,
    val footer: String,
    val pageLabel: (Int) -> String
)

/**
 * The holidays of [month] listed under its grid: national ones first on each date, at most
 * [maxLines]. When they don't all fit, the last line is left for "+N more" and N is returned.
 */
internal fun monthNotes(holidays: List<CalendarHoliday>, month: YearMonth, maxLines: Int): Pair<List<CalendarHoliday>, Int> {
    val inMonth = holidays
        .filter { YearMonth.from(it.date) == month }
        .sortedWith(compareBy<CalendarHoliday> { it.date }.thenBy { !it.national })
    if (inMonth.size <= maxLines) return inMonth to 0
    val shown = inMonth.take(maxLines - 1)
    return shown to inMonth.size - shown.size
}

/** How many dates have a national holiday on a working day, i.e. a day off it gives. */
internal fun weekdayHolidayCount(dates: Map<LocalDate, Boolean>, weekend: Set<DayOfWeek>): Int =
    dates.count { (date, national) -> national && date.dayOfWeek !in weekend }

/** County codes of a regional holiday without the country prefix: ["DE-BW", "DE-BY"] -> "BW, BY". */
internal fun regionsText(counties: List<String>?): String? =
    counties?.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.substringAfter('-') }
