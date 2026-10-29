package com.longweekend.optimizer.ui.components

import com.longweekend.optimizer.model.WorkWeek
import com.longweekend.optimizer.ui.text.weekdayInitials
import com.longweekend.optimizer.ui.text.formatDate
import com.longweekend.optimizer.ui.text.appLocale
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longweekend.optimizer.model.DayStripItem
import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun MonthCalendarView(
    yearMonth: YearMonth,
    days: List<DayStripItem>,
    firstWorkday: DayOfWeek,
    modifier: Modifier = Modifier
) {
    val monthTitle = formatDate(yearMonth.atDay(1), R.string.pattern_month_year).replaceFirstChar { it.titlecase(appLocale()) }
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1).dayOfWeek
    // Monday is index 0 in British/European standard
    // Columns start on the first working day; the weekend is the other two days
    val startOffset = (firstDayOfMonth.value - firstWorkday.value + 7) % 7
    val weekend = WorkWeek.weekend(firstWorkday)
    val columnDays = WorkWeek.daysFrom(firstWorkday)

    val dayTypeMap = days.associateBy({ it.date }, { it.type })

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextOnSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AmberSecondaryFixed)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.calendar_strategy_window),
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberOnSecondaryFixed
                    )
                }
            }

            // Weekday Headers (M T W T F S S)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekdayInitials(firstWorkday).forEachIndexed { i, label ->
                    val color = if (columnDays[i] in weekend) AmberSecondary else TextOnSurfaceVariant
                    Text(
                        text = label,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }

            // Calendar Grid
            val totalCells = startOffset + daysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - startOffset + 1

                            if (dayNum in 1..daysInMonth) {
                                val date = yearMonth.atDay(dayNum)
                                val type = dayTypeMap[date]

                                when (type) {
                                    DayType.PUBLIC_HOLIDAY -> {
                                        Column(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AmberSecondaryContainer),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp
                                                ),
                                                color = AmberOnSecondaryContainer
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = AmberOnSecondaryContainer,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                    DayType.ANNUAL_LEAVE -> {
                                        Column(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(TealPrimary),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                ),
                                                color = TealOnPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = TealOnPrimary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                    DayType.WEEKEND -> {
                                        Column(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SurfaceContainerHighest),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                ),
                                                color = TextOnSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(OutlineColor)
                                            )
                                        }
                                    }
                                    else -> {
                                        val isWk = date.dayOfWeek in weekend
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isWk) SurfaceContainer.copy(alpha = 0.5f) else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                                color = if (isWk) TextOnSurfaceVariant else TextOnSurface
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.size(38.dp))
                            }
                        }
                    }
                }
            }

            // Legend
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLow)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.calendar_legend),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextOnSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AmberSecondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, null, tint = AmberOnSecondaryContainer, modifier = Modifier.size(9.dp))
                            }
                        },
                        text = stringResource(R.string.legend_public_holiday)
                    )
                    LegendItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = TealOnPrimary, modifier = Modifier.size(9.dp))
                            }
                        },
                        text = stringResource(R.string.legend_annual_leave)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineColor))
                            }
                        },
                        text = stringResource(R.string.legend_weekend)
                    )
                    LegendItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceLowest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("·", fontSize = 14.sp, color = OutlineColor)
                            }
                        },
                        text = stringResource(R.string.legend_working_day)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(icon: @Composable () -> Unit, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextOnSurface
        )
    }
}
