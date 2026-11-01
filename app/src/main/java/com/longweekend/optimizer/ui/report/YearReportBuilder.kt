@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.longweekend.optimizer.ui.report

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.model.SavedPlan
import com.longweekend.optimizer.model.WorkWeek
import com.longweekend.optimizer.ui.text.appLocale
import com.longweekend.optimizer.ui.text.dateRangeText
import com.longweekend.optimizer.ui.text.daysText
import com.longweekend.optimizer.ui.text.formatDate
import com.longweekend.optimizer.ui.text.includedHolidayLabels
import com.longweekend.optimizer.ui.text.title
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Words the yearly report for [plans] (the saved plans starting in [year]) in the app's language.
 * Public holidays for the calendar are filled in later, once they have been loaded.
 */
@Composable
fun yearReport(
    year: Int,
    plans: List<SavedPlan>,
    countryName: String,
    allowance: Int,
    firstWorkday: DayOfWeek
): YearReport {
    val sorted = plans.sortedBy { it.opportunity.startDate }
    val used = sorted.sumOf { it.opportunity.leaveDays }
    val daysOff = sorted.sumOf { it.opportunity.totalDays }

    val reportPlans = sorted.map { plan ->
        val opp = plan.opportunity
        val leave = opp.days.filter { it.type == DayType.ANNUAL_LEAVE }
        val holidays = opp.includedHolidayLabels()
        ReportPlan(
            title = opp.title(),
            range = opp.dateRangeText(),
            summary = stringResource(R.string.report_plan_summary, daysText(opp.leaveDays), daysText(opp.totalDays), opp.gainPercent),
            leaveHeading = pluralStringResource(R.plurals.share_leave_heading, opp.leaveDays, opp.leaveDays).removeSuffix(":"),
            leaveDays = leave.mapIndexed { i, day ->
                val text = formatDate(day.date, R.string.pattern_full_date).replaceFirstChar { it.titlecase(appLocale()) }
                val requested = plan.checklistItems.getOrNull(i)?.isChecked == true
                val status = stringResource(if (requested) R.string.status_requested else R.string.status_to_request)
                "$text · $status" to requested
            },
            holidays = holidays.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.report_holidays, it.joinToString(", ")) },
            leaveDates = leave.map { it.date }.toSet(),
            holidayDates = opp.holidayDays.map { it.date }.toSet()
        )
    }

    val warnings = buildList {
        if (used > allowance) add(stringResource(R.string.report_warn_over, used, used - allowance, allowance))
        // Overlapping plans would double-book the same leave days
        for (i in sorted.indices) for (j in i + 1 until sorted.size) {
            val a = sorted[i].opportunity
            val b = sorted[j].opportunity
            if (!a.startDate.isAfter(b.endDate) && !b.startDate.isAfter(a.endDate)) {
                add(stringResource(R.string.report_warn_overlap, a.dateRangeText(), b.dateRangeText()))
            }
        }
    }

    val pageFormat = stringResource(R.string.report_page)
    return YearReport(
        year = year,
        locale = appLocale(),
        title = stringResource(R.string.report_title, year),
        subtitle = stringResource(R.string.report_subtitle, countryName, formatDate(LocalDate.now(), R.string.pattern_day_month_year)),
        tiles = listOf(
            stringResource(R.string.report_tile_plans) to "${sorted.size}",
            stringResource(R.string.report_tile_leave) to stringResource(R.string.report_tile_leave_value, used, allowance),
            stringResource(R.string.report_tile_days_off) to "$daysOff",
            stringResource(R.string.report_tile_remaining) to "${(allowance - used).coerceAtLeast(0)}"
        ),
        warnings = warnings,
        calendarTitle = stringResource(R.string.report_calendar_title),
        legend = Triple(
            stringResource(R.string.legend_leave),
            stringResource(R.string.legend_holiday),
            stringResource(R.string.legend_weekend)
        ),
        plansTitle = stringResource(R.string.report_plans_title),
        plans = reportPlans,
        // Until the year's holidays load, at least the ones inside the plans are marked
        publicHolidays = reportPlans.flatMap { it.holidayDates }.toSet(),
        weekend = WorkWeek.weekend(firstWorkday),
        firstWorkday = firstWorkday,
        footer = stringResource(R.string.report_footer),
        pageLabel = { n -> pageFormat.format(n) }
    )
}

/** Writes the report into the app's cache (off the main thread). */
fun Context.writeYearReport(report: YearReport): ReadyPdf {
    val file = pdfCacheFile("long-weekend-${report.year}.pdf")
    YearReportPdf.write(report, file)
    return ReadyPdf(file, report.title)
}
