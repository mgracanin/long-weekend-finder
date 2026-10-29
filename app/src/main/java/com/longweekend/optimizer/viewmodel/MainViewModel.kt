package com.longweekend.optimizer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.longweekend.optimizer.model.Country
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.model.PublicHoliday
import com.longweekend.optimizer.model.SavedPlan
import com.longweekend.optimizer.optimizer.HolidayBridgeOptimizer
import com.longweekend.optimizer.repository.HolidayRepository
import com.longweekend.optimizer.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class UiStatus {
    RESULTS,
    LOADING,
    EMPTY,
    ERROR
}

enum class SortOrder {
    LONGEST_BREAK,
    FEWEST_LEAVE_DAYS,
    UPCOMING_FIRST
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HolidayRepository.getInstance(application)
    private val settings = SettingsRepository.getInstance(application)

    /** Every country the holiday API covers, sorted by name in the current language by the UI. */
    val supportedCountries: List<Country> = Country.all

    val currentYear: Int = LocalDate.now().year

    val annualAllowance: StateFlow<Int> = settings.annualAllowance
    val defaultLeaveBudget: StateFlow<Int> = settings.defaultLeaveBudget
    val onboardingDone: StateFlow<Boolean> = settings.onboardingDone

    val defaultCountry: StateFlow<Country> = settings.defaultCountryCode
        .map(::countryFor)
        .stateIn(viewModelScope, SharingStarted.Eagerly, countryFor(settings.defaultCountryCode.value))

    private val _selectedCountry = MutableStateFlow(countryFor(settings.defaultCountryCode.value))
    val selectedCountry: StateFlow<Country> = _selectedCountry.asStateFlow()

    private val _selectedYear = MutableStateFlow(currentYear)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _leaveBudget = MutableStateFlow(settings.defaultLeaveBudget.value)
    val leaveBudget: StateFlow<Int> = _leaveBudget.asStateFlow()

    private val _selectedSeason = MutableStateFlow("any")
    val selectedSeason: StateFlow<String> = _selectedSeason.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.LONGEST_BREAK)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _uiStatus = MutableStateFlow(UiStatus.LOADING)
    val uiStatus: StateFlow<UiStatus> = _uiStatus.asStateFlow()

    private val _isOfflineData = MutableStateFlow(false)
    val isOfflineData: StateFlow<Boolean> = _isOfflineData.asStateFlow()

    /** Every public holiday (national and regional) for the selected country and year. */
    private val _holidays = MutableStateFlow<List<PublicHoliday>>(emptyList())
    val holidays: StateFlow<List<PublicHoliday>> = _holidays.asStateFlow()
    private var holidaysKey: Pair<String, Int>? = null

    private val _opportunities = MutableStateFlow<List<Opportunity>>(emptyList())
    val opportunities: StateFlow<List<Opportunity>> = _opportunities.asStateFlow()

    /**
     * The most efficient breaks for the current search, shown as "Top picks" on Discover.
     * Overlapping breaks (e.g. 31 Dec–10 Jan vs 1 Jan–11 Jan) are skipped so picks stay distinct.
     */
    val topPicks: StateFlow<List<Opportunity>> = _opportunities
        .map { list ->
            list.sortedWith(compareByDescending<Opportunity> { it.efficiency }.thenByDescending { it.totalDays })
                .fold(mutableListOf<Opportunity>()) { picks, opp ->
                    val overlaps = picks.any { !opp.startDate.isAfter(it.endDate) && !opp.endDate.isBefore(it.startDate) }
                    if (!overlaps && picks.size < TOP_PICKS_COUNT) picks.add(opp)
                    picks
                }
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedOpportunity = MutableStateFlow<Opportunity?>(null)
    val selectedOpportunity: StateFlow<Opportunity?> = _selectedOpportunity.asStateFlow()

    val savedPlans: StateFlow<List<SavedPlan>> = repository.savedPlans

    private var searchJob: Job? = null

    init {
        searchBreaks()
    }

    fun setCountry(country: Country) {
        if (country == _selectedCountry.value) return
        _selectedCountry.value = country
        searchBreaks()
    }

    /** Updates the saved default country and switches the current search to it. */
    fun setDefaultCountry(country: Country) {
        settings.setDefaultCountryCode(country.countryCode)
        setCountry(country)
    }

    private fun countryFor(code: String): Country =
        Country.forCode(code) ?: Country.forCode(SettingsRepository.deviceCountryCode())!!

    fun setYear(year: Int) {
        if (year == _selectedYear.value) return
        _selectedYear.value = year
        searchBreaks()
    }

    fun setLeaveBudget(budget: Int) {
        val coerced = budget.coerceIn(1, annualAllowance.value)
        if (coerced == _leaveBudget.value) return
        _leaveBudget.value = coerced
        searchBreaks()
    }

    fun setSeason(season: String) {
        if (season == _selectedSeason.value) return
        _selectedSeason.value = season
        searchBreaks()
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        _opportunities.value = sorted(_opportunities.value)
    }

    fun setAnnualAllowance(days: Int) {
        settings.setAnnualAllowance(days)
        if (_leaveBudget.value > annualAllowance.value) setLeaveBudget(annualAllowance.value)
    }

    /** Updates the saved default and applies it to the current search straight away. */
    fun setDefaultLeaveBudget(days: Int) {
        settings.setDefaultLeaveBudget(days)
        setLeaveBudget(defaultLeaveBudget.value)
    }

    fun resetProfile() {
        settings.setDefaultCountryCode(SettingsRepository.deviceCountryCode())
        settings.setAnnualAllowance(SettingsRepository.DEFAULT_ANNUAL_ALLOWANCE)
        settings.setDefaultLeaveBudget(SettingsRepository.DEFAULT_LEAVE_BUDGET)
        _selectedCountry.value = defaultCountry.value
        _leaveBudget.value = defaultLeaveBudget.value
        searchBreaks()
    }

    fun resetFilters() {
        _selectedSeason.value = "any"
        _leaveBudget.value = defaultLeaveBudget.value
        searchBreaks()
    }

    /** Re-fetches holidays from the API, bypassing the cache. */
    fun refresh() {
        searchBreaks(forceRefresh = true)
    }

    fun selectOpportunity(opp: Opportunity) {
        _selectedOpportunity.value = opp
    }

    fun toggleSavePlan(opp: Opportunity, checkedStates: List<Boolean>? = null) {
        val isNowSaved = repository.toggleSavePlan(opp, checkedStates)
        _opportunities.value = _opportunities.value.map {
            if (it.id == opp.id) it.copy(isSaved = isNowSaved) else it
        }
        if (_selectedOpportunity.value?.id == opp.id) {
            _selectedOpportunity.value = _selectedOpportunity.value?.copy(isSaved = isNowSaved)
        }
    }

    fun updateChecklistItem(oppId: String, index: Int, isChecked: Boolean) {
        repository.updateChecklistItem(oppId, index, isChecked)
    }

    /** True when every start date [season] allows in [year] is already behind us. */
    fun seasonHasPassed(season: String, year: Int): Boolean {
        val lastMonth = when (season) {
            "spring" -> 5
            "summer" -> 8
            "autumn" -> 11
            else -> return false
        }
        val today = LocalDate.now()
        return year == today.year && today.monthValue > lastMonth
    }

    /** Finishes the welcome screens, applying the country and allowance picked there. */
    fun completeOnboarding(country: Country, allowance: Int) {
        settings.setAnnualAllowance(allowance)
        setDefaultCountry(country)
        settings.setOnboardingDone(true)
    }

    fun showOnboardingAgain() {
        settings.setOnboardingDone(false)
    }

    /** Leave days already committed in saved plans for [year]. */
    fun plannedLeaveDays(plans: List<SavedPlan>, year: Int): Int =
        plans.filter { it.opportunity.startDate.year == year }.sumOf { it.opportunity.leaveDays }

    private fun searchBreaks(forceRefresh: Boolean = false) {
        // A newer search always wins; stale results must never overwrite it
        searchJob?.cancel()
        // A new country/year must never show the previous one's holidays while loading
        val key = _selectedCountry.value.countryCode to _selectedYear.value
        if (key != holidaysKey) {
            _holidays.value = emptyList()
            holidaysKey = key
        }
        searchJob = viewModelScope.launch {
            _uiStatus.value = UiStatus.LOADING
            val startedAt = System.currentTimeMillis()
            try {
                val country = _selectedCountry.value
                val year = _selectedYear.value
                // Next year's holidays are needed for breaks that run past 31 December
                val (result, nextYear) = coroutineScope {
                    val current = async { repository.fetchHolidays(country.countryCode, year, forceRefresh) }
                    val next = async { repository.fetchHolidays(country.countryCode, year + 1, forceRefresh) }
                    current.await() to next.await()
                }

                val generated = withContext(Dispatchers.Default) {
                    HolidayBridgeOptimizer.generateOpportunities(
                        holidays = result.holidays + nextYear.holidays,
                        year = year,
                        leaveBudget = _leaveBudget.value,
                        seasonFilter = _selectedSeason.value
                    )
                }

                // Breaks that have already started this year can no longer be booked
                val today = LocalDate.now()
                val bookable = (if (year == today.year) generated.filter { it.startDate.isAfter(today) } else generated)
                    // Same dates in two countries are different plans; ids must not collide
                    .map { it.copy(id = "${country.countryCode}_${it.id}", countryCode = country.countryCode, countryName = country.name) }

                if (forceRefresh) {
                    // Keep the loading state on screen long enough to be noticed
                    delay((MIN_REFRESH_DISPLAY_MS - (System.currentTimeMillis() - startedAt)).coerceAtLeast(0))
                }

                _isOfflineData.value = result.isOffline
                _holidays.value = result.holidays
                    .distinctBy { it.date to it.name }
                    .sortedBy { it.date }
                _opportunities.value = sorted(bookable.map { it.copy(isSaved = repository.isPlanSaved(it.id)) })
                _uiStatus.value = if (bookable.isEmpty()) UiStatus.EMPTY else UiStatus.RESULTS
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Don't leave the previous country's/year's breaks on screen
                _opportunities.value = emptyList()
                _holidays.value = emptyList()
                _uiStatus.value = UiStatus.ERROR
            }
        }
    }

    private fun sorted(list: List<Opportunity>): List<Opportunity> = list.sortedWith(
        when (_sortOrder.value) {
            SortOrder.LONGEST_BREAK -> compareByDescending<Opportunity> { it.totalDays }.thenBy { it.leaveDays }
            SortOrder.FEWEST_LEAVE_DAYS -> compareBy<Opportunity> { it.leaveDays }.thenByDescending { it.totalDays }
            SortOrder.UPCOMING_FIRST -> compareBy { it.startDate }
        }.thenBy { it.startDate }
    )

    companion object {
        private const val TOP_PICKS_COUNT = 4
        private const val MIN_REFRESH_DISPLAY_MS = 900L
    }
}
