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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
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
            }

            when {
                uiStatus == UiStatus.LOADING && yearHolidays.isEmpty() -> StatusMessage(
                    icon = null,
                    title = stringResource(R.string.holidays_loading_title),
                    body = stringResource(R.string.holidays_loading_body, countryName, selectedYear)
                )
                uiStatus == UiStatus.ERROR -> StatusMessage(
                    icon = Icons.Default.ErrorOutline,
                    title = stringResource(R.string.holidays_error_title),
                    body = stringResource(R.string.holidays_error_body),
                    actionLabel = stringResource(R.string.action_try_again),
                    onAction = viewModel::refresh
                )
                isOfflineData && yearHolidays.isEmpty() -> StatusMessage(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.holidays_offline_empty_title),
                    body = stringResource(R.string.holidays_offline_empty_body, countryName),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = viewModel::refresh
                )
                yearHolidays.isEmpty() -> StatusMessage(
                    icon = Icons.Default.EventBusy,
                    title = stringResource(R.string.holidays_empty_title),
                    body = stringResource(R.string.holidays_empty_body, countryName, selectedYear)
                )
                else -> {
                    if (isOfflineData) {
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

                    Text(
                        text = stringResource(R.string.holidays_summary, yearHolidays.size, weekdayCount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )

                    // Year at a glance: two months per row
                    (1..12).chunked(2).forEach { months ->
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

                    Legend()

                    SectionLabel(icon = Icons.Default.EventNote, title = stringResource(R.string.holidays_all_in_year, selectedYear))
                    yearHolidays.forEach { HolidayRow(it, selectedCountry.countryCode, weekend) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
            for (week in 0 until (offset + days + 6) / 7) {
                Row {
                    for (col in 0..6) {
                        val day = week * 7 + col - offset + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day in 1..days) {
                                val date = yearMonth.atDay(day)
                                val dayHolidays = holidays[date].orEmpty()
                                val national = dayHolidays.any { it.global }
                                val regionalOnly = dayHolidays.isNotEmpty() && !national
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                national -> AmberSecondaryContainer
                                                date.dayOfWeek in weekend -> SurfaceContainer
                                                else -> Color.Transparent
                                            }
                                        )
                                        .then(
                                            when {
                                                regionalOnly -> Modifier.border(1.dp, AmberSecondaryContainer, RoundedCornerShape(4.dp))
                                                date == today -> Modifier.border(1.dp, TealPrimary, RoundedCornerShape(4.dp))
                                                else -> Modifier
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$day",
                                        fontSize = 9.sp,
                                        fontWeight = if (national) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (national) AmberOnSecondaryContainer else TextOnSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
