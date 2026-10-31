package com.longweekend.optimizer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longweekend.optimizer.R
import com.longweekend.optimizer.model.Planning
import com.longweekend.optimizer.ui.theme.*

/**
 * The years leave can be planned for, all visible at once as one segmented strip: a single tap
 * picks a year, and "this" / "next" captions orient the user without reading the numbers.
 */
@Composable
fun YearPicker(
    selectedYear: Int,
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceLowest,
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Planning.years(currentYear).forEach { year ->
                YearSegment(
                    year = year,
                    caption = when (year) {
                        currentYear -> stringResource(R.string.year_short_this)
                        currentYear + 1 -> stringResource(R.string.year_short_next)
                        else -> null
                    },
                    selected = year == selectedYear,
                    onClick = { onYearSelected(year) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun YearSegment(
    year: Int,
    caption: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fill by animateColorAsState(if (selected) SelectedFill else Color.Transparent, label = "yearFill")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$year",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) OnSelectedFill else TextOnSurface,
            maxLines = 1,
            softWrap = false
        )
        // Every segment keeps the caption line, so the numbers stay on one baseline
        Text(
            text = caption ?: " ",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (selected) OnSelectedFill.copy(alpha = 0.8f) else TextOnSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}
