package com.longweekend.optimizer.optimizer

import com.longweekend.optimizer.model.DayStripItem
import com.longweekend.optimizer.model.DayType
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.model.PublicHoliday
import java.time.DayOfWeek
import java.time.LocalDate

object HolidayBridgeOptimizer {

    private const val MIN_WINDOW_DAYS = 4
    private const val MAX_WINDOW_DAYS = 30

    /**
     * [holidays] should include the following year's holidays too, so breaks that run past
     * 31 December (e.g. Christmas into New Year) count those days correctly.
     */
    fun generateOpportunities(
        holidays: List<PublicHoliday>,
        year: Int,
        leaveBudget: Int,
        seasonFilter: String = "any"
    ): List<Opportunity> {
        val holidayMap = mutableMapOf<LocalDate, PublicHoliday>()
        for (h in holidays) {
            // Regional holidays (e.g. a single German state) are not days off for everyone
            if (!h.global) continue
            try {
                val d = LocalDate.parse(h.date)
                holidayMap.putIfAbsent(d, h)
            } catch (_: Exception) {}
        }

        val opportunities = mutableListOf<Opportunity>()

        // Look around every public holiday
        val holidayDates = holidayMap.keys.sorted()

        val processedIntervals = mutableSetOf<Pair<LocalDate, LocalDate>>()

        // A window can at most be the leave budget plus the surrounding weekends and holidays
        val maxWindowDays = (leaveBudget + 11).coerceIn(MIN_WINDOW_DAYS, MAX_WINDOW_DAYS)

        for (holidayDate in holidayDates) {
            if (holidayDate.year != year) continue

            for (windowDays in MIN_WINDOW_DAYS..maxWindowDays) {
                for (offset in -(windowDays - 1)..0) {
                    val start = holidayDate.plusDays(offset.toLong())
                    val end = start.plusDays((windowDays - 1).toLong())

                    // Breaks belong to the year they start in (that year's leave is spent)
                    if (start.year != year) continue

                    // Only maximal breaks: the days either side must be working days, otherwise
                    // the break could be extended for free (e.g. Sat–Thu around Easter Monday)
                    val isOff = { d: LocalDate -> isWeekend(d) || holidayMap.containsKey(d) }
                    if (isOff(start.minusDays(1)) || isOff(end.plusDays(1))) continue

                    val intervalKey = Pair(start, end)
                    if (processedIntervals.contains(intervalKey)) continue

                    // Count leave days, holidays, weekends
                    var leaveDaysCount = 0
                    var holidayCount = 0
                    var weekdayHolidayCount = 0
                    val dayItems = mutableListOf<DayStripItem>()

                    var curr = start
                    while (!curr.isAfter(end)) {
                        val holiday = holidayMap[curr]

                        val dayType = when {
                            holiday != null -> {
                                holidayCount++
                                if (!isWeekend(curr)) weekdayHolidayCount++
                                DayType.PUBLIC_HOLIDAY
                            }
                            isWeekend(curr) -> DayType.WEEKEND
                            else -> {
                                leaveDaysCount++
                                DayType.ANNUAL_LEAVE
                            }
                        }

                        dayItems.add(
                            DayStripItem(
                                date = curr,
                                type = dayType,
                                holidayName = holiday?.name,
                                holidayLocalName = holiday?.localName
                            )
                        )
                        curr = curr.plusDays(1)
                    }

                    // Candidate must require at least 1 leave day, but no more than leaveBudget,
                    // and a holiday on a weekday — one on a Saturday/Sunday saves no leave, so the
                    // "bridge" would just be an ordinary weekend plus leave
                    if (weekdayHolidayCount == 0 || leaveDaysCount !in 1..leaveBudget) continue

                    val month = start.monthValue
                    val matchesSeason = when (seasonFilter.lowercase()) {
                        "spring" -> month in 3..5
                        "summer" -> month in 6..8
                        "autumn" -> month in 9..11
                        else -> true
                    }
                    if (!matchesSeason) continue

                    processedIntervals.add(intervalKey)

                    opportunities.add(
                        Opportunity(
                            id = "opp_${start}_${end}_$leaveDaysCount",
                            totalDays = windowDays,
                            leaveDays = leaveDaysCount,
                            publicHolidaysCount = holidayCount,
                            startDate = start,
                            endDate = end,
                            days = dayItems
                        )
                    )
                }
            }
        }

        return paretoFront(opportunities).sortedByDescending { it.efficiency }
    }

    /**
     * Drops a break when an overlapping break is at least as good on both axes (no more leave,
     * no fewer days off) and strictly better on one — or identical but starting earlier.
     * What remains is, per holiday cluster, the best break for each leave-day count, so
     * "fewest leave days" and "longest break" sorting both have real choices to order.
     */
    private fun paretoFront(candidates: List<Opportunity>): List<Opportunity> =
        candidates.filter { a ->
            candidates.none { b ->
                b !== a && overlaps(a, b) && b.leaveDays <= a.leaveDays && b.totalDays >= a.totalDays &&
                    (b.leaveDays < a.leaveDays || b.totalDays > a.totalDays || b.startDate < a.startDate)
            }
        }

    private fun overlaps(a: Opportunity, b: Opportunity): Boolean =
        !a.startDate.isAfter(b.endDate) && !b.startDate.isAfter(a.endDate)

    private fun isWeekend(date: LocalDate): Boolean {
        return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }
}
