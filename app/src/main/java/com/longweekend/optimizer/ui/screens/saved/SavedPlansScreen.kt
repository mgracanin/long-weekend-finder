package com.longweekend.optimizer.ui.screens.saved

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
        // Lazy so a long list of saved plans composes only what is on screen
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // The plan count is already in the top bar
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
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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

                                IconButton(
                                    onClick = { viewModel.toggleSavePlan(opp) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, stringResource(R.string.cd_remove), tint = TextOnSurfaceVariant)
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
