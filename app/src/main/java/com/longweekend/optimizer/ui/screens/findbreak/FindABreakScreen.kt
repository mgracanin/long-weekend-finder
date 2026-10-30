@file:OptIn(ExperimentalComposeUiApi::class)

package com.longweekend.optimizer.ui.screens.findbreak

import java.time.DayOfWeek
import androidx.annotation.StringRes
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import com.longweekend.optimizer.ui.text.dateRangeText
import com.longweekend.optimizer.ui.text.daysText
import com.longweekend.optimizer.ui.text.localName
import com.longweekend.optimizer.ui.text.mainHolidayLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.ui.components.CountryPicker
import com.longweekend.optimizer.ui.components.DaysStepper
import com.longweekend.optimizer.ui.components.LongWeekendTopBar
import com.longweekend.optimizer.ui.components.SectionLabel
import com.longweekend.optimizer.ui.components.YearPicker
import com.longweekend.optimizer.ui.theme.*
import com.longweekend.optimizer.viewmodel.MainViewModel
import com.longweekend.optimizer.viewmodel.UiStatus

@Composable
fun FindABreakScreen(
    viewModel: MainViewModel,
    onNavigateToSuggested: () -> Unit,
    onNavigateToDetail: (Opportunity) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val leaveBudget by viewModel.leaveBudget.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val annualAllowance by viewModel.annualAllowance.collectAsState()
    val opportunities by viewModel.opportunities.collectAsState()
    val topPicks by viewModel.topPicks.collectAsState()
    val uiStatus by viewModel.uiStatus.collectAsState()
    val firstWorkday by viewModel.firstWorkday.collectAsState()
    val isOfflineData by viewModel.isOfflineData.collectAsState()
    val countries = viewModel.supportedCountries
    val isLoading = uiStatus == UiStatus.LOADING
    val seasonPassed = viewModel.seasonHasPassed(selectedSeason, selectedYear)

    Scaffold(
        topBar = {
            LongWeekendTopBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.app_subtitle),
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.discover_headline),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = TealPrimary
                )
                Text(
                    text = stringResource(R.string.discover_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant
                )
            }

            // Opportunity Radar: live summary of the current search; tapping it opens the results
            RadarCard(
                status = uiStatus,
                isOffline = isOfflineData,
                headline = when {
                    uiStatus == UiStatus.ERROR -> stringResource(R.string.radar_error)
                    seasonPassed -> stringResource(
                        R.string.radar_season_passed,
                        stringResource(SEASONS.first { it.key == selectedSeason }.title),
                        selectedYear,
                        selectedYear + 1
                    )
                    else -> radarText(isLoading, opportunities.size, selectedYear, selectedCountry.localName(), leaveBudget)
                },
                onClick = if (uiStatus == UiStatus.ERROR) viewModel::refresh else onNavigateToSuggested
            )

            // Interactive Search / Optimization Form Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Country Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel(icon = Icons.Default.Public, title = stringResource(R.string.label_your_country))

                        CountryPicker(
                            selected = selectedCountry,
                            countries = countries,
                            caption = stringResource(R.string.data_source_caption),
                            onCountrySelected = viewModel::setCountry
                        )
                    }

                    // Year Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel(icon = Icons.Default.CalendarToday, title = stringResource(R.string.label_holiday_year))
                        YearPicker(
                            selectedYear = selectedYear,
                            currentYear = viewModel.currentYear,
                            onYearSelected = viewModel::setYear
                        )
                    }

                    // Leave Days Stepper & Presets
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel(
                            icon = Icons.Default.FlightTakeoff,
                            title = stringResource(R.string.label_leave_to_invest),
                            subtitle = pluralStringResource(R.plurals.label_leave_to_invest_hint, annualAllowance, annualAllowance)
                        )

                        DaysStepper(
                            value = leaveBudget,
                            range = 1..annualAllowance,
                            unitLabel = pluralStringResource(R.plurals.working_days_unit, leaveBudget),
                            onValueChange = viewModel::setLeaveBudget
                        )

                        // One shortcut is enough next to the stepper: spend the whole allowance
                        val allSelected = leaveBudget == annualAllowance
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (allSelected) TealPrimary else SurfaceContainerLow)
                                .clickable { viewModel.setLeaveBudget(annualAllowance) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = pluralStringResource(R.plurals.preset_all, annualAllowance, annualAllowance),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (allSelected) TealOnPrimary else TextOnSurfaceVariant
                            )
                        }
                    }

                    // Season Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionLabel(icon = Icons.Default.FilterVintage, title = stringResource(R.string.label_season))

                        SEASONS.chunked(2).forEach { rowSeasons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowSeasons.forEach { season ->
                                    SeasonButton(
                                        title = stringResource(season.title),
                                        subtitle = when {
                                            season.months == null -> stringResource(R.string.season_any_hint, selectedYear)
                                            viewModel.seasonHasPassed(season.key, selectedYear) ->
                                                stringResource(R.string.season_passed, stringResource(season.months))
                                            else -> stringResource(season.months)
                                        },
                                        isSelected = selectedSeason == season.key,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.setSeason(season.key) }
                                    )
                                }
                            }
                        }
                    }

                    // Submit CTA
                    Button(
                        onClick = onNavigateToSuggested,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimaryContainer),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_find_breaks),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TealOnPrimary
                            )
                            Icon(Icons.Default.ArrowForward, null, tint = TealOnPrimary)
                        }
                    }

                    Text(
                        text = stringResource(if (firstWorkday == DayOfWeek.SUNDAY) R.string.working_week_note_sunday else R.string.working_week_note_monday),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextOnSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // Top picks: the most efficient bridges for the current search
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.top_picks_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TealPrimaryFixed)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${selectedCountry.flagEmoji} $selectedYear",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TealOnPrimaryFixed
                        )
                    }
                }

                when {
                    isLoading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = TealPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.top_picks_loading), style = MaterialTheme.typography.bodySmall, color = TextOnSurfaceVariant)
                    }
                    uiStatus == UiStatus.ERROR -> Text(
                        text = stringResource(R.string.top_picks_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnErrorContainer
                    )
                    topPicks.isEmpty() -> Text(
                        text = stringResource(R.string.top_picks_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextOnSurfaceVariant
                    )
                    else -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        topPicks.forEach { opp ->
                            PairingCard(
                                opportunity = opp,
                                onClick = { onNavigateToDetail(opp) },
                                modifier = Modifier.width(190.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class SeasonOption(val key: String, @StringRes val title: Int, @StringRes val months: Int?)

private val SEASONS = listOf(
    SeasonOption("any", R.string.season_any, null),
    SeasonOption("spring", R.string.season_spring, R.string.season_spring_months),
    SeasonOption("summer", R.string.season_summer, R.string.season_summer_months),
    SeasonOption("autumn", R.string.season_autumn, R.string.season_autumn_months)
)

@Composable
private fun radarText(isLoading: Boolean, count: Int, year: Int, country: String, budget: Int): String = when {
    isLoading -> stringResource(R.string.radar_scanning, country, year)
    count == 0 -> stringResource(R.string.radar_none, daysText(budget), year)
    else -> pluralStringResource(R.plurals.radar_count, count, count, daysText(budget), year)
}

@Composable
private fun RadarCard(
    status: UiStatus,
    isOffline: Boolean,
    headline: String,
    onClick: () -> Unit
) {
    val isError = status == UiStatus.ERROR
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) ErrorContainer else SurfaceContainerHigh.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = status != UiStatus.LOADING, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isError) OnErrorContainer else TealPrimary),
                contentAlignment = Alignment.Center
            ) {
                if (status == UiStatus.LOADING) {
                    CircularProgressIndicator(color = TealOnPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TealOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.radar_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isError) OnErrorContainer else TealPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = TextOnSurface
                )
                val hint = when {
                    status == UiStatus.LOADING -> null
                    isError -> stringResource(R.string.radar_hint_retry)
                    isOffline -> stringResource(R.string.radar_hint_offline)
                    status == UiStatus.EMPTY -> stringResource(R.string.radar_hint_tips)
                    else -> stringResource(R.string.radar_hint_results)
                }
                if (hint != null) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isError) OnErrorContainer else TextOnSurfaceVariant
                    )
                }
            }
            if (status != UiStatus.LOADING) {
                Icon(
                    imageVector = if (isError) Icons.Default.Refresh else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isError) OnErrorContainer else TealPrimary
                )
            }
        }
    }
}

@Composable
private fun SeasonButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) TealPrimary else SurfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = if (isSelected) TealOnPrimary else TextOnSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) TealOnPrimary.copy(alpha = 0.8f) else TextOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun PairingCard(
    opportunity: Opportunity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(TealPrimary)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = opportunity.mainHolidayLabel(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = TealOnPrimary,
                    maxLines = 1
                )
            }
            Text(
                text = opportunity.dateRangeText(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextOnSurface
            )
            Text(
                text = stringResource(R.string.take_get, opportunity.leaveDays, opportunity.totalDays),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AmberSecondary
            )
        }
    }
}
