@file:OptIn(ExperimentalComposeUiApi::class)

package com.longweekend.optimizer.ui.screens.plandetails

import com.longweekend.optimizer.ui.share.planShare
import com.longweekend.optimizer.ui.share.sharePlan
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import com.longweekend.optimizer.ui.text.appLocale
import com.longweekend.optimizer.ui.text.dateRangeText
import com.longweekend.optimizer.ui.text.formatDate
import com.longweekend.optimizer.ui.text.includedHolidayLabels
import com.longweekend.optimizer.ui.text.localName
import com.longweekend.optimizer.ui.text.title
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.ui.components.LongWeekendTopBar
import com.longweekend.optimizer.ui.components.MetricBentoStrip
import com.longweekend.optimizer.ui.components.MonthCalendarView
import com.longweekend.optimizer.ui.theme.*
import com.longweekend.optimizer.viewmodel.MainViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlanDetailsScreen(
    opportunity: Opportunity,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentCountry by viewModel.selectedCountry.collectAsState()
    // A saved plan keeps its own country, even after the search switches to another one
    val planCountry = viewModel.supportedCountries.firstOrNull { it.countryCode == opportunity.countryCode } ?: currentCountry
    val savedPlans by viewModel.savedPlans.collectAsState()
    val firstWorkday by viewModel.firstWorkday.collectAsState()

    val savedPlan = savedPlans.firstOrNull { it.opportunity.id == opportunity.id }
    val isSaved = savedPlan != null
    var showSavedBanner by remember { mutableStateOf(false) }

    val leaveDates = remember(opportunity.id) {
        opportunity.days.filter { it.type == DayType.ANNUAL_LEAVE }
    }

    // Unsaved plans keep ticks locally; saved plans read and write the persisted checklist
    val localChecks = remember(opportunity.id) { leaveDates.map { false }.toMutableStateList() }
    val checkedItems: List<Boolean> = savedPlan?.checklistItems?.map { it.isChecked } ?: localChecks
    val setChecked: (Int, Boolean) -> Unit = { idx, checked ->
        if (savedPlan != null) {
            viewModel.updateChecklistItem(opportunity.id, idx, checked)
        } else if (idx in localChecks.indices) {
            localChecks[idx] = checked
        }
    }
    val toggleSave: () -> Unit = {
        viewModel.toggleSavePlan(opportunity, localChecks.toList())
        showSavedBanner = !isSaved
    }

    val title = opportunity.title()
    val rangeText = opportunity.dateRangeText()
    val countryName = planCountry.localName()
    val leaveDatesText = leaveDates.map { formatDate(it.date, R.string.pattern_day_month_year) }.joinToString(", ")
    val clipboardLabel = stringResource(R.string.clipboard_label)
    val clipboardText = stringResource(R.string.clipboard_text, title, leaveDatesText, rangeText)
    val copiedMessage = stringResource(R.string.toast_copied)
    val noCalendarMessage = stringResource(R.string.toast_no_calendar)
    val eventTitle = stringResource(R.string.calendar_event_title, title, countryName)
    val eventDetails = pluralStringResource(R.plurals.calendar_event_details, opportunity.leaveDays, opportunity.leaveDays)
    val share = planShare(opportunity, countryName)
    val shareTitle = stringResource(R.string.share_chooser_title)

    val months = remember(opportunity.id) {
        generateSequence(YearMonth.from(opportunity.startDate)) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(YearMonth.from(opportunity.endDate)) }
            .toList()
    }

    Scaffold(
        topBar = {
            LongWeekendTopBar(
                title = stringResource(R.string.detail_title),
                subtitle = countryName,
                countryBadge = planCountry.countryCode,
                showBackButton = true,
                onBackClick = onNavigateBack
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
            // Header context (back and country are in the top bar)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextOnSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = rangeText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextOnSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { context.sharePlan(share, shareTitle) },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainer)
                        ) {
                            Icon(Icons.Default.Share, stringResource(R.string.cd_share_plan), tint = TealPrimary)
                        }
                        IconButton(
                            onClick = toggleSave,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainer)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = stringResource(if (isSaved) R.string.cd_remove_saved else R.string.cd_save_plan),
                                tint = if (isSaved) AmberSecondary else TealPrimary
                            )
                        }
                    }
                }
            }

            // High-Impact Bento Metric Strip (Total Days, Leave Cost, ROI Boost)
            MetricBentoStrip(
                totalDays = opportunity.totalDays,
                leaveCost = opportunity.leaveDays,
                gainPercent = opportunity.gainPercent
            )

            // One calendar per month the break touches (e.g. 28 Apr – 4 May shows both)
            months.forEachIndexed { index, month ->
                MonthCalendarView(
                    yearMonth = month,
                    days = opportunity.days,
                    firstWorkday = firstWorkday,
                    // One legend under the last month is enough
                    showLegend = index == months.lastIndex
                )
            }

            // Explicit Actionable Leave Checklist
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FactCheck, null, tint = AmberSecondary, modifier = Modifier.size(22.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.request_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextOnSurface
                            )
                            Text(
                                text = pluralStringResource(R.plurals.request_body, opportunity.leaveDays, opportunity.leaveDays),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnSurfaceVariant
                            )
                        }
                    }

                    // Checkable Task List
                    leaveDates.forEachIndexed { idx, day ->
                        val isChecked = checkedItems.getOrElse(idx) { false }
                        val dayFormatted = formatDate(day.date, R.string.pattern_full_date).replaceFirstChar { it.titlecase(appLocale()) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { setChecked(idx, !isChecked) },
                            color = SurfaceContainerLow
                        ) {
                            // Status sits under the date (not in a side pill), so every row has the
                            // same layout however long the date is
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked -> setChecked(idx, checked) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = TealPrimary,
                                        checkmarkColor = TealOnPrimary
                                    )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dayFormatted,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextOnSurface
                                    )
                                    Text(
                                        text = stringResource(if (isChecked) R.string.status_requested else R.string.status_to_request),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChecked) TealPrimary else TextOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Free Public Holiday Inclusions Banner
                    if (opportunity.holidayDays.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AmberSecondaryFixed.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.Verified, null, tint = AmberSecondary, modifier = Modifier.size(18.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(R.string.holidays_included),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberOnSecondaryFixed
                                    )
                                    opportunity.includedHolidayLabels().forEach { h ->
                                        Text(
                                            text = stringResource(R.string.bullet_item, h),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextOnSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save Plan & Actions Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Saved Alert Banner
                if (showSavedBanner && isSaved) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealPrimaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.TaskAlt, null, tint = TealPrimaryFixed, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.saved_banner_title),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimaryFixed
                                    )
                                    Text(
                                        text = stringResource(R.string.saved_banner_body),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TealOnPrimaryContainer
                                    )
                                }
                            }
                            IconButton(onClick = { showSavedBanner = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = TealOnPrimaryContainer)
                            }
                        }
                    }
                }

                // Primary Save CTA
                Button(
                    onClick = toggleSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) TealPrimaryContainer else TealPrimary
                    ),
                    shape = CircleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(if (isSaved) R.string.action_saved else R.string.action_save),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Secondary Tool Buttons (Copy dates & Google Calendar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(clipboardLabel, clipboardText)
                            clipboard.setPrimaryClip(clip)

                            Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = TealPrimary)
                            Text(stringResource(R.string.action_copy_dates), style = MaterialTheme.typography.labelMedium, color = TealPrimary)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val startStr = opportunity.startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                            val endStr = opportunity.endDate.plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                            val gCalUrl = "https://calendar.google.com/calendar/render?action=TEMPLATE&text=" +
                                    Uri.encode(eventTitle) +
                                    "&dates=${startStr}/${endStr}" +
                                    "&details=" + Uri.encode(eventDetails)

                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(gCalUrl)))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, noCalendarMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp), tint = TealPrimary)
                            Text(stringResource(R.string.action_calendar), style = MaterialTheme.typography.labelMedium, color = TealPrimary)
                        }
                    }
                }
            }

            // Data Source Attribution & Verification
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(R.string.data_verification),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextOnSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${planCountry.countryCode} / $countryName",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextOnSurface
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.data_source_full),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextOnSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.data_notice),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
