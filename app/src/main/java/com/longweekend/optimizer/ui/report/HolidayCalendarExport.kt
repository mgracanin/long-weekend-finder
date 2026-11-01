package com.longweekend.optimizer.ui.report

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.longweekend.optimizer.R
import com.longweekend.optimizer.model.PublicHoliday
import com.longweekend.optimizer.model.WorkWeek
import com.longweekend.optimizer.ui.text.appLocale
import com.longweekend.optimizer.ui.text.formatDate
import com.longweekend.optimizer.ui.text.holidayLabel
import com.longweekend.optimizer.ui.text.localName
import com.longweekend.optimizer.ui.theme.CardBorder
import com.longweekend.optimizer.ui.theme.TealPrimary
import com.longweekend.optimizer.viewmodel.MainViewModel
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Words the printable calendar of [holidays] (one country, one year) in the app's language.
 * [isOffline] notes that only the built-in national holidays were available.
 */
@Composable
fun holidayCalendar(
    countryCode: String,
    countryName: String,
    year: Int,
    holidays: List<PublicHoliday>,
    firstWorkday: DayOfWeek,
    isOffline: Boolean
): HolidayCalendar {
    val weekend = WorkWeek.weekend(firstWorkday)
    val regional = stringResource(R.string.tag_regional)
    val weekendTag = stringResource(R.string.tag_weekend)
    val entries = holidays.mapNotNull { h ->
        val date = runCatching { LocalDate.parse(h.date) }.getOrNull() ?: return@mapNotNull null
        val name = holidayLabel(h.name, h.localName, countryCode)
        CalendarHoliday(
            date = date,
            name = name,
            otherName = (if (name == h.name) h.localName else h.name).takeIf { it.isNotBlank() && it != name },
            national = h.global,
            tag = when {
                !h.global -> regional
                date.dayOfWeek in weekend -> weekendTag
                else -> null
            },
            regions = if (h.global) null else regionsText(h.counties)
        )
    }
    val dates = entries.groupBy { it.date }.mapValues { (_, list) -> list.any { it.national } }
    val pageFormat = stringResource(R.string.report_page)
    val moreFormat = stringResource(R.string.calendar_more)
    return HolidayCalendar(
        year = year,
        locale = appLocale(),
        fileName = "public-holidays-$countryCode-$year.pdf",
        title = stringResource(R.string.calendar_title, year),
        subtitle = stringResource(
            R.string.calendar_subtitle,
            countryName,
            stringResource(R.string.holidays_summary, entries.size, weekdayHolidayCount(dates, weekend))
        ),
        notice = if (isOffline) stringResource(R.string.holidays_offline) else null,
        legend = Triple(
            stringResource(R.string.legend_national),
            stringResource(R.string.legend_regional),
            stringResource(R.string.legend_weekend)
        ),
        listTitle = stringResource(R.string.holidays_all_in_year, year),
        holidays = entries,
        weekend = weekend,
        firstWorkday = firstWorkday,
        moreLabel = { n -> moreFormat.format(n) },
        footer = stringResource(R.string.calendar_footer, formatDate(LocalDate.now(), R.string.pattern_day_month_year)),
        pageLabel = { n -> pageFormat.format(n) }
    )
}

/** Writes the calendar into the app's cache (off the main thread). */
fun Context.writeHolidayCalendar(calendar: HolidayCalendar): ReadyPdf {
    val file = pdfCacheFile(calendar.fileName)
    HolidayCalendarPdf.write(calendar, file)
    return ReadyPdf(file, calendar.title)
}

/**
 * Exports the holidays of the country and year selected in [viewModel] as a printable PDF.
 * Shows nothing until they have loaded.
 */
@Composable
fun HolidayCalendarButton(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val country by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val year by viewModel.selectedYear.collectAsStateWithLifecycle()
    val holidays by viewModel.holidays.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOfflineData.collectAsStateWithLifecycle()
    val firstWorkday by viewModel.firstWorkday.collectAsStateWithLifecycle()
    // Searches also load next year, for breaks that run past 31 December
    val yearHolidays = remember(holidays, year) { holidays.filter { it.date.startsWith("$year-") } }
    if (yearHolidays.isEmpty()) return

    val calendar = holidayCalendar(country.countryCode, country.localName(), year, yearHolidays, firstWorkday, isOffline)
    val context = LocalContext.current
    val export = rememberPdfExport()
    PdfExportDialog(export)
    OutlinedButton(
        onClick = { export.start { context.writeHolidayCalendar(calendar) } },
        border = BorderStroke(1.dp, CardBorder),
        shape = CircleShape,
        modifier = modifier
    ) {
        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.calendar_action), color = TealPrimary, style = MaterialTheme.typography.labelLarge)
    }
}
