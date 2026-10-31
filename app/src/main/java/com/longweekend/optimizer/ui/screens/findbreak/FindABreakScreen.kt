@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.longweekend.optimizer.ui.screens.findbreak

import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.ui.components.CountryChip
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
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val leaveBudget by viewModel.leaveBudget.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()
    val annualAllowance by viewModel.annualAllowance.collectAsStateWithLifecycle()
    val opportunities by viewModel.opportunities.collectAsStateWithLifecycle()
    val topPicks by viewModel.topPicks.collectAsStateWithLifecycle()
    val uiStatus by viewModel.uiStatus.collectAsStateWithLifecycle()
    val isOfflineData by viewModel.isOfflineData.collectAsStateWithLifecycle()
    val countries = viewModel.supportedCountries
    val isLoading = uiStatus == UiStatus.LOADING

    Scaffold(
        topBar = {
            LongWeekendTopBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.app_subtitle),
                countryBadge = selectedCountry.countryCode,
                onProfileClick = onNavigateToProfile
            )
        },
        // One way to the results, always in reach, updating as the inputs change
        bottomBar = {
            ResultsBar(
                status = uiStatus,
                isOffline = isOfflineData,
                count = opportunities.size,
                leaveBudget = leaveBudget,
                onClick = if (uiStatus == UiStatus.ERROR) viewModel::refresh else onNavigateToSuggested
            )
        },
        containerColor = SurfaceBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.discover_headline),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp),
                fontWeight = FontWeight.ExtraBold,
                color = TealPrimary
            )

            // The country rarely changes, so it is a compact pill; every plannable year is one tap away
            CountryChip(
                selected = selectedCountry,
                countries = countries,
                onCountrySelected = viewModel::setCountry
            )
            YearPicker(
                selectedYear = selectedYear,
                currentYear = viewModel.currentYear,
                onYearSelected = viewModel::setYear
            )

            // The one control most people adjust: how many leave days, and when
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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

                    // Season and "spend it all" as one row of light chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SEASONS.forEach { season ->
                            FilterPill(
                                label = stringResource(season.title),
                                selected = selectedSeason == season.key,
                                dimmed = viewModel.seasonHasPassed(season.key, selectedYear),
                                onClick = { viewModel.setSeason(season.key) }
                            )
                        }
                        FilterPill(
                            label = pluralStringResource(R.plurals.preset_all, annualAllowance, annualAllowance),
                            selected = leaveBudget == annualAllowance,
                            onClick = { viewModel.setLeaveBudget(annualAllowance) }
                        )
                    }
                }
            }

            // Top picks: the most efficient bridges for the current search
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.top_picks_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
                when {
                    isLoading -> Text(stringResource(R.string.top_picks_loading), style = MaterialTheme.typography.bodySmall, color = TextOnSurfaceVariant)
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
                                modifier = Modifier.width(180.dp)
                            )
                        }
                    }
                }
            }
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

/** Fixed-height bar above the navigation: the live result of the search and the way into it. */
@Composable
private fun ResultsBar(
    status: UiStatus,
    isOffline: Boolean,
    count: Int,
    leaveBudget: Int,
    onClick: () -> Unit
) {
    val isError = status == UiStatus.ERROR
    val isLoading = status == UiStatus.LOADING
    val text = when {
        isLoading -> stringResource(R.string.home_summary_loading)
        isError -> stringResource(R.string.home_summary_error)
        count == 0 -> stringResource(R.string.home_summary_none, daysText(leaveBudget))
        else -> pluralStringResource(R.plurals.home_summary, count, count, daysText(leaveBudget))
    }
    val action = when {
        isError -> stringResource(R.string.action_retry)
        count == 0 && !isLoading -> stringResource(R.string.action_tips)
        else -> stringResource(R.string.action_see_all)
    }
    Surface(color = SurfaceBg) {
        Surface(
            onClick = onClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(50),
            color = if (isError) ErrorContainer else TealPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(52.dp)
        ) {
            val onBar = if (isError) OnErrorContainer else TealOnPrimary
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // The icon slot keeps its size in every state, so the text never shifts
                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    when {
                        isLoading -> CircularProgressIndicator(color = onBar, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        isOffline -> Icon(Icons.Default.CloudOff, contentDescription = stringResource(R.string.home_offline), tint = onBar, modifier = Modifier.size(18.dp))
                        else -> Icon(Icons.Default.EventAvailable, contentDescription = null, tint = onBar, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = onBar,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = onBar,
                    modifier = Modifier.alpha(if (isLoading) 0f else 1f)
                )
                Icon(
                    imageVector = if (isError) Icons.Default.Refresh else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = onBar,
                    modifier = Modifier.size(18.dp).alpha(if (isLoading) 0f else 1f)
                )
            }
        }
    }
}

/** Light, tonal filter chip; [dimmed] greys out a season that has already passed this year. */
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit, dimmed: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) SelectedFill else SurfaceLowest,
        border = if (selected) null else BorderStroke(1.dp, CardBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                selected -> OnSelectedFill
                dimmed -> TextOnSurfaceVariant.copy(alpha = 0.6f)
                else -> TextOnSurface
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
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
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AmberSecondaryFixed)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = opportunity.mainHolidayLabel(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = AmberOnSecondaryFixed,
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
