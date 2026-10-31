package com.longweekend.optimizer.repository

import android.content.Context
import com.longweekend.optimizer.BuildConfig
import com.longweekend.optimizer.api.NagerDateApi
import com.longweekend.optimizer.model.Planning
import com.longweekend.optimizer.model.PublicHoliday
import com.longweekend.optimizer.model.StoredHolidays
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Holidays for one country/year, and whether they came from the built-in offline calendar. */
data class HolidayFetchResult(
    val holidays: List<PublicHoliday>,
    val isOffline: Boolean
)

/** [storedYears] of [totalYears] holiday lists are on the phone; the oldest was downloaded at [oldestUpdate]. */
data class HolidayDataStatus(
    val storedYears: Int,
    val totalYears: Int,
    val oldestUpdate: Long?
)

/**
 * Public holidays: downloaded, kept on the phone, or built in.
 * Tests pass fakes for the API, the dispatcher and the clock.
 */
class HolidayRepository internal constructor(
    private val api: NagerDateApi,
    private val store: HolidayStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis
) {

    // Background downloads outlive any one screen; they only ever update the store and caches
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val holidayCache = ConcurrentHashMap<Pair<String, Int>, CachedHolidays>()
    private val downloading: MutableSet<Pair<String, Int>> = ConcurrentHashMap.newKeySet()

    private val _holidaysChanged = MutableSharedFlow<Pair<String, Int>>(extraBufferCapacity = 16)

    /** Country/year lists whose saved holidays changed after a download, so open searches can redo. */
    val holidaysChanged: SharedFlow<Pair<String, Int>> = _holidaysChanged.asSharedFlow()

    private val _storeVersion = MutableStateFlow(0)

    /** Changes whenever a list is saved, for screens that show how fresh the data is. */
    val storeVersion: StateFlow<Int> = _storeVersion.asStateFlow()

    /**
     * Returns the public holidays for [countryCode] in [year], from the phone whenever they were
     * downloaded before; lists older than a week are updated in the background. Only lists never
     * downloaded wait for the API, and without a connection the built-in calendar stands in.
     * [forceRefresh] asks the API straight away.
     */
    suspend fun fetchHolidays(countryCode: String, year: Int, forceRefresh: Boolean = false): HolidayFetchResult {
        val key = countryCode to year
        if (!forceRefresh) {
            holidayCache[key]?.takeIf { it.isFresh(now()) }?.let { return it.result }
            val saved = withContext(ioDispatcher) { store.read(countryCode, year) }
            if (saved != null) {
                if (saved.isOutdated()) scope.launch { download(countryCode, year) }
                return cacheResult(key, saved.holidays.orEmpty(), retrySoon = false)
            }
        }

        val downloaded = download(countryCode, year)
        if (!downloaded.isNullOrEmpty()) return cacheResult(key, downloaded, retrySoon = false)
        // A refresh without a connection keeps what the phone already has
        val saved = withContext(ioDispatcher) { store.read(countryCode, year) }
        return if (saved != null) cacheResult(key, saved.holidays.orEmpty(), retrySoon = true)
        else cacheResult(key, OfflineHolidayCalendar.holidays(countryCode, year), retrySoon = true, isOffline = true)
    }

    private fun cacheResult(
        key: Pair<String, Int>,
        holidays: List<PublicHoliday>,
        retrySoon: Boolean,
        isOffline: Boolean = false
    ): HolidayFetchResult {
        val result = HolidayFetchResult(holidays, isOffline)
        holidayCache[key] = CachedHolidays(result, now(), retrySoon)
        return result
    }

    /**
     * Downloads one list and saves it. Returns null when the API can't be reached (or the same
     * list is already downloading), and an empty list when the API has nothing for that year.
     */
    private suspend fun download(countryCode: String, year: Int): List<PublicHoliday>? {
        val key = countryCode to year
        if (!downloading.add(key)) return null
        try {
            val holidays = try {
                api.getPublicHolidays(year, countryCode)
            } catch (e: CancellationException) {
                // A superseded search must not count as the API failing
                throw e
            } catch (e: Exception) {
                return null
            }
            if (holidays.isEmpty()) return holidays
            val changed = withContext(ioDispatcher) {
                val previous = store.read(countryCode, year)
                store.write(countryCode, year, StoredHolidays(holidays, now()))
                previous?.holidays != holidays
            }
            cacheResult(key, holidays, retrySoon = false)
            _storeVersion.update { it + 1 }
            if (changed) _holidaysChanged.tryEmit(key)
            return holidays
        } finally {
            downloading.remove(key)
        }
    }

    /**
     * Downloads, in the background, every plannable year of [countryCode] that isn't on the phone
     * yet or is older than a week, so the app works offline for it. Years gone by are tidied away.
     */
    fun prefetch(countryCode: String, currentYear: Int) {
        scope.launch {
            store.deleteBefore(currentYear - 1)
            for (year in Planning.holidayYears(currentYear)) {
                val saved = store.read(countryCode, year)
                if (saved != null && !saved.isOutdated() || (countryCode to year) in downloading) continue
                // Without a connection the rest would fail the same way; the next start tries again
                if (download(countryCode, year) == null) break
            }
        }
    }

    /** Downloads every plannable year of [countryCode] again; false when any of them failed. */
    suspend fun refreshAll(countryCode: String, currentYear: Int): Boolean =
        // Runs in the repository's scope, so leaving the screen doesn't stop it halfway
        scope.async {
            Planning.holidayYears(currentYear)
                .map { year -> async { download(countryCode, year) != null } }
                .awaitAll()
                .all { it }
        }.await()

    /** How much of [countryCode]'s plannable holiday data is on the phone, and how old it is. */
    suspend fun storedStatus(countryCode: String, currentYear: Int): HolidayDataStatus =
        withContext(ioDispatcher) {
            val years = Planning.holidayYears(currentYear)
            val saved = years.mapNotNull { store.read(countryCode, it) }
            HolidayDataStatus(
                storedYears = saved.size,
                totalYears = years.count(),
                oldestUpdate = saved.minOfOrNull { it.fetchedAt }
            )
        }

    private fun StoredHolidays.isOutdated(): Boolean =
        now() - fetchedAt > UPDATE_AFTER_MS

    private class CachedHolidays(val result: HolidayFetchResult, val fetchedAt: Long, val retrySoon: Boolean) {
        /** Saved and downloaded lists last the session; stand-ins are retried after a short while. */
        fun isFresh(now: Long): Boolean =
            !retrySoon || now - fetchedAt < OFFLINE_RETRY_AFTER_MS
    }

    companion object {
        internal const val OFFLINE_RETRY_AFTER_MS = 60_000L
        // The API itself allows a week of caching; holidays rarely change faster than that
        internal const val UPDATE_AFTER_MS = 7L * 24 * 60 * 60 * 1000

        @Volatile
        private var instance: HolidayRepository? = null

        fun getInstance(context: Context): HolidayRepository {
            return instance ?: synchronized(this) {
                val app = context.applicationContext
                instance ?: HolidayRepository(
                    api = createApi(),
                    store = HolidayStore(app.filesDir, createGson())
                ).also { instance = it }
            }
        }

        private fun createApi(): NagerDateApi {
            val logging = HttpLoggingInterceptor().apply {
                // Request logging is for debugging only; release builds log nothing
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://date.nager.at/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NagerDateApi::class.java)
        }
    }
}
