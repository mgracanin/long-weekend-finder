@file:OptIn(ExperimentalComposeUiApi::class)

package com.longweekend.optimizer.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.longweekend.optimizer.repository.ThemeMode
import com.longweekend.optimizer.ui.theme.applyNightMode
import java.time.DayOfWeek
import com.longweekend.optimizer.model.WorkWeek
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.longweekend.optimizer.AppLinks
import com.longweekend.optimizer.BuildConfig
import com.longweekend.optimizer.R
import com.longweekend.optimizer.model.Planning
import com.longweekend.optimizer.repository.HolidayDataStatus
import com.longweekend.optimizer.repository.SettingsRepository
import com.longweekend.optimizer.ui.components.CountryPicker
import com.longweekend.optimizer.ui.components.DaysStepper
import com.longweekend.optimizer.ui.components.LongWeekendTopBar
import com.longweekend.optimizer.ui.components.SectionLabel
import com.longweekend.optimizer.ui.text.formatDate
import com.longweekend.optimizer.ui.text.localName
import com.longweekend.optimizer.ui.theme.*
import com.longweekend.optimizer.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Language tags offered in-app; "" follows the phone's language. */
private val LANGUAGES = listOf("" to R.string.language_system, "en" to R.string.language_english, "hr" to R.string.language_croatian)

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val defaultCountry by viewModel.defaultCountry.collectAsStateWithLifecycle()
    val annualAllowance by viewModel.annualAllowance.collectAsStateWithLifecycle()
    val defaultLeaveBudget by viewModel.defaultLeaveBudget.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val savedPlans by viewModel.savedPlans.collectAsStateWithLifecycle()
    val firstWorkday by viewModel.firstWorkday.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val holidayDataStatus by viewModel.holidayDataStatus.collectAsStateWithLifecycle()

    val plannedDays = viewModel.plannedLeaveDays(savedPlans, selectedYear)
    val remainingDays = annualAllowance - plannedDays

    Scaffold(
        topBar = {
            LongWeekendTopBar(
                title = stringResource(R.string.profile_title),
                subtitle = stringResource(R.string.profile_subtitle),
                countryBadge = pluralStringResource(R.plurals.days_count, annualAllowance, annualAllowance),
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_heading),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TealPrimary
                )
                Text(
                    text = stringResource(R.string.profile_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant
                )
            }

            SettingsCard {
                SectionLabel(
                    icon = Icons.Default.Public,
                    title = stringResource(R.string.label_default_country),
                    subtitle = stringResource(R.string.label_default_country_hint)
                )
                CountryPicker(
                    selected = defaultCountry,
                    countries = viewModel.supportedCountries,
                    caption = stringResource(R.string.caption_default_country),
                    onCountrySelected = viewModel::setDefaultCountry
                )
            }

            HolidayDataCard(
                countryName = defaultCountry.localName(),
                years = Planning.years(viewModel.currentYear),
                status = holidayDataStatus,
                onRefresh = viewModel::refreshHolidayData
            )

            SettingsCard {
                SectionLabel(
                    icon = Icons.Default.WorkHistory,
                    title = stringResource(R.string.label_allowance),
                    subtitle = stringResource(R.string.label_allowance_hint)
                )
                DaysStepper(
                    value = annualAllowance,
                    range = SettingsRepository.ALLOWANCE_RANGE,
                    unitLabel = pluralStringResource(R.plurals.days_per_year_unit, annualAllowance),
                    onValueChange = viewModel::setAnnualAllowance
                )
            }

            SettingsCard {
                SectionLabel(
                    icon = Icons.Default.FlightTakeoff,
                    title = stringResource(R.string.label_default_invest),
                    subtitle = stringResource(R.string.label_default_invest_hint)
                )
                DaysStepper(
                    value = defaultLeaveBudget,
                    range = 1..annualAllowance,
                    unitLabel = pluralStringResource(R.plurals.working_days_unit, defaultLeaveBudget),
                    onValueChange = viewModel::setDefaultLeaveBudget
                )
            }

            // Allowance usage for the selected year, based on saved plans
            SettingsCard {
                SectionLabel(
                    icon = Icons.Default.PieChart,
                    title = stringResource(R.string.label_planned, selectedYear),
                    subtitle = stringResource(R.string.label_planned_hint, selectedYear)
                )
                LinearProgressIndicator(
                    progress = { (plannedDays.toFloat() / annualAllowance).coerceIn(0f, 1f) },
                    color = if (remainingDays < 0) ErrorRed else TealPrimary,
                    trackColor = SurfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.planned_progress, plannedDays, annualAllowance),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextOnSurface
                    )
                    Text(
                        text = if (remainingDays >= 0) stringResource(R.string.planned_left, remainingDays)
                        else stringResource(R.string.planned_over, -remainingDays),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingDays >= 0) AmberSecondary else ErrorRed
                    )
                }
                if (remainingDays < 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ErrorContainer)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = OnErrorContainer, modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.planned_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnErrorContainer
                        )
                    }
                }
            }

            WorkWeekCard(selected = firstWorkday, onSelect = viewModel::setFirstWorkday)

            LanguageCard()

            AppearanceCard(
                selected = themeMode,
                onSelect = { mode ->
                    viewModel.setThemeMode(mode)
                    mode.applyNightMode()
                }
            )

            TextButton(
                onClick = viewModel::resetProfile,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.RestartAlt, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.action_reset_defaults), style = MaterialTheme.typography.labelLarge, color = TealPrimary)
            }

            AboutCard(
                countryCount = viewModel.supportedCountries.size,
                onShowWelcome = viewModel::showOnboardingAgain
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Where the home country's holidays stand on the phone, with a way to update them now. */
@Composable
private fun HolidayDataCard(
    countryName: String,
    years: IntRange,
    status: HolidayDataStatus?,
    onRefresh: suspend () -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val doneMessage = stringResource(R.string.holiday_data_refreshed)
    val failedMessage = stringResource(R.string.holiday_data_failed)

    SettingsCard {
        SectionLabel(
            icon = Icons.Default.CloudDownload,
            title = stringResource(R.string.label_holiday_data),
            subtitle = stringResource(R.string.label_holiday_data_hint, countryName, years.first, years.last)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when {
                    status == null -> ""
                    status.storedYears == 0 -> stringResource(R.string.holiday_data_none)
                    status.storedYears < status.totalYears -> stringResource(R.string.holiday_data_partial)
                    else -> stringResource(
                        R.string.holiday_data_updated,
                        formatDate(
                            Instant.ofEpochMilli(status.oldestUpdate ?: 0).atZone(ZoneId.systemDefault()).toLocalDate(),
                            R.string.pattern_day_month_year
                        )
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = TextOnSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = {
                    refreshing = true
                    scope.launch {
                        val ok = onRefresh()
                        refreshing = false
                        Toast.makeText(context, if (ok) doneMessage else failedMessage, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !refreshing,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                // The icon slot keeps its size, so the button doesn't change width while working
                Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    if (refreshing) CircularProgressIndicator(strokeWidth = 2.dp, color = TealPrimary, modifier = Modifier.size(14.dp))
                    else Icon(Icons.Default.Refresh, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_update_now), color = TealPrimary)
            }
        }
    }
}

@Composable
private fun WorkWeekCard(selected: DayOfWeek, onSelect: (DayOfWeek) -> Unit) {
    SettingsCard {
        SectionLabel(
            icon = Icons.Default.DateRange,
            title = stringResource(R.string.label_first_workday),
            subtitle = stringResource(R.string.label_first_workday_hint)
        )
        WorkWeek.OPTIONS.forEach { day ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(day) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = day == selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(if (day == DayOfWeek.SUNDAY) R.string.workweek_sunday else R.string.workweek_monday),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextOnSurface
                )
            }
        }
    }
}

@Composable
private fun AppearanceCard(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SettingsCard {
        SectionLabel(
            icon = Icons.Default.Contrast,
            title = stringResource(R.string.label_appearance),
            subtitle = stringResource(R.string.label_appearance_hint)
        )
        ThemeMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { if (mode != selected) onSelect(mode) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = mode == selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                            ThemeMode.OLED -> R.string.theme_oled
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextOnSurface
                )
            }
        }
    }
}

@Composable
private fun LanguageCard() {
    // AppCompat stores the choice and recreates the activity in the new language
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
    SettingsCard {
        SectionLabel(
            icon = Icons.Default.Translate,
            title = stringResource(R.string.label_language),
            subtitle = stringResource(R.string.label_language_hint)
        )
        LANGUAGES.forEach { (tag, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        if (tag != current) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = tag == current,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = TextOnSurface)
            }
        }
    }
}

@Composable
private fun AboutCard(countryCount: Int, onShowWelcome: () -> Unit) {
    val context = LocalContext.current
    val noBrowser = stringResource(R.string.about_no_browser)
    val open: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, noBrowser, Toast.LENGTH_SHORT).show()
        }
    }

    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BeachAccess, null, tint = TealOnPrimary)
            }
            Column {
                Text(
                    text = stringResource(R.string.about_heading) + " · " + stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextOnSurface
                )
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextOnSurfaceVariant
                )
            }
        }
        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodySmall,
            color = TextOnSurfaceVariant
        )
        AboutRow(Icons.Default.Code, stringResource(R.string.about_open_source), stringResource(R.string.about_open_source_hint)) {
            open(AppLinks.REPOSITORY)
        }
        AboutRow(Icons.Default.Shield, stringResource(R.string.about_privacy), stringResource(R.string.about_privacy_hint)) {
            open(AppLinks.PRIVACY_POLICY)
        }
        AboutRow(Icons.Default.BugReport, stringResource(R.string.about_feedback), stringResource(R.string.about_feedback_hint)) {
            open(AppLinks.NEW_ISSUE)
        }
        AboutRow(Icons.Default.Public, stringResource(R.string.about_data), pluralStringResource(R.plurals.about_data_hint, countryCount, countryCount)) {
            open(AppLinks.HOLIDAY_DATA)
        }
        AboutRow(Icons.Default.Description, stringResource(R.string.about_licence), stringResource(R.string.about_licence_hint)) {
            open(AppLinks.LICENSE)
        }
        AboutRow(Icons.Default.School, stringResource(R.string.about_welcome_again), null, onClick = onShowWelcome)
        Text(
            text = stringResource(R.string.about_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = TextOnSurfaceVariant
        )
    }
}

@Composable
private fun AboutRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextOnSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextOnSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = OutlineVariant)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}
