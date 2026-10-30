package com.longweekend.optimizer.ui.screens.holidays

import com.longweekend.optimizer.model.WorkWeek
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import com.longweekend.optimizer.ui.text.appLocale
import com.longweekend.optimizer.ui.text.holidayLabel
import com.longweekend.optimizer.ui.text.localName
import com.longweekend.optimizer.ui.text.weekdayInitials
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longweekend.optimizer.model.PublicHoliday
import com.longweekend.optimizer.ui.components.CountryPicker
import com.longweekend.optimizer.ui.components.LongWeekendTopBar
import com.longweekend.optimizer.ui.components.SectionLabel
import com.longweekend.optimizer.ui.components.YearPicker
import com.longweekend.optimizer.ui.theme.*
import com.longweekend.optimizer.viewmodel.MainViewModel
import com.longweekend.optimizer.viewmodel.UiStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Year-at-a-glance calendar and list of every public holiday for the selected country/year. */
@Composable
fun HolidayCalendarScreen(
    viewModel: MainViewModel,
    onNavigateToProfile: () -> Unit
) {
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val holidays by viewModel.holidays.collectAsState()
    val uiStatus by viewModel.uiStatus.collectAsState()
    val isOfflineData by viewModel.isOfflineData.collectAsState()
    val firstWorkday by viewModel.firstWorkday.collectAsState()
    val weekend = WorkWeek.weekend(firstWorkday)

    // Holidays are fetched for the selected year; ignore anything else that slipped in
    val yearHolidays = remember(holidays, selectedYear) {
        holidays.filter { it.localDate()?.year == selectedYear }
    }
    val byDate = remember(yearHolidays) { yearHolidays.groupBy { it.localDate()!! } }
    val countryName = selectedCountry.localName()
    val weekdayCount = byDate.keys.count { it.dayOfWeek !in weekend && byDate.getValue(it).any { h -> h.global } }

    Scaffold(
        topBar = {
            LongWeekendTopBar(
                title = stringResource(R.string.holidays_title),
                subtitle = stringResource(R.string.country_calendar, countryName),
                countryBadge = selectedCountry.countryCode,
                onProfileClick = onNavigateToProfile
            )
        },
        containerColor = SurfaceBg
    ) { padding ->
        // Lazy so the month grids and holiday rows are built only as they scroll into view
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "pickers") { Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionLabel(icon = Icons.Default.Public, title = stringResource(R.string.label_country))
                    CountryPicker(
                        selected = selectedCountry,
                        countries = viewModel.supportedCountries,
                        caption = stringResource(R.string.data_source_caption),
                        onCountrySelected = viewModel::setCountry
                    )
                    SectionLabel(icon = Icons.Default.CalendarToday, title = stringResource(R.string.label_year))
                    YearPicker(
                        selectedYear = selectedYear,
                        currentYear = viewModel.currentYear,
                        onYearSelected = viewModel::setYear
                    )
                }
            } }

            when {
                uiStatus == UiStatus.LOADING && yearHolidays.isEmpty() -> item(key = "status") { StatusMessage(
                    icon = null,
                    title = stringResource(R.string.holidays_loading_title),
                    body = stringResource(R.string.holidays_loading_body, countryName, selectedYear)
                ) }
                uiStatus == UiStatus.ERROR -> item(key = "status") { StatusMessage(
                    icon = Icons.Default.ErrorOutline,
                    title = stringResource(R.string.holidays_error_title),
                    body = stringResource(R.string.holidays_error_body),
                    actionLabel = stringResource(R.string.action_try_again),
                    onAction = viewModel::refresh
                ) }
                isOfflineData && yearHolidays.isEmpty() -> item(key = "status") { StatusMessage(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.holidays_offline_empty_title),
                    body = stringResource(R.string.holidays_offline_empty_body, countryName),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = viewModel::refresh
                ) }
                yearHolidays.isEmpty() -> item(key = "status") { StatusMessage(
                    icon = Icons.Default.EventBusy,
                    title = stringResource(R.string.holidays_empty_title),
                    body = stringResource(R.string.holidays_empty_body, countryName, selectedYear)
                ) }
                else -> {
                    if (isOfflineData) item(key = "offline") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberSecondaryFixed.copy(alpha = 0.6f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudOff, null, tint = AmberOnSecondaryFixed, modifier = Modifier.size(18.dp))
                            Text(
                                text = stringResource(R.string.holidays_offline),
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberOnSecondaryFixed,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = viewModel::refresh) {
                                Text(stringResource(R.string.action_retry), color = TealPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item(key = "summary") { Text(
                        text = stringResource(R.string.holidays_summary, yearHolidays.size, weekdayCount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    ) }

                    // Year at a glance: two months per row
                    items((1..12).chunked(2), key = { "months_${it.first()}" }, contentType = { "months" }) { months ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            months.forEach { month ->
                                MiniMonth(
                                    yearMonth = YearMonth.of(selectedYear, month),
                                    holidays = byDate,
                                    firstWorkday = firstWorkday,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item(key = "legend") { Legend() }

                    item(key = "list_label") {
                        SectionLabel(icon = Icons.Default.EventNote, title = stringResource(R.string.holidays_all_in_year, selectedYear))
                    }
                    items(yearHolidays, key = { "${it.date}_${it.name}" }, contentType = { "holiday" }) {
                        HolidayRow(it, selectedCountry.countryCode, weekend)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon == null) {
            CircularProgressIndicator(color = TealPrimary)
        } else {
            Icon(icon, null, tint = TextOnSurfaceVariant, modifier = Modifier.size(40.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TealPrimary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextOnSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = CircleShape
            ) { Text(actionLabel) }
        }
    }
}

@Composable
private fun MiniMonth(
    yearMonth: YearMonth,
    holidays: Map<LocalDate, List<PublicHoliday>>,
    firstWorkday: DayOfWeek,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    // Columns start on the first working day; the weekend is the other two days
    val offset = (yearMonth.atDay(1).dayOfWeek.value - firstWorkday.value + 7) % 7
    val weekend = WorkWeek.weekend(firstWorkday)
    val columnDays = WorkWeek.daysFrom(firstWorkday)
    val days = yearMonth.lengthOfMonth()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = yearMonth.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, appLocale()).replaceFirstChar { it.titlecase(appLocale()) },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TextOnSurface,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
            Row {
                weekdayInitials(firstWorkday).forEachIndexed { i, d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (columnDays[i] in weekend) AmberSecondary else TextOnSurfaceVariant
                    )
                }
            }
            // The day grid is drawn in one pass: twelve months of per-day boxes and texts would be
            // hundreds of layout nodes, which makes the screen slow to build and to scroll
            val textMeasurer = rememberTextMeasurer(cacheSize = 64)
            val dayStyle = TextStyle(fontSize = 9.sp, color = TextOnSurface)
            val holidayStyle = dayStyle.copy(fontWeight = FontWeight.ExtraBold, color = AmberOnSecondaryContainer)
            val rows = (offset + days + 6) / 7
            // Theme colours are read here; the draw block below is not composable
            val holidayColor = AmberSecondaryContainer
            val weekendColor = SurfaceContainer
            val todayColor = TealPrimary
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(7f / rows)
                    .drawBehind {
                        val cell = size.width / 7
                        val inset = 1.dp.toPx()
                        val stroke = 1.dp.toPx()
                        val radius = CornerRadius(4.dp.toPx())
                        val box = Size(cell - 2 * inset, cell - 2 * inset)
                        for (day in 1..days) {
                            val index = offset + day - 1
                            val topLeft = Offset((index % 7) * cell + inset, (index / 7) * cell + inset)
                            val date = yearMonth.atDay(day)
                            val dayHolidays = holidays[date].orEmpty()
                            val national = dayHolidays.any { it.global }
                            val background = when {
                                national -> holidayColor
                                date.dayOfWeek in weekend -> weekendColor
                                else -> null
                            }
                            if (background != null) drawRoundRect(background, topLeft, box, radius)
                            val outline = when {
                                dayHolidays.isNotEmpty() && !national -> holidayColor
                                date == today -> todayColor
                                else -> null
                            }
                            if (outline != null) drawRoundRect(
                                color = outline,
                                topLeft = topLeft + Offset(stroke / 2, stroke / 2),
                                size = Size(box.width - stroke, box.height - stroke),
                                cornerRadius = radius,
                                style = Stroke(stroke)
                            )
                            val label = textMeasurer.measure("$day", if (national) holidayStyle else dayStyle)
                            drawText(
                                label,
                                topLeft = topLeft + Offset(
                                    (box.width - label.size.width) / 2,
                                    (box.height - label.size.height) / 2
                                )
                            )
                        }
                    }
            )
        }
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
    ) {
        LegendSwatch(Modifier.background(AmberSecondaryContainer), stringResource(R.string.legend_national))
        LegendSwatch(Modifier.border(1.dp, AmberSecondaryContainer, RoundedCornerShape(3.dp)), stringResource(R.string.legend_regional))
        LegendSwatch(Modifier.background(SurfaceContainer), stringResource(R.string.legend_weekend))
        LegendSwatch(Modifier.border(1.dp, TealPrimary, RoundedCornerShape(3.dp)), stringResource(R.string.legend_today))
    }
}

@Composable
private fun LegendSwatch(style: Modifier, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).then(style))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextOnSurfaceVariant)
    }
}

@Composable
private fun HolidayRow(holiday: PublicHoliday, countryCode: String, weekend: Set<DayOfWeek>) {
    val date = holiday.localDate() ?: return
    val isPast = date.isBefore(LocalDate.now())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPast) 0.55f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceLowest)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .width(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (holiday.global) AmberSecondaryContainer else SurfaceContainer)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.month.getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, appLocale()).uppercase(appLocale()),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AmberOnSecondaryContainer
            )
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextOnSurface
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Main name in the app's language; the other-language name underneath when it differs
            val primaryName = holidayLabel(holiday.name, holiday.localName, countryCode)
            val secondaryName = (if (primaryName == holiday.name) holiday.localName else holiday.name)
                .takeIf { it != primaryName }
            Text(primaryName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextOnSurface)
            val subtitle = listOfNotNull(
                date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, appLocale()).replaceFirstChar { it.titlecase(appLocale()) },
                secondaryName
            ).joinToString(" · ")
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextOnSurfaceVariant)
        }
        val tag = when {
            !holiday.global -> stringResource(R.string.tag_regional)
            date.dayOfWeek in weekend -> stringResource(R.string.tag_weekend)
            else -> null
        }
        if (tag != null) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = TextOnSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

private fun PublicHoliday.localDate(): LocalDate? = runCatching { LocalDate.parse(date) }.getOrNull()
