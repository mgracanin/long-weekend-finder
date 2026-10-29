package com.longweekend.optimizer.model

import java.time.LocalDate

enum class DayType {
    WEEKEND,
    PUBLIC_HOLIDAY,
    ANNUAL_LEAVE,
    WORKDAY
}

/** One day of a break. Display text (weekday, badge, holiday name) is formatted by the UI. */
data class DayStripItem(
    val date: LocalDate,
    val type: DayType,
    /** English holiday name, used for matching and as the default label. */
    val holidayName: String? = null,
    /** Holiday name in the country's own language (e.g. "Božić"); null for older saved plans. */
    val holidayLocalName: String? = null
)
