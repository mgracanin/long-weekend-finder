package com.longweekend.optimizer.repository

import com.longweekend.optimizer.model.StoredHolidays
import com.longweekend.optimizer.optimizer.HolidayBridgeOptimizer
import com.longweekend.optimizer.testing.FakeHolidayApi
import com.longweekend.optimizer.testing.FakeSharedPreferences
import com.longweekend.optimizer.testing.holiday
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HolidayRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val year = 2030
    private val gson = HolidayRepository.createGson()
    private val api = FakeHolidayApi { code, year -> listOf(holiday("$year-12-25", countryCode = code)) }
    private val prefs = FakeSharedPreferences()
    private var clock = 1_000_000_000_000L

    /** A new repository over the same phone storage, as after an app restart. */
    private fun TestScope.repository() = HolidayRepository(
        api = api,
        store = HolidayStore(folder.root, gson),
        prefs = prefs,
        gson = gson,
        ioDispatcher = StandardTestDispatcher(testScheduler),
        now = { clock }
    )

    @Test
    fun firstFetchDownloadsAndLaterStartsReadFromThePhone() = runTest {
        val first = repository().fetchHolidays("HR", year)
        assertFalse(first.isOffline)
        assertEquals(listOf("HR" to year), api.requests)

        val afterRestart = repository().fetchHolidays("HR", year)
        assertEquals(first, afterRestart)
        assertEquals("no second download", 1, api.requests.size)
    }

    @Test
    fun outdatedListIsShownAndUpdatedInTheBackground() = runTest {
        val old = repository().fetchHolidays("HR", year).holidays
        clock += HolidayRepository.UPDATE_AFTER_MS + 1
        val updated = listOf(holiday("$year-12-25"), holiday("$year-12-26"))
        api.holidays = { _, _ -> updated }

        val repository = repository()
        val changes = mutableListOf<Pair<String, Int>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.holidaysChanged.toList(changes) }

        assertEquals("the saved list comes back straight away", old, repository.fetchHolidays("HR", year).holidays)
        advanceUntilIdle()
        assertEquals(listOf("HR" to year), changes)
        assertEquals(updated, repository.fetchHolidays("HR", year).holidays)
    }

    @Test
    fun unchangedUpdateIsNotReportedAsAChange() = runTest {
        repository().fetchHolidays("HR", year)
        clock += HolidayRepository.UPDATE_AFTER_MS + 1

        val repository = repository()
        val changes = mutableListOf<Pair<String, Int>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.holidaysChanged.toList(changes) }
        repository.fetchHolidays("HR", year)
        advanceUntilIdle()

        assertEquals(2, api.requests.size)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun withoutConnectionTheBuiltInCalendarStandsIn() = runTest {
        api.online = false
        val result = repository().fetchHolidays("HR", year)
        assertTrue(result.isOffline)
        assertTrue(result.holidays.isNotEmpty())
        assertEquals(OfflineHolidayCalendar.holidays("HR", year), result.holidays)
    }

    @Test
    fun refreshWithoutConnectionKeepsTheSavedList() = runTest {
        val repository = repository()
        val saved = repository.fetchHolidays("HR", year)
        api.online = false
        assertEquals(saved, repository.fetchHolidays("HR", year, forceRefresh = true))
    }

    @Test
    fun builtInStandInIsRetriedAfterAMinute() = runTest {
        val repository = repository()
        api.online = false
        assertTrue(repository.fetchHolidays("HR", year).isOffline)

        api.online = true
        assertTrue("still within the minute", repository.fetchHolidays("HR", year).isOffline)
        clock += HolidayRepository.OFFLINE_RETRY_AFTER_MS + 1
        assertFalse(repository.fetchHolidays("HR", year).isOffline)
    }

    @Test
    fun prefetchKeepsEveryPlannableYearOnThePhone() = runTest {
        val repository = repository()
        repository.prefetch("HR", year)
        advanceUntilIdle()

        assertEquals((year..year + 5).map { "HR" to it }, api.requests)
        assertEquals(HolidayDataStatus(storedYears = 6, totalYears = 6, oldestUpdate = clock), repository.storedStatus("HR", year))
    }

    @Test
    fun prefetchSkipsFreshYearsAndStopsWhenOffline() = runTest {
        val store = HolidayStore(folder.root, gson)
        store.write("HR", year, StoredHolidays(listOf(holiday("$year-12-25")), fetchedAt = clock))
        api.online = false

        repository().prefetch("HR", year)
        advanceUntilIdle()

        assertEquals("the fresh year is skipped, the first failure ends it", listOf("HR" to year + 1), api.requests)
    }

    @Test
    fun refreshAllReportsWhetherEveryYearDownloaded() = runTest {
        val repository = repository()
        api.online = false
        assertFalse(repository.refreshAll("HR", year))
        api.online = true
        assertTrue(repository.refreshAll("HR", year))
        assertEquals(6, repository.storedStatus("HR", year).storedYears)
    }

    @Test
    fun savedPlansAndTheirTicksSurviveARestart() = runTest {
        val opportunity = HolidayBridgeOptimizer.generateOpportunities(
            holidays = listOf(holiday("2030-12-25"), holiday("2030-12-26")),
            year = year,
            leaveBudget = 3
        ).first { it.leaveDays >= 2 }

        val repository = repository()
        assertTrue(repository.toggleSavePlan(opportunity, checkedStates = listOf(true)))
        assertTrue(repository.isPlanSaved(opportunity.id))
        repository.updateChecklistItem(opportunity.id, itemIndex = 1, isChecked = true)

        val restored = repository().savedPlans.value.single()
        assertEquals(opportunity.copy(isSaved = true), restored.opportunity)
        assertEquals(
            listOf(true, true) + List(restored.checklistItems.size - 2) { false },
            restored.checklistItems.map { it.isChecked }
        )

        assertFalse("a second tap removes it", repository.toggleSavePlan(opportunity))
        assertTrue(repository().savedPlans.value.isEmpty())
    }
}
