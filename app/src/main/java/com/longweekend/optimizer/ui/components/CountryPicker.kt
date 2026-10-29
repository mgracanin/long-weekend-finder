package com.longweekend.optimizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.longweekend.optimizer.R
import com.longweekend.optimizer.model.Country
import com.longweekend.optimizer.ui.text.appLocale
import com.longweekend.optimizer.ui.theme.*
import java.text.Collator
import java.text.Normalizer

/** Shows the selected country; tapping opens a searchable list of every supported country. */
@Composable
fun CountryPicker(
    selected: Country,
    countries: List<Country>,
    caption: String,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val locale = appLocale()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDialog = true },
        color = SurfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = selected.flagEmoji, fontSize = 22.sp)
                Column {
                    Text(
                        text = selected.displayName(locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextOnSurface
                    )
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextOnSurfaceVariant
                    )
                }
            }
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TealPrimary)
        }
    }

    if (showDialog) {
        CountrySearchDialog(
            selected = selected,
            countries = countries,
            onDismiss = { showDialog = false },
            onCountrySelected = {
                onCountrySelected(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun CountrySearchDialog(
    selected: Country,
    countries: List<Country>,
    onDismiss: () -> Unit,
    onCountrySelected: (Country) -> Unit
) {
    val locale = appLocale()
    var query by remember { mutableStateOf("") }
    val sorted = remember(countries, locale) {
        val collator = Collator.getInstance(locale)
        countries.sortedWith(compareBy(collator) { it.displayName(locale) })
    }
    val filtered = remember(sorted, query) {
        val q = query.trim().foldForSearch()
        if (q.isEmpty()) sorted
        else sorted.filter {
            it.displayName(locale).foldForSearch().contains(q) ||
                it.name.foldForSearch().contains(q) ||
                it.countryCode.equals(q, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceLowest
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TealPrimary) },
                        placeholder = { Text(stringResource(R.string.search_country_hint)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.cd_back), tint = TextOnSurfaceVariant)
                    }
                }
                if (filtered.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_country_match, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(filtered, key = { it.countryCode }) { country ->
                        val isCurrent = country.countryCode == selected.countryCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) TealPrimaryFixed.copy(alpha = 0.5f) else SurfaceLowest)
                                .clickable { onCountrySelected(country) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(country.flagEmoji, fontSize = 20.sp)
                            Text(
                                text = country.displayName(locale),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) TealPrimary else TextOnSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCurrent) Icon(Icons.Default.Check, null, tint = TealPrimary)
                        }
                    }
                }
            }
        }
    }
}

/** Lower-case and strip accents so "hrv" matches "Hrvatska" and "cote" matches "Côte d'Ivoire". */
private fun String.foldForSearch(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
