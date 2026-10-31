package com.longweekend.optimizer.testing

import android.content.SharedPreferences
import com.longweekend.optimizer.api.NagerDateApi
import com.longweekend.optimizer.model.PublicHoliday
import kotlinx.coroutines.delay
import java.io.IOException

/** In-memory SharedPreferences; share one instance between repositories to simulate an app restart. */
class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = values.toMap()
    override fun getString(key: String, defValue: String?): String? = values[key] as String? ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = values[key] as Set<String>? ?: defValues
    override fun getInt(key: String, defValue: Int): Int = values[key] as Int? ?: defValue
    override fun getLong(key: String, defValue: Long): Long = values[key] as Long? ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = values[key] as Float? ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as Boolean? ?: defValue
    override fun contains(key: String): Boolean = key in values
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun putStringSet(key: String, values: Set<String>?) = apply { changes[key] = values }
        override fun putInt(key: String, value: Int) = apply { changes[key] = value }
        override fun putLong(key: String, value: Long) = apply { changes[key] = value }
        override fun putFloat(key: String, value: Float) = apply { changes[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }
        override fun remove(key: String) = apply { changes[key] = null }
        override fun clear() = apply { clear = true }
        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) values.clear()
            changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
        }
    }
}

/** Holiday API that answers from [holidays], or fails like a phone without a connection. */
class FakeHolidayApi(
    var holidays: (countryCode: String, year: Int) -> List<PublicHoliday> = { _, _ -> emptyList() }
) : NagerDateApi {
    var online = true
    /** How long each request takes, in virtual time; real downloads take seconds. */
    var latencyMs = 0L
    val requests = mutableListOf<Pair<String, Int>>()

    override suspend fun getPublicHolidays(year: Int, countryCode: String): List<PublicHoliday> {
        requests += countryCode to year
        delay(latencyMs)
        if (!online) throw IOException("No connection")
        return holidays(countryCode, year)
    }
}

/** A national holiday on [date] (yyyy-MM-dd). */
fun holiday(date: String, name: String = "Holiday $date", countryCode: String = "HR") = PublicHoliday(
    date = date,
    localName = name,
    name = name,
    countryCode = countryCode,
    global = true
)
