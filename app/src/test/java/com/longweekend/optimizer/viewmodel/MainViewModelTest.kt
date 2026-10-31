package com.longweekend.optimizer.viewmodel

import com.longweekend.optimizer.model.Country
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.model.PublicHoliday
import com.longweekend.optimizer.model.StoredHolidays
import com.longweekend.optimizer.repository.HolidayRepository
import com.longweekend.optimizer.repository.HolidayStore
import com.longweekend.optimizer.repository.SettingsRepository
import com.longweekend.optimizer.testing.FakeHolidayApi
import com.longweekend.optimizer.testing.FakeSharedPreferences
import com.longweekend.optimizer.testing.holiday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val gson = HolidayRepository.createGson()
    private val api = FakeHolidayApi { code, year -> croatianHolidays(year, code) }
    private val settings = SettingsRepository(FakeSharedPreferences()).apply {
        setDefaultCountryCode("HR")
        setOnboardingDone(true)
    }
    private var clock = 1_000_000_000_000L

    // Results for the current year drop breaks that have already started, so tests plan next year
    private val nextYear = LocalDate.now().year + 1

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = MainViewModel(
        repository = HolidayRepository(
            api = api,
            store = HolidayStore(folder.root, gson),
            prefs = FakeSharedPreferences(),
            gson = gson,
            ioDispatcher = dispatcher,
            now = { clock }
        ),
        settings = settings,
        computeDispatcher = dispatcher
    )

    /** A view model showing the results for next year. */
    private fun TestScope.viewModelForNextYear() = viewModel().also {
        it.setYear(nextYear)
        advanceUntilIdle()
    }

    @Test
    fun searchFindsBreaksForTheHomeCountry() = runTest(dispatcher) {
        val viewModel = viewModelForNextYear()

        assertEquals(UiStatus.RESULTS, viewModel.uiStatus.value)
        assertFalse(viewModel.isOfflineData.value)
        assertEquals("HR", viewModel.selectedCountry.value.countryCode)
        val breaks = viewModel.opportunities.value
        assertTrue(breaks.isNotEmpty())
        assertTrue(breaks.all { it.id.startsWith("HR_") && it.leaveDays in 1..viewModel.leaveBudget.value })
        assertEquals("sorted by longest break", breaks.sortedByDescending { it.totalDays }.map { it.totalDays }, breaks.map { it.totalDays })
        assertTrue(viewModel.holidays.value.all { it.date.startsWith("$nextYear-") })
    }

    @Test
    fun anotherYearNeverShowsThePreviousYearsHolidays() = runTest(dispatcher) {
        val viewModel = viewModelForNextYear()
        viewModel.setYear(nextYear + 1)
        assertTrue("cleared while loading", viewModel.holidays.value.isEmpty())

        advanceUntilIdle()
        assertTrue(viewModel.holidays.value.isNotEmpty())
        assertTrue(viewModel.holidays.value.all { it.date.startsWith("${nextYear + 1}-") })
    }

    @Test
    fun leaveBudgetStaysWithinTheAllowance() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setAnnualAllowance(10)
        viewModel.setLeaveBudget(50)
        assertEquals(10, viewModel.leaveBudget.value)
        viewModel.setLeaveBudget(0)
        assertEquals(1, viewModel.leaveBudget.value)
    }

    @Test
    fun savingABreakMarksItSavedEverywhere() = runTest(dispatcher) {
        val viewModel = viewModelForNextYear()
        val chosen = viewModel.opportunities.value.first()
        viewModel.selectOpportunity(chosen)

        viewModel.toggleSavePlan(chosen)
        assertTrue(viewModel.opportunities.value.first { it.id == chosen.id }.isSaved)
        assertTrue(viewModel.selectedOpportunity.value!!.isSaved)
        assertEquals(listOf(chosen.id), viewModel.savedPlans.value.map { it.opportunity.id })

        viewModel.toggleSavePlan(chosen)
        assertFalse(viewModel.opportunities.value.first { it.id == chosen.id }.isSaved)
        assertTrue(viewModel.savedPlans.value.isEmpty())
    }

    @Test
    fun countryWithoutDataOfflineShowsAnEmptyOfflineState() = runTest(dispatcher) {
        val viewModel = viewModelForNextYear()
        api.online = false
        // Brazil has no built-in calendar, so offline there is nothing to plan with
        viewModel.setCountry(Country.forCode("BR")!!)
        advanceUntilIdle()

        assertEquals(UiStatus.EMPTY, viewModel.uiStatus.value)
        assertTrue(viewModel.isOfflineData.value)
        assertTrue(viewModel.opportunities.value.isEmpty())
    }

    @Test
    fun topPicksAreDistinctBreaks() = runTest(dispatcher) {
        val viewModel = viewModelForNextYear()
        val picks = mutableListOf<List<Opportunity>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.topPicks.toList(picks) }
        advanceUntilIdle()

        val top = picks.last()
        assertTrue(top.size in 2..4)
        for ((i, a) in top.withIndex()) for (b in top.drop(i + 1)) {
            assertTrue("$a overlaps $b", a.endDate.isBefore(b.startDate) || b.endDate.isBefore(a.startDate))
        }
    }

    @Test
    fun backgroundUpdateRedoesTheSearchWithoutALoadingState() = runTest(dispatcher) {
        // A year the first search doesn't touch, and no prefetch, so only the search below updates it
        val year = nextYear + 1
        settings.setOnboardingDone(false)
        val store = HolidayStore(folder.root, gson)
        store.write("HR", year, StoredHolidays(listOf(holiday("$year-12-25")), fetchedAt = 0))
        store.write("HR", year + 1, StoredHolidays(croatianHolidays(year + 1, "HR"), fetchedAt = clock))
        clock += HolidayRepository.UPDATE_AFTER_MS + 1

        val viewModel = viewModel()
        advanceUntilIdle()
        api.latencyMs = 3_000
        val statuses = mutableListOf<UiStatus>()
        val holidayCounts = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiStatus.toList(statuses) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.holidays.collect { holidayCounts += it.size }
        }

        viewModel.setYear(year)
        advanceUntilIdle()

        assertTrue("the outdated list was shown first", 1 in holidayCounts)
        assertEquals("then replaced by the download", croatianHolidays(year, "HR").size, holidayCounts.last())
        assertEquals(
            "only the year change shows loading, the update after it doesn't",
            listOf(UiStatus.RESULTS, UiStatus.LOADING, UiStatus.RESULTS),
            statuses
        )
    }

    @Test
    fun finishingOnboardingSavesTheProfileAndDownloadsTheHomeCountry() = runTest(dispatcher) {
        settings.setOnboardingDone(false)
        val viewModel = viewModel()
        advanceUntilIdle()
        api.requests.clear()

        viewModel.completeOnboarding(Country.forCode("DE")!!, allowance = 28)
        advanceUntilIdle()

        assertTrue(viewModel.onboardingDone.value)
        assertEquals("DE", viewModel.defaultCountry.value.countryCode)
        assertEquals(28, viewModel.annualAllowance.value)
        val year = viewModel.currentYear
        assertTrue(api.requests.containsAll((year..year + 5).map { "DE" to it }))
    }

    private fun croatianHolidays(year: Int, code: String): List<PublicHoliday> =
        listOf("01-01", "01-06", "05-01", "05-30", "06-22", "08-05", "08-15", "11-01", "11-18", "12-25", "12-26")
            .map { holiday("$year-$it", countryCode = code) }
}
