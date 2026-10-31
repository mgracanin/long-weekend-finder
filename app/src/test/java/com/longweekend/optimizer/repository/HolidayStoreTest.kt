package com.longweekend.optimizer.repository

import com.google.gson.Gson
import com.longweekend.optimizer.model.PublicHoliday
import com.longweekend.optimizer.model.StoredHolidays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HolidayStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val christmas = PublicHoliday("2027-12-25", "Božić", "Christmas Day", "HR")

    @Test
    fun savedListReadsBackUnchanged() {
        val store = HolidayStore(folder.root, Gson())
        val stored = StoredHolidays(listOf(christmas), fetchedAt = 1234L)
        store.write("HR", 2027, stored)
        assertEquals(stored, store.read("HR", 2027))
        assertNull(store.read("HR", 2028))
    }

    @Test
    fun damagedOrEmptyFileCountsAsMissing() {
        val store = HolidayStore(folder.root, Gson())
        File(folder.root, "holidays").mkdirs()
        File(folder.root, "holidays/HR_2027.json").writeText("{not json")
        assertNull(store.read("HR", 2027))
        store.write("HR", 2028, StoredHolidays(emptyList(), fetchedAt = 1L))
        assertNull(store.read("HR", 2028))
    }

    @Test
    fun yearsGoneByAreDeleted() {
        val store = HolidayStore(folder.root, Gson())
        for (year in 2024..2027) store.write("HR", year, StoredHolidays(listOf(christmas), fetchedAt = 1L))
        store.deleteBefore(2026)
        assertNull(store.read("HR", 2025))
        assertEquals(1L, store.read("HR", 2026)?.fetchedAt)
    }
}
