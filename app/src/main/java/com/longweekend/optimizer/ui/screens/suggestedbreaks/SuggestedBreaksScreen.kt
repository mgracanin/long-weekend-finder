@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)

package com.longweekend.optimizer.ui.screens.suggestedbreaks

import java.time.DayOfWeek
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import com.longweekend.optimizer.ui.text.badge
import com.longweekend.optimizer.ui.text.dateRangeText
import com.longweekend.optimizer.ui.text.gainTag
import com.longweekend.optimizer.ui.text.includedHolidayLabels
import com.longweekend.optimizer.ui.text.localName
import com.longweekend.optimizer.ui.text.title
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.ui.components.BreakVisualizerStrip
import com.longweekend.optimizer.ui.components.LongWeekendTopBar
import com.longweekend.optimizer.ui.theme.*
import com.longweekend.optimizer.viewmodel.MainViewModel
import com.longweekend.optimizer.viewmodel.SortOrder
import com.longweekend.optimizer.viewmodel.UiStatus

@Composable
fun SuggestedBreaksScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Opportunity) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val leaveBudget by viewModel.leaveBudget.collectAsState()
    val annualAllowance by viewModel.annualAllowance.collectAsState()
    val opportunities by viewModel.opportunities.collectAsState()
    val uiStatus by viewModel.uiStatus.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val firstWorkday by viewModel.firstWorkday.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val isOfflineData by viewModel.isOfflineData.collectAsState()

    Scaffold(
        topBar = {
            LongWeekendTopBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.app_subtitle),
                countryBadge = selectedCountry.countryCode,
                showBackButton = true,
                onBackClick = onNavigateBack,
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Summary Bar (Tactile Pill Card)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Pills wrap onto a second line instead of being cut off
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SummaryPill(SurfaceContainer) {
                                Text(selectedCountry.flagEmoji, fontSize = 12.sp, lineHeight = 12.sp)
                                Text(
                                    selectedCountry.localName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            SummaryPill(SurfaceContainer) {
                                Text(
                                    "$selectedYear",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextOnSurfaceVariant
                                )
                            }
                            SummaryPill(AmberSecondaryFixed) {
                                Text(
                                    pluralStringResource(R.plurals.summary_budget, leaveBudget, leaveBudget),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberOnSecondaryFixed,
                                    maxLines = 1
                                )
                            }
                            SummaryPill(SurfaceContainer) {
                                Text(
                                    stringResource(if (firstWorkday == DayOfWeek.SUNDAY) R.string.summary_week_sunday else R.string.summary_week_monday),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextOnSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                    }

                    // Micro progress + edit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Insights, null, tint = AmberSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = pluralStringResource(R.plurals.summary_optimising, leaveBudget, leaveBudget),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onNavigateBack,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 32.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.action_edit),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }
                    }
                }
            }

            // Offline notice applies to results and to "no results" alike
            if (isOfflineData && uiStatus != UiStatus.LOADING && uiStatus != UiStatus.ERROR) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmberSecondaryFixed.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudOff, null, tint = AmberOnSecondaryFixed, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.offline_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberOnSecondaryFixed,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.action_retry), color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Render view depending on State
            when (uiStatus) {
                UiStatus.RESULTS -> {
                    // Header & Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.suggested_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TealPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TealPrimaryFixed)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = pluralStringResource(R.plurals.opportunities_count, opportunities.size, opportunities.size),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TealOnPrimaryFixed
                            )
                        }
                    }

                    // Sorting Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortChip(
                            label = stringResource(R.string.sort_longest),
                            icon = Icons.Default.HourglassTop,
                            isSelected = sortOrder == SortOrder.LONGEST_BREAK,
                            onClick = { viewModel.setSortOrder(SortOrder.LONGEST_BREAK) }
                        )
                        SortChip(
                            label = stringResource(R.string.sort_fewest),
                            icon = Icons.Default.Savings,
                            isSelected = sortOrder == SortOrder.FEWEST_LEAVE_DAYS,
                            onClick = { viewModel.setSortOrder(SortOrder.FEWEST_LEAVE_DAYS) }
                        )
                        SortChip(
                            label = stringResource(R.string.sort_upcoming),
                            icon = Icons.Default.Event,
                            isSelected = sortOrder == SortOrder.UPCOMING_FIRST,
                            onClick = { viewModel.setSortOrder(SortOrder.UPCOMING_FIRST) }
                        )
                    }

                    // Disclaimer Notice
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = stringResource(R.string.disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnSurfaceVariant
                            )
                        }
                    }

                    // Opportunities List
                    opportunities.forEach { opp ->
                        OpportunityFeedCard(
                            opportunity = opp,
                            onViewPlan = { onNavigateToDetail(opp) },
                            onToggleBookmark = { viewModel.toggleSavePlan(opp) }
                        )
                    }

                    // Upsell card, only while there is allowance left to invest
                    val suggestedBudget = (leaveBudget + 3).coerceAtMost(annualAllowance)
                    if (suggestedBudget > leaveBudget) Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AmberSecondaryFixed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FlightTakeoff, null, tint = AmberOnSecondaryFixed, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = stringResource(R.string.upsell_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                            Text(
                                text = stringResource(R.string.upsell_body, suggestedBudget, annualAllowance),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.setLeaveBudget(suggestedBudget) },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = CircleShape
                            ) {
                                Text(stringResource(R.string.upsell_action, suggestedBudget), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                UiStatus.LOADING -> {
                    // Loading Skeleton View
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = TealPrimary,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.loading_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.loading_body, selectedCountry.localName(), selectedYear),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextOnSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Skeleton Card 1
                        SkeletonCard()
                        SkeletonCard()
                    }
                }

                UiStatus.EMPTY -> {
                    // Empty State View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, tint = TextOnSurfaceVariant, modifier = Modifier.size(36.dp))
                        }
                        Text(
                            text = stringResource(R.string.empty_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        Text(
                            text = if (viewModel.seasonHasPassed(selectedSeason, selectedYear)) {
                                stringResource(R.string.empty_season_passed, selectedYear, selectedYear + 1)
                            } else {
                                pluralStringResource(R.plurals.empty_body, leaveBudget, leaveBudget, selectedCountry.localName())
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextOnSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.empty_tips_title), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TealPrimary)
                                Text(stringResource(R.string.empty_tip_more_days, annualAllowance), style = MaterialTheme.typography.bodySmall, color = TextOnSurfaceVariant)
                                Text(stringResource(R.string.empty_tip_flexible), style = MaterialTheme.typography.bodySmall, color = TextOnSurfaceVariant)
                            }
                        }

                        Button(
                            onClick = { viewModel.resetFilters() },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_reset_filters), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                UiStatus.ERROR -> {
                    // Error view: holidays always fall back offline, so this means the calculation failed
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(ErrorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = OnErrorContainer, modifier = Modifier.size(36.dp))
                        }
                        Text(
                            text = stringResource(R.string.error_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        Text(
                            text = stringResource(R.string.error_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextOnSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_try_again), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = if (isSelected) TealPrimary else SurfaceLowest,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) TealOnPrimary else TextOnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TealOnPrimary else TextOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun OpportunityFeedCard(
    opportunity: Opportunity,
    onViewPlan: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ROI Badge Header: the gain tag moves to its own line rather than wrapping inside
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AmberSecondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = AmberOnSecondaryContainer, modifier = Modifier.size(14.dp))
                    Text(
                        text = opportunity.badge(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberOnSecondaryContainer,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(TealPrimaryFixed)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = opportunity.gainTag(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TealOnPrimaryFixed,
                        maxLines = 1
                    )
                }
            }

            // Title & Dates
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = opportunity.dateRangeText(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
                Text(
                    text = opportunity.title(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant
                )
            }

            // Stats Micro-Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLow)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatColumn(number = "${opportunity.totalDays}", label = stringResource(R.string.stat_total_days), color = TealPrimary)
                StatColumn(number = "${opportunity.leaveDays}", label = stringResource(R.string.stat_annual_leave), color = AmberSecondary)
                StatColumn(number = "${opportunity.publicHolidaysCount}", label = stringResource(R.string.stat_public_holidays), color = TealPrimaryContainer)
            }

            // Included Holidays Tags
            if (opportunity.holidayDays.isNotEmpty()) {
                // Wraps so long holiday names are shown in full instead of scrolling off the card
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.includes),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextOnSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    opportunity.includedHolidayLabels().forEach { hName ->
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AmberSecondaryFixed)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AmberSecondary))
                            Text(
                                text = hName,
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberOnSecondaryFixed
                            )
                        }
                    }
                }
            }

            // Visual Strip
            BreakVisualizerStrip(days = opportunity.days)

            // Card Action Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onViewPlan,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = CircleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(stringResource(R.string.action_view_plan), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainer)
                ) {
                    Icon(
                        imageVector = if (opportunity.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(if (opportunity.isSaved) R.string.cd_remove_saved else R.string.cd_save_plan),
                        tint = if (opportunity.isSaved) AmberSecondary else TealPrimary
                    )
                }
            }
        }
    }
}

/** One fixed-height pill in the search summary, so pills with and without a flag line up. */
@Composable
private fun SummaryPill(background: Color, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .height(26.dp)
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
private fun StatColumn(number: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextOnSurfaceVariant
        )
    }
}

@Composable
private fun SkeletonCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(width = 140.dp, height = 20.dp).clip(CircleShape).background(SurfaceContainerHigh))
            Box(modifier = Modifier.size(width = 220.dp, height = 26.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceContainerHigh))
            Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow))
            Box(modifier = Modifier.fillMaxWidth().height(42.dp).clip(CircleShape).background(SurfaceContainerHigh))
        }
    }
}
