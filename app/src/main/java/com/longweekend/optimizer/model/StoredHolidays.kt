package com.longweekend.optimizer.model

/** One country's holidays for one year as downloaded from the API, and when ([fetchedAt], epoch ms). */
data class StoredHolidays(
    val holidays: List<PublicHoliday>?,
    val fetchedAt: Long
)
