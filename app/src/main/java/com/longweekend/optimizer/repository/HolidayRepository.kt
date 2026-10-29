package com.longweekend.optimizer.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.longweekend.optimizer.BuildConfig
import com.longweekend.optimizer.api.NagerDateApi
import com.longweekend.optimizer.model.ChecklistItem
import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.model.PublicHoliday
import com.longweekend.optimizer.model.SavedPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Holidays for one country/year, and whether they came from the built-in offline calendar. */
data class HolidayFetchResult(
    val holidays: List<PublicHoliday>,
    val isOffline: Boolean
)

class HolidayRepository private constructor(context: Context) {

    private val api: NagerDateApi
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .create()

    private val holidayCache = mutableMapOf<Pair<String, Int>, CachedHolidays>()

    private val _savedPlans = MutableStateFlow(loadSavedPlans())
    val savedPlans: StateFlow<List<SavedPlan>> = _savedPlans.asStateFlow()

    init {
        val logging = HttpLoggingInterceptor().apply {
            // Request logging is for debugging only; release builds log nothing
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://date.nager.at/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(NagerDateApi::class.java)
    }

    /**
     * Returns the public holidays for [countryCode] in [year]. Results are cached per session;
     * [forceRefresh] bypasses the cache and asks the API again.
     */
    suspend fun fetchHolidays(countryCode: String, year: Int, forceRefresh: Boolean = false): HolidayFetchResult {
        val key = countryCode to year
        if (!forceRefresh) {
            holidayCache[key]?.takeIf { it.isFresh() }?.let { return it.result }
        }

        val result = try {
            val holidays = api.getPublicHolidays(year, countryCode)
            if (holidays.isNotEmpty()) HolidayFetchResult(holidays, isOffline = false)
            else HolidayFetchResult(OfflineHolidayCalendar.holidays(countryCode, year), isOffline = true)
        } catch (e: CancellationException) {
            // A superseded search must not cache the offline fallback as if the API had failed
            throw e
        } catch (e: Exception) {
            HolidayFetchResult(OfflineHolidayCalendar.holidays(countryCode, year), isOffline = true)
        }
        holidayCache[key] = CachedHolidays(result, System.currentTimeMillis())
        return result
    }

    private class CachedHolidays(val result: HolidayFetchResult, val fetchedAt: Long) {
        /** API results last the session; offline fallbacks are retried after a short while. */
        fun isFresh(): Boolean =
            !result.isOffline || System.currentTimeMillis() - fetchedAt < OFFLINE_RETRY_AFTER_MS
    }

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

    private class LocalDateAdapter : TypeAdapter<LocalDate>() {
        override fun write(out: JsonWriter, value: LocalDate?) {
            if (value == null) out.nullValue() else out.value(value.toString())
        }

        override fun read(input: JsonReader): LocalDate = LocalDate.parse(input.nextString())
    }

    companion object {
        private const val PREFS_NAME = "saved_plans"
        private const val KEY_SAVED_PLANS = "plans_json"
        private const val OFFLINE_RETRY_AFTER_MS = 60_000L

        @Volatile
        private var instance: HolidayRepository? = null

        fun getInstance(context: Context): HolidayRepository {
            return instance ?: synchronized(this) {
                instance ?: HolidayRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
