@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package com.longweekend.optimizer.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.longweekend.optimizer.R
import com.longweekend.optimizer.model.Country
import com.longweekend.optimizer.model.DayStripItem
import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.repository.SettingsRepository
import com.longweekend.optimizer.ui.components.BreakVisualizerStrip
import com.longweekend.optimizer.ui.components.CountryPicker
import com.longweekend.optimizer.ui.components.DaysStepper
import com.longweekend.optimizer.ui.components.SectionLabel
import com.longweekend.optimizer.ui.theme.*
import com.longweekend.optimizer.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private const val PAGE_COUNT = 4

/** Shown on first start: what the app does, how bridging works, privacy, then a quick setup. */
@Composable
fun OnboardingScreen(viewModel: MainViewModel) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val defaultCountry by viewModel.defaultCountry.collectAsState()
    val savedAllowance by viewModel.annualAllowance.collectAsState()
    var country by remember { mutableStateOf(defaultCountry) }
    var allowance by remember { mutableIntStateOf(savedAllowance) }
    val isLastPage = pagerState.currentPage == PAGE_COUNT - 1
    val finish = { viewModel.completeOnboarding(country, allowance) }

    Surface(color = SurfaceBg, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!isLastPage) {
                    TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(PAGE_COUNT - 1) } }) {
                        Text(stringResource(R.string.onboarding_skip), color = TextOnSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> InfoPage(
                        icon = Icons.Default.BeachAccess,
                        title = stringResource(R.string.onboarding_1_title),
                        body = stringResource(R.string.onboarding_1_body)
                    )
                    1 -> InfoPage(
                        icon = null,
                        title = stringResource(R.string.onboarding_2_title),
                        body = stringResource(R.string.onboarding_2_body)
                    ) { SampleBridge() }
                    2 -> InfoPage(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.onboarding_3_title),
                        body = pluralStringResource(R.plurals.onboarding_3_body, viewModel.supportedCountries.size, viewModel.supportedCountries.size)
                    )
                    else -> SetupPage(
                        country = country,
                        countries = viewModel.supportedCountries,
                        onCountryChange = { country = it },
                        allowance = allowance,
                        onAllowanceChange = { allowance = it.coerceIn(SettingsRepository.ALLOWANCE_RANGE) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(PAGE_COUNT) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (i == pagerState.currentPage) 22.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(if (i == pagerState.currentPage) TealPrimary else OutlineVariant)
                    )
                }
            }

            Button(
                onClick = {
                    if (isLastPage) finish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimaryContainer),
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(if (isLastPage) R.string.onboarding_start else R.string.onboarding_next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InfoPage(
    icon: ImageVector?,
    title: String,
    body: String,
    illustration: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(TealPrimaryFixed),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(60.dp))
            }
        }
        illustration?.invoke()
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = TealPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = TextOnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** A Thursday holiday + one leave day = four days off, drawn with the app's own strip. */
@Composable
private fun SampleBridge() {
    val thursday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
    val days = listOf(
        DayStripItem(thursday, DayType.PUBLIC_HOLIDAY),
        DayStripItem(thursday.plusDays(1), DayType.ANNUAL_LEAVE),
        DayStripItem(thursday.plusDays(2), DayType.WEEKEND),
        DayStripItem(thursday.plusDays(3), DayType.WEEKEND)
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        BreakVisualizerStrip(days = days, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun SetupPage(
    country: Country,
    countries: List<Country>,
    onCountryChange: (Country) -> Unit,
    allowance: Int,
    onAllowanceChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(AmberSecondaryFixed)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = AmberSecondary, modifier = Modifier.size(48.dp))
        }
        Text(
            text = stringResource(R.string.onboarding_4_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = TealPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.onboarding_4_body),
            style = MaterialTheme.typography.bodyLarge,
            color = TextOnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        SectionLabel(icon = Icons.Default.BeachAccess, title = stringResource(R.string.label_your_country))
        CountryPicker(
            selected = country,
            countries = countries,
            caption = stringResource(R.string.data_source_caption),
            onCountrySelected = onCountryChange
        )
        SectionLabel(
            icon = Icons.Default.Tune,
            title = stringResource(R.string.label_allowance),
            subtitle = stringResource(R.string.label_allowance_hint)
        )
        DaysStepper(
            value = allowance,
            range = SettingsRepository.ALLOWANCE_RANGE,
            unitLabel = pluralStringResource(R.plurals.days_per_year_unit, allowance),
            onValueChange = onAllowanceChange
        )
    }
}
