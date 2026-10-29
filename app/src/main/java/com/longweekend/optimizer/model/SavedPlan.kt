package com.longweekend.optimizer.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ChecklistItem(
    val date: LocalDate,
    val title: String,
    val subtitle: String = FULL_DAY,
    val isChecked: Boolean = false
) {
    companion object {
        const val FULL_DAY = "Full Day • 8 hrs"
        val TITLE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
    }
}

data class SavedPlan(
    val opportunity: Opportunity,
    val checklistItems: List<ChecklistItem>,
    val savedTimestamp: Long = System.currentTimeMillis()
)
