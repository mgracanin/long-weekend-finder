package com.longweekend.optimizer.model

/** Leave can be planned for this year and the next few; the year picker and offline data cover the same span. */
object Planning {
    const val YEARS = 5

    fun years(currentYear: Int): IntRange = currentYear until currentYear + YEARS

    /** The plannable years plus the one after, whose holidays breaks running past 31 December need. */
    fun holidayYears(currentYear: Int): IntRange = currentYear..currentYear + YEARS
}
