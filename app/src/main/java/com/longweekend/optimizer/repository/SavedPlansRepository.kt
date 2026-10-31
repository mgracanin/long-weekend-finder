package com.longweekend.optimizer.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.longweekend.optimizer.model.ChecklistItem
import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.model.SavedPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The breaks the user saved, with their "requested from HR" ticks, persisted in SharedPreferences. */
class SavedPlansRepository internal constructor(
    private val prefs: SharedPreferences,
    private val gson: Gson = createGson()
) {

    private val _savedPlans = MutableStateFlow(loadSavedPlans())

    /** Newest first. */
    val savedPlans: StateFlow<List<SavedPlan>> = _savedPlans.asStateFlow()

    fun isPlanSaved(opportunityId: String): Boolean {
        return _savedPlans.value.any { it.opportunity.id == opportunityId }
    }

    /**
     * Saves or removes [opportunity]. When saving, [checkedStates] carries over the
     * "requested from HR" ticks the user already made on the detail screen.
     */
    fun toggleSavePlan(opportunity: Opportunity, checkedStates: List<Boolean>? = null): Boolean {
        val currentList = _savedPlans.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.opportunity.id == opportunity.id }
        val nowSaved = if (existingIndex >= 0) {
            currentList.removeAt(existingIndex)
            false
        } else {
            val checklist = opportunity.days
                .filter { it.type == DayType.ANNUAL_LEAVE }
                .mapIndexed { idx, day ->
                    ChecklistItem(
                        date = day.date,
                        title = day.date.format(ChecklistItem.TITLE_FORMAT),
                        isChecked = checkedStates?.getOrNull(idx) ?: false
                    )
                }
            currentList.add(0, SavedPlan(opportunity.copy(isSaved = true), checklist))
            true
        }
        updateSavedPlans(currentList)
        return nowSaved
    }

    fun updateChecklistItem(opportunityId: String, itemIndex: Int, isChecked: Boolean) {
        val currentList = _savedPlans.value.map { plan ->
            if (plan.opportunity.id == opportunityId && itemIndex in plan.checklistItems.indices) {
                val updatedItems = plan.checklistItems.toMutableList()
                updatedItems[itemIndex] = updatedItems[itemIndex].copy(isChecked = isChecked)
                plan.copy(checklistItems = updatedItems)
            } else {
                plan
            }
        }
        updateSavedPlans(currentList)
    }

    private fun updateSavedPlans(plans: List<SavedPlan>) {
        _savedPlans.value = plans
        prefs.edit().putString(KEY_SAVED_PLANS, gson.toJson(plans)).apply()
    }

    private fun loadSavedPlans(): List<SavedPlan> {
        val json = prefs.getString(KEY_SAVED_PLANS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedPlan>>() {}.type
            gson.fromJson<List<SavedPlan>>(json, type).orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        // Kept from when saved plans lived in HolidayRepository, so existing plans are found
        private const val PREFS_NAME = "saved_plans"
        internal const val KEY_SAVED_PLANS = "plans_json"

        @Volatile
        private var instance: SavedPlansRepository? = null

        fun getInstance(context: Context): SavedPlansRepository {
            return instance ?: synchronized(this) {
                instance ?: SavedPlansRepository(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
