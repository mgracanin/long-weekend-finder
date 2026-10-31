package com.longweekend.optimizer.repository

import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.optimizer.HolidayBridgeOptimizer
import com.longweekend.optimizer.testing.FakeSharedPreferences
import com.longweekend.optimizer.testing.holiday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SavedPlansRepositoryTest {

    private val prefs = FakeSharedPreferences()

    /** Christmas 2030 falls on a Wednesday, so there are several breaks around it. */
    private val opportunities: List<Opportunity> = HolidayBridgeOptimizer.generateOpportunities(
        holidays = listOf(holiday("2030-12-25"), holiday("2030-12-26")),
        year = 2030,
        leaveBudget = 3
    )

    /** A new repository over the same preferences, as after an app restart. */
    private fun repository() = SavedPlansRepository(prefs)

    @Test
    fun savedPlansAndTheirTicksSurviveARestart() {
        val opportunity = opportunities.first { it.leaveDays >= 2 }

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
        assertFalse(repository.isPlanSaved(opportunity.id))
        assertTrue(repository().savedPlans.value.isEmpty())
    }

    @Test
    fun checklistHasOneItemPerLeaveDay() {
        val opportunity = opportunities.first { it.leaveDays >= 2 }

        val plan = repository().apply { toggleSavePlan(opportunity) }.savedPlans.value.single()

        assertEquals(
            opportunity.days.filter { it.type == DayType.ANNUAL_LEAVE }.map { it.date },
            plan.checklistItems.map { it.date }
        )
        assertEquals(opportunity.leaveDays, plan.checklistItems.size)
        assertTrue(plan.checklistItems.none { it.isChecked })
    }

    @Test
    fun newestPlanComesFirstAndRemovingOneKeepsTheRest() {
        val (first, second, third) = opportunities.take(3)
        val repository = repository()
        repository.toggleSavePlan(first)
        repository.toggleSavePlan(second)
        repository.toggleSavePlan(third)
        assertEquals(listOf(third.id, second.id, first.id), repository.savedPlans.value.map { it.opportunity.id })

        repository.toggleSavePlan(second)
        assertEquals(listOf(third.id, first.id), repository().savedPlans.value.map { it.opportunity.id })
    }

    @Test
    fun ticksForUnknownPlansOrDaysChangeNothing() {
        val opportunity = opportunities.first()
        val repository = repository()
        repository.toggleSavePlan(opportunity)
        val before = repository.savedPlans.value

        repository.updateChecklistItem("no-such-plan", itemIndex = 0, isChecked = true)
        repository.updateChecklistItem(opportunity.id, itemIndex = 99, isChecked = true)
        repository.updateChecklistItem(opportunity.id, itemIndex = -1, isChecked = true)

        assertEquals(before, repository.savedPlans.value)
    }

    @Test
    fun plansSavedByEarlierVersionsAreStillFound() {
        // As 1.3.x stored them, under the same key; this plan predates the country fields
        prefs.edit().putString(
            SavedPlansRepository.KEY_SAVED_PLANS,
            """
            [{"opportunity":{"id":"2030-12-24_2030-12-29","totalDays":6,"leaveDays":2,"publicHolidaysCount":2,
              "startDate":"2030-12-24","endDate":"2030-12-29",
              "days":[{"date":"2030-12-24","type":"ANNUAL_LEAVE"},{"date":"2030-12-25","type":"PUBLIC_HOLIDAY","holidayName":"Christmas Day"}],
              "isSaved":true},
              "checklistItems":[{"date":"2030-12-24","title":"Tuesday, 24 December 2030","subtitle":"Full Day • 8 hrs","isChecked":true}],
              "savedTimestamp":1700000000000}]
            """.trimIndent()
        ).apply()

        val plan = repository().savedPlans.value.single()
        assertEquals("2030-12-24_2030-12-29", plan.opportunity.id)
        assertEquals(LocalDate.of(2030, 12, 29), plan.opportunity.endDate)
        assertEquals(null, plan.opportunity.countryCode)
        assertTrue(plan.checklistItems.single().isChecked)
    }

    @Test
    fun damagedDataStartsWithNoPlans() {
        prefs.edit().putString(SavedPlansRepository.KEY_SAVED_PLANS, "{not json").apply()

        val repository = repository()
        assertTrue(repository.savedPlans.value.isEmpty())

        // And saving works again afterwards
        repository.toggleSavePlan(opportunities.first())
        assertEquals(1, repository().savedPlans.value.size)
    }
}
