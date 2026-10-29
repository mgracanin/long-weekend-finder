package com.longweekend.optimizer.repository

import com.longweekend.optimizer.model.PublicHoliday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Built-in calendars used when the Nager.Date API is unreachable.
 * Movable feasts are computed per year, so the fallback stays correct for any year.
 */
object OfflineHolidayCalendar {

    fun holidays(countryCode: String, year: Int): List<PublicHoliday> {
        val code = countryCode.uppercase()
        val easter = easterSunday(year)
        fun fixed(month: Int, day: Int) = LocalDate.of(year, month, day)
        fun h(date: LocalDate, localName: String, name: String) =
            PublicHoliday(date.toString(), localName, name, code)

        return when (code) {
            "HR" -> listOf(
                h(fixed(1, 1), "Nova Godina", "New Year's Day"),
                h(fixed(1, 6), "Bogojavljenje", "Epiphany"),
                h(easter, "Uskrs", "Easter Sunday"),
                h(easter.plusDays(1), "Uskrsni ponedjeljak", "Easter Monday"),
                h(fixed(5, 1), "Praznik rada", "Labour Day"),
                h(fixed(5, 30), "Dan državnosti", "Statehood Day"),
                h(easter.plusDays(60), "Tijelovo", "Corpus Christi"),
                h(fixed(6, 22), "Dan antifašističke borbe", "Anti-Fascist Struggle Day"),
                h(fixed(8, 5), "Dan pobjede", "Victory Day"),
                h(fixed(8, 15), "Velika Gospa", "Assumption Day"),
                h(fixed(11, 1), "Dan svih svetih", "All Saints' Day"),
                h(fixed(11, 18), "Dan sjećanja", "Remembrance Day"),
                h(fixed(12, 25), "Božić", "Christmas Day"),
                h(fixed(12, 26), "Sveti Stjepan", "St. Stephen's Day")
            )
            "GB" -> listOf(
                h(fixed(1, 1), "New Year's Day", "New Year's Day"),
                h(easter.minusDays(2), "Good Friday", "Good Friday"),
                h(easter.plusDays(1), "Easter Monday", "Easter Monday"),
                h(firstMonday(year, 5), "Early May Bank Holiday", "Early May Bank Holiday"),
                h(lastMonday(year, 5), "Spring Bank Holiday", "Spring Bank Holiday"),
                h(lastMonday(year, 8), "Summer Bank Holiday", "Summer Bank Holiday"),
                h(fixed(12, 25), "Christmas Day", "Christmas Day"),
                h(fixed(12, 26), "Boxing Day", "Boxing Day")
            )
            "DE" -> listOf(
                h(fixed(1, 1), "Neujahr", "New Year's Day"),
                h(easter.minusDays(2), "Karfreitag", "Good Friday"),
                h(easter.plusDays(1), "Ostermontag", "Easter Monday"),
                h(fixed(5, 1), "Tag der Arbeit", "Labour Day"),
                h(easter.plusDays(39), "Christi Himmelfahrt", "Ascension Day"),
                h(easter.plusDays(50), "Pfingstmontag", "Whit Monday"),
                h(fixed(10, 3), "Tag der Deutschen Einheit", "German Unity Day"),
                h(fixed(12, 25), "1. Weihnachtstag", "Christmas Day"),
                h(fixed(12, 26), "2. Weihnachtstag", "St. Stephen's Day")
            )
            "FR" -> listOf(
                h(fixed(1, 1), "Jour de l'an", "New Year's Day"),
                h(easter.plusDays(1), "Lundi de Pâques", "Easter Monday"),
                h(fixed(5, 1), "Fête du Travail", "Labour Day"),
                h(fixed(5, 8), "Victoire 1945", "Victory in Europe Day"),
                h(easter.plusDays(39), "Ascension", "Ascension Day"),
                h(easter.plusDays(50), "Lundi de Pentecôte", "Whit Monday"),
                h(fixed(7, 14), "Fête nationale", "Bastille Day"),
                h(fixed(8, 15), "Assomption", "Assumption Day"),
                h(fixed(11, 1), "Toussaint", "All Saints' Day"),
                h(fixed(11, 11), "Armistice 1918", "Armistice Day"),
                h(fixed(12, 25), "Noël", "Christmas Day")
            )
            "IT" -> listOf(
                h(fixed(1, 1), "Capodanno", "New Year's Day"),
                h(fixed(1, 6), "Epifania", "Epiphany"),
                h(easter.plusDays(1), "Lunedì dell'Angelo", "Easter Monday"),
                h(fixed(4, 25), "Festa della Liberazione", "Liberation Day"),
                h(fixed(5, 1), "Festa del Lavoro", "Labour Day"),
                h(fixed(6, 2), "Festa della Repubblica", "Republic Day"),
                h(fixed(8, 15), "Ferragosto", "Assumption Day"),
                h(fixed(11, 1), "Ognissanti", "All Saints' Day"),
                h(fixed(12, 8), "Immacolata Concezione", "Immaculate Conception"),
                h(fixed(12, 25), "Natale", "Christmas Day"),
                h(fixed(12, 26), "Santo Stefano", "St. Stephen's Day")
            )
            "ES" -> listOf(
                h(fixed(1, 1), "Año Nuevo", "New Year's Day"),
                h(fixed(1, 6), "Epifanía del Señor", "Epiphany"),
                h(easter.minusDays(2), "Viernes Santo", "Good Friday"),
                h(fixed(5, 1), "Fiesta del Trabajo", "Labour Day"),
                h(fixed(8, 15), "Asunción de la Virgen", "Assumption Day"),
                h(fixed(10, 12), "Fiesta Nacional de España", "National Day"),
                h(fixed(11, 1), "Todos los Santos", "All Saints' Day"),
                h(fixed(12, 6), "Día de la Constitución", "Constitution Day"),
                h(fixed(12, 8), "Inmaculada Concepción", "Immaculate Conception"),
                h(fixed(12, 25), "Navidad", "Christmas Day")
            )
            "US" -> listOf(
                h(fixed(1, 1), "New Year's Day", "New Year's Day"),
                h(nthWeekday(year, 1, 3, DayOfWeek.MONDAY), "Martin Luther King, Jr. Day", "Martin Luther King, Jr. Day"),
                h(nthWeekday(year, 2, 3, DayOfWeek.MONDAY), "Presidents Day", "Presidents Day"),
                h(lastMonday(year, 5), "Memorial Day", "Memorial Day"),
                h(fixed(6, 19), "Juneteenth", "Juneteenth"),
                h(fixed(7, 4), "Independence Day", "Independence Day"),
                h(nthWeekday(year, 9, 1, DayOfWeek.MONDAY), "Labor Day", "Labor Day"),
                h(nthWeekday(year, 10, 2, DayOfWeek.MONDAY), "Columbus Day", "Columbus Day"),
                h(fixed(11, 11), "Veterans Day", "Veterans Day"),
                h(nthWeekday(year, 11, 4, DayOfWeek.THURSDAY), "Thanksgiving Day", "Thanksgiving Day"),
                h(fixed(12, 25), "Christmas Day", "Christmas Day")
            )
            "CA" -> listOf(
                h(fixed(1, 1), "New Year's Day", "New Year's Day"),
                h(easter.minusDays(2), "Good Friday", "Good Friday"),
                // Victoria Day: the Monday before 25 May
                h(fixed(5, 24).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), "Victoria Day", "Victoria Day"),
                h(fixed(7, 1), "Canada Day", "Canada Day"),
                h(nthWeekday(year, 9, 1, DayOfWeek.MONDAY), "Labour Day", "Labour Day"),
                h(nthWeekday(year, 10, 2, DayOfWeek.MONDAY), "Thanksgiving", "Thanksgiving"),
                h(fixed(12, 25), "Christmas Day", "Christmas Day"),
                h(fixed(12, 26), "Boxing Day", "Boxing Day")
            )
            "AU" -> listOf(
                h(fixed(1, 1), "New Year's Day", "New Year's Day"),
                h(fixed(1, 26), "Australia Day", "Australia Day"),
                h(easter.minusDays(2), "Good Friday", "Good Friday"),
                h(easter.plusDays(1), "Easter Monday", "Easter Monday"),
                h(fixed(4, 25), "Anzac Day", "Anzac Day"),
                h(fixed(12, 25), "Christmas Day", "Christmas Day"),
                h(fixed(12, 26), "Boxing Day", "Boxing Day")
            )
            "IE" -> listOf(
                h(fixed(1, 1), "New Year's Day", "New Year's Day"),
                // St Brigid's Day: 1 Feb when it is a Friday, otherwise the first Monday of February
                h(if (fixed(2, 1).dayOfWeek == DayOfWeek.FRIDAY) fixed(2, 1) else firstMonday(year, 2), "St. Brigid's Day", "St. Brigid's Day"),
                h(fixed(3, 17), "St. Patrick's Day", "St. Patrick's Day"),
                h(easter.plusDays(1), "Easter Monday", "Easter Monday"),
                h(firstMonday(year, 5), "May Day", "May Day"),
                h(firstMonday(year, 6), "June Holiday", "June Holiday"),
                h(firstMonday(year, 8), "August Holiday", "August Holiday"),
                h(lastMonday(year, 10), "October Holiday", "October Holiday"),
                h(fixed(12, 25), "Christmas Day", "Christmas Day"),
                h(fixed(12, 26), "St. Stephen's Day", "St. Stephen's Day")
            )
            // No built-in calendar: guessing (e.g. Christmas in Japan) would show made-up holidays
            else -> emptyList()
        }
    }

    private fun nthWeekday(year: Int, month: Int, n: Int, day: DayOfWeek): LocalDate =
        LocalDate.of(year, month, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, day))

    /** Western (Gregorian) Easter Sunday — anonymous Gregorian algorithm. */
    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    private fun firstMonday(year: Int, month: Int): LocalDate =
        LocalDate.of(year, month, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))

    private fun lastMonday(year: Int, month: Int): LocalDate =
        LocalDate.of(year, month, 1).with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))
}
