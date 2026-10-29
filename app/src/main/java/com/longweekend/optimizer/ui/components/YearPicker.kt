package com.longweekend.optimizer.ui.components

import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longweekend.optimizer.ui.theme.*

private val YearItemWidth = 72.dp

/**
 * Horizontally scrollable year selector spanning [yearsAround] years either side of
 * [currentYear]. The selected year is always scrolled to the centre.
 */
@Composable
fun YearPicker(
    selectedYear: Int,
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    yearsAround: Int = 10
) {
    val years = remember(currentYear, yearsAround) {
        ((currentYear - yearsAround)..(currentYear + yearsAround)).toList()
    }
    val selectedIndex = years.indexOf(selectedYear).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    var hasPositioned by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex) {
        // Content padding of half the viewport means "first visible item" is the centred one
        if (hasPositioned) listState.animateScrollToItem(selectedIndex) else listState.scrollToItem(selectedIndex)
        hasPositioned = true
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onYearSelected(selectedYear - 1) },
            enabled = selectedYear > years.first(),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.ChevronLeft, stringResource(R.string.cd_previous_year), tint = if (selectedYear > years.first()) TealPrimary else OutlineVariant)
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val sidePadding = ((maxWidth - YearItemWidth) / 2).coerceAtLeast(0.dp)
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(years, key = { _, year -> year }) { _, year ->
                    YearItem(
                        year = year,
                        isSelected = year == selectedYear,
                        caption = when (year) {
                            currentYear -> stringResource(R.string.year_this)
                            currentYear + 1 -> stringResource(R.string.year_next)
                            currentYear - 1 -> stringResource(R.string.year_last)
                            else -> null
                        },
                        onClick = { onYearSelected(year) }
                    )
                }
            }
        }

        IconButton(
            onClick = { onYearSelected(selectedYear + 1) },
            enabled = selectedYear < years.last(),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.ChevronRight, stringResource(R.string.cd_next_year), tint = if (selectedYear < years.last()) TealPrimary else OutlineVariant)
        }
    }
}

@Composable
private fun YearItem(
    year: Int,
    isSelected: Boolean,
    caption: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(YearItemWidth)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SurfaceLowest else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$year",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (isSelected) TealPrimary else TextOnSurfaceVariant
        )
        Text(
            text = caption ?: " ",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = if (isSelected) AmberSecondary else TextOnSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
