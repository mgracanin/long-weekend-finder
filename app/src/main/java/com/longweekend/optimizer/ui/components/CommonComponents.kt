package com.longweekend.optimizer.ui.components

import com.longweekend.optimizer.ui.text.shortWeekday
import com.longweekend.optimizer.ui.text.badgeText
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longweekend.optimizer.model.DayStripItem
import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.ui.theme.*
import java.time.LocalDate

@Composable
fun LongWeekendTopBar(
    title: String,
    subtitle: String,
    countryBadge: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onProfileClick: (() -> Unit)? = null
) {
    Surface(
        color = SurfaceLowest.copy(alpha = 0.95f),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = TextOnSurface
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TealPrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BeachAccess,
                            contentDescription = stringResource(R.string.cd_logo),
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AmberSecondaryFixed)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = countryBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberOnSecondaryFixed
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextOnSurfaceVariant
                    )
                }
            }

            if (onProfileClick != null) {
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TealPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.cd_profile),
                        tint = TealOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionLabel(
    icon: ImageVector,
    title: String,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextOnSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DaysStepper(
    value: Int,
    range: IntRange,
    unitLabel: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(
            icon = Icons.Default.Remove,
            contentDescription = stringResource(R.string.cd_decrease),
            enabled = value > range.first,
            onClick = { onValueChange(value - 1) }
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                fontWeight = FontWeight.ExtraBold,
                color = TealPrimary
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextOnSurfaceVariant
            )
        }

        StepperButton(
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.cd_increase),
            enabled = value < range.last,
            onClick = { onValueChange(value + 1) }
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(SurfaceLowest)
    ) {
        Icon(icon, contentDescription, tint = if (enabled) TealPrimary else OutlineVariant)
    }
}

@Composable
fun BreakVisualizerStrip(
    days: List<DayStripItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.visualiser_label),
                style = MaterialTheme.typography.labelSmall,
                color = TextOnSurfaceVariant,
                letterSpacing = 1.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = SurfaceContainerHighest, label = stringResource(R.string.legend_weekend))
                LegendDot(color = AmberSecondaryContainer, label = stringResource(R.string.legend_holiday))
                LegendDot(color = TealPrimary, label = stringResource(R.string.legend_leave))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            days.forEach { day ->
                val (bg, textColor, badgeBg, badgeTextColor) = when (day.type) {
                    DayType.WEEKEND -> Quad(
                        SurfaceContainer,
                        TextOnSurface,
                        Color.Transparent,
                        TextOnSurfaceVariant
                    )
                    DayType.PUBLIC_HOLIDAY -> Quad(
                        AmberSecondaryContainer,
                        AmberOnSecondaryContainer,
                        AmberOnSecondaryContainer.copy(alpha = 0.15f),
                        AmberOnSecondaryContainer
                    )
                    DayType.ANNUAL_LEAVE -> Quad(
                        TealPrimaryFixed,
                        TealPrimary,
                        TealPrimary.copy(alpha = 0.15f),
                        TealPrimary
                    )
                    DayType.WORKDAY -> Quad(
                        SurfaceContainerLow,
                        TextOnSurfaceVariant,
                        Color.Transparent,
                        TextOnSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .width(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = shortWeekday(day.date),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = textColor.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${day.date.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(badgeBg)
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = day.type.badgeText(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = badgeTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = TextOnSurfaceVariant
        )
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun MetricBentoStrip(
    totalDays: Int,
    leaveCost: Int,
    gainPercent: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Card 1: Total
        Card(
            modifier = Modifier
                .weight(1f)
                .height(95.dp),
            colors = CardDefaults.cardColors(containerColor = TealPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BeachAccess,
                        contentDescription = null,
                        tint = TealPrimaryFixed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.metric_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = TealPrimaryFixed
                    )
                }
                Column {
                    Text(
                        text = "$totalDays",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = TealOnPrimary
                    )
                    Text(
                        text = stringResource(R.string.metric_days_away),
                        style = MaterialTheme.typography.labelSmall,
                        color = TealOnPrimaryContainer
                    )
                }
            }
        }

        // Card 2: Cost
        Card(
            modifier = Modifier
                .weight(1f)
                .height(95.dp),
            colors = CardDefaults.cardColors(containerColor = AmberSecondaryContainer),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = null,
                        tint = AmberOnSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.metric_cost),
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberOnSecondaryContainer
                    )
                }
                Column {
                    Text(
                        text = "$leaveCost",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberOnSecondaryContainer
                    )
                    Text(
                        text = stringResource(R.string.metric_leave_days),
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberOnSecondaryContainer
                    )
                }
            }
        }

        // Card 3: ROI
        Card(
            modifier = Modifier
                .weight(1f)
                .height(95.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = AmberSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.metric_roi),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextOnSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = "+${gainPercent}%",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberSecondary
                    )
                    Text(
                        text = stringResource(R.string.metric_extra_days),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextOnSurfaceVariant
                    )
                }
            }
        }
    }
}
