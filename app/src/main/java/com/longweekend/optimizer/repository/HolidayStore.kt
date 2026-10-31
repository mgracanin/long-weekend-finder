package com.longweekend.optimizer.repository

import com.google.gson.Gson
import com.longweekend.optimizer.model.StoredHolidays
import java.io.File

/**
 * Downloaded holiday lists, one small JSON file per country and year in the app's own storage,
 * kept until a newer download replaces them. Call from a background thread.
 */
internal class HolidayStore(filesDir: File, private val gson: Gson) {

    private val dir = File(filesDir, "holidays")

    private fun file(countryCode: String, year: Int) = File(dir, "${countryCode}_$year.json")

    fun read(countryCode: String, year: Int): StoredHolidays? = try {
        file(countryCode, year).takeIf { it.exists() }
            ?.reader()?.use { gson.fromJson(it, StoredHolidays::class.java) }
            ?.takeIf { !it.holidays.isNullOrEmpty() }
    } catch (e: Exception) {
        // A damaged file is as good as none; the next download replaces it
        null
    }

    fun write(countryCode: String, year: Int, stored: StoredHolidays) {
        dir.mkdirs()
        // Written aside and renamed, so a crash mid-write never leaves half a file behind
        val tmp = File(dir, "${countryCode}_$year.tmp")
        tmp.writeText(gson.toJson(stored))
        if (!tmp.renameTo(file(countryCode, year))) tmp.delete()
    }

    /** Deletes lists for years before [year]; they can no longer be planned. */
    fun deleteBefore(year: Int) {
        dir.listFiles()?.forEach { f ->
            val fileYear = f.nameWithoutExtension.substringAfterLast('_').toIntOrNull()
            if (fileYear != null && fileYear < year) f.delete()
        }
    }
}
