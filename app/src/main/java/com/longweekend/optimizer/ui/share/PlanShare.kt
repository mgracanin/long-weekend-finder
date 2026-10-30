package com.longweekend.optimizer.ui.share

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.AppLinks
import com.longweekend.optimizer.R
import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.ui.text.appLocale
import com.longweekend.optimizer.ui.text.dateRangeText
import com.longweekend.optimizer.ui.text.daysText
import com.longweekend.optimizer.ui.text.formatDate
import com.longweekend.optimizer.ui.text.includedHolidayLabels
import com.longweekend.optimizer.ui.text.title

/** A plan as plain text for e-mail and messengers, plus a subject line for e-mail. */
data class PlanShare(val subject: String, val text: String)

/**
 * What a shared plan says: the break and its dates, the leave days to request (the part the
 * reader acts on), the holidays it uses and what it gains, then where the plan came from.
 */
@Composable
@ReadOnlyComposable
fun planShare(opportunity: Opportunity, countryName: String): PlanShare {
    val title = opportunity.title()
    val range = opportunity.dateRangeText()
    val leaveDates = opportunity.days.filter { it.type == DayType.ANNUAL_LEAVE }
        .map { formatDate(it.date, R.string.pattern_full_date).replaceFirstChar { c -> c.titlecase(appLocale()) } }
    val holidays = opportunity.includedHolidayLabels()

    val text = buildString {
        appendLine("🏖️ $title ($countryName)")
        appendLine(pluralStringResource(R.plurals.share_range_line, opportunity.totalDays, range, opportunity.totalDays))
        appendLine()
        appendLine(pluralStringResource(R.plurals.share_leave_heading, opportunity.leaveDays, opportunity.leaveDays))
        leaveDates.forEach { appendLine("• $it") }
        appendLine()
        if (holidays.isNotEmpty()) appendLine(stringResource(R.string.share_holidays, holidays.joinToString(", ")))
        appendLine(
            stringResource(
                R.string.share_summary,
                daysText(opportunity.leaveDays),
                daysText(opportunity.totalDays),
                opportunity.gainPercent
            )
        )
        appendLine()
        append(stringResource(R.string.share_footer, AppLinks.REPOSITORY))
    }
    return PlanShare(subject = stringResource(R.string.share_subject, title, range), text = text)
}

/** Opens Android's share sheet (e-mail, messengers, SMS…) with the plan. */
fun Context.sharePlan(share: PlanShare, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, share.subject)
        putExtra(Intent.EXTRA_TEXT, share.text)
    }
    startActivity(Intent.createChooser(send, chooserTitle))
}
