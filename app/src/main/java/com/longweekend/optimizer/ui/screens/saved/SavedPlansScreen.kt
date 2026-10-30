package com.longweekend.optimizer.ui.screens.saved

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.longweekend.optimizer.ui.report.YearReport
import com.longweekend.optimizer.ui.report.openYearReport
import com.longweekend.optimizer.ui.report.saveYearReport
import java.io.File
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import com.longweekend.optimizer.ui.report.yearReport
import com.longweekend.optimizer.ui.report.shareYearReport
import com.longweekend.optimizer.ui.report.writeYearReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import com.longweekend.optimizer.ui.share.planShare
import com.longweekend.optimizer.ui.share.sharePlan
import com.longweekend.optimizer.model.Country
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import com.longweekend.optimizer.ui.text.appLocale
import com.longweekend.optimizer.ui.text.badge
import com.longweekend.optimizer.ui.text.dateRangeText
import com.longweekend.optimizer.ui.text.formatDate
import com.longweekend.optimizer.ui.text.localName
import com.longweekend.optimizer.ui.text.title
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.longweekend.optimizer.ui.components.BreakVisualizerStrip
import com.longweekend.optimizer.ui.components.LongWeekendTopBar
import com.longweekend.optimizer.ui.theme.*
import com.longweekend.optimizer.viewmodel.MainViewModel

@Composable
fun SavedPlansScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (Opportunity) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDiscover: () -> Unit
) {
    val savedPlans by viewModel.savedPlans.collectAsState()
    val context = LocalContext.current
    val allowance by viewModel.annualAllowance.collectAsState()
    val firstWorkday by viewModel.firstWorkday.collectAsState()
    val scope = rememberCoroutineScope()

    // One report per year that has saved plans; worded now, holidays loaded when it is made
    val plansByYear = savedPlans.groupBy { it.opportunity.startDate.year }.toSortedMap()
    val reports = plansByYear.mapValues { (year, plans) ->
        val code = plans.groupingBy { it.opportunity.countryCode.orEmpty() }.eachCount().maxByOrNull { it.value }?.key.orEmpty()
        val countryName = Country.forCode(code)?.localName() ?: plans.first().opportunity.countryName.orEmpty()
        code to yearReport(year, plans, countryName, allowance, firstWorkday)
    }
    val chooserTitle = stringResource(R.string.report_chooser_title)
    val failedMessage = stringResource(R.string.report_failed)
    var pickYear by remember { mutableStateOf(false) }
    // The written report waits here while the user chooses to open, save or share it
    var ready by remember { mutableStateOf<Pair<File, YearReport>?>(null) }
    val savedMessage = stringResource(R.string.report_saved)
    val noViewerMessage = stringResource(R.string.report_no_viewer)
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        val file = ready?.first
        if (target != null && file != null) {
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { context.saveYearReport(file, target) } }
                    .onSuccess { Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show() }
            }
        }
        ready = null
    }
    val makeReport: (Int) -> Unit = { year ->
        val (code, report) = reports.getValue(year)
        scope.launch {
            runCatching {
                val holidays = runCatching { viewModel.publicHolidayDates(code, year) }.getOrDefault(emptySet())
                val full = report.copy(publicHolidays = report.publicHolidays + holidays)
                val file = withContext(Dispatchers.IO) { context.writeYearReport(full) }
                ready = file to full
            }.onFailure { Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show() }
        }
    }

    Scaffold(
        topBar = {
            LongWeekendTopBar(
                title = stringResource(R.string.saved_title),
                subtitle = null,
                countryBadge = "${savedPlans.size}",
                onProfileClick = onNavigateToProfile
            )
        },
        containerColor = SurfaceBg
    ) { padding ->
        ready?.let { (file, report) ->
            AlertDialog(
                onDismissRequest = { ready = null },
                icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = TealPrimary) },
                title = { Text(stringResource(R.string.report_ready_title)) },
                text = { Text(file.name, style = MaterialTheme.typography.bodyMedium, color = TextOnSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = {
                        ready = null
                        if (!context.openYearReport(file)) {
                            Toast.makeText(context, noViewerMessage, Toast.LENGTH_LONG).show()
                            context.shareYearReport(file, report, chooserTitle)
                        }
                    }) { Text(stringResource(R.string.report_open), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { saveLauncher.launch(file.name) }) { Text(stringResource(R.string.report_save)) }
                        TextButton(onClick = {
                            ready = null
                            context.shareYearReport(file, report, chooserTitle)
                        }) { Text(stringResource(R.string.action_share)) }
                    }
                }
            )
        }
        if (pickYear) {
            AlertDialog(
                onDismissRequest = { pickYear = false },
                title = { Text(stringResource(R.string.report_pick_year)) },
                text = {
                    Column {
                        reports.keys.forEach { year ->
                            TextButton(onClick = { pickYear = false; makeReport(year) }) {
                                Text("$year", style = MaterialTheme.typography.titleMedium, color = TealPrimary)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { pickYear = false }) { Text(stringResource(android.R.string.cancel)) } }
            )
        }
        // Lazy so a long list of saved plans composes only what is on screen
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // The plan count is already in the top bar
            if (reports.isNotEmpty()) item(key = "report") {
                OutlinedButton(
                    onClick = { if (reports.size > 1) pickYear = true else makeReport(reports.keys.first()) },
                    border = BorderStroke(1.dp, CardBorder),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.report_action), color = TealPrimary, style = MaterialTheme.typography.labelLarge)
                }
            }
            item(key = "heading") {
                Text(
                    text = stringResource(R.string.saved_heading),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TealPrimary
                )
            }

            if (savedPlans.isEmpty()) item(key = "empty") {
                // Empty saved state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
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
                        Icon(Icons.Default.BookmarkBorder, null, tint = TextOnSurfaceVariant, modifier = Modifier.size(36.dp))
                    }
                    Text(
                        text = stringResource(R.string.saved_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                    Text(
                        text = stringResource(R.string.saved_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onNavigateToDiscover,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Explore, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_explore), style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                items(savedPlans, key = { it.opportunity.id }, contentType = { "plan" }) { plan ->
                    val opp = plan.opportunity
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(AmberSecondaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = opp.badge(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberOnSecondaryContainer
                                    )
                                }

                                val share = planShare(
                                    opp,
                                    Country.forCode(opp.countryCode.orEmpty())?.localName() ?: opp.countryName.orEmpty()
                                )
                                val shareTitle = stringResource(R.string.share_chooser_title)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IconButton(
                                        onClick = { context.sharePlan(share, shareTitle) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Share, stringResource(R.string.cd_share_plan), tint = TealPrimary)
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleSavePlan(opp) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, stringResource(R.string.cd_remove), tint = TextOnSurfaceVariant)
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = opp.title(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextOnSurface
                                )
                                val country = viewModel.supportedCountries.firstOrNull { it.countryCode == opp.countryCode }
                                Text(
                                    text = listOfNotNull(
                                        country?.let { "${it.flagEmoji} ${it.localName()}" },
                                        opp.dateRangeText()
                                    ).joinToString("  •  "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextOnSurfaceVariant
                                )
                            }

                            BreakVisualizerStrip(days = opp.days)

                            // Checkable submission checklist items
                            if (plan.checklistItems.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceContainerLow)
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.checklist_progress, plan.checklistItems.count { it.isChecked }, plan.checklistItems.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )

                                    plan.checklistItems.forEachIndexed { idx, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.updateChecklistItem(opp.id, idx, !item.isChecked)
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = item.isChecked,
                                                onCheckedChange = { checked ->
                                                    viewModel.updateChecklistItem(opp.id, idx, checked)
                                                },
                                                modifier = Modifier.size(24.dp),
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = TealPrimary,
                                                    checkmarkColor = TealOnPrimary
                                                )
                                            )
                                            Text(
                                                text = formatDate(item.date, R.string.pattern_full_date).replaceFirstChar { it.titlecase(appLocale()) },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (item.isChecked) TextOnSurface else TextOnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { onNavigateToDetail(opp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = CircleShape
                            ) {
                                Text(stringResource(R.string.action_view_full_plan), style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
