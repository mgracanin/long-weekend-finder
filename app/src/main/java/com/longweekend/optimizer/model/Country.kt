package com.longweekend.optimizer.model

import java.util.Locale

data class Country(
    val countryCode: String,
    /** English name from the holiday API; the UI prefers [displayName]. */
    val name: String
) {
    /** Flag emoji built from the two regional-indicator letters of the ISO code. */
    val flagEmoji: String
        get() = countryCode.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("")

    /** Country name in [locale]'s language, e.g. "Hrvatska" for HR in Croatian. */
    fun displayName(locale: Locale): String =
        Locale.Builder().setRegion(countryCode).build().getDisplayCountry(locale).takeIf { it.isNotBlank() && it != countryCode } ?: name

    companion object {
        val all: List<Country> = SupportedCountries.all.map { (code, name) -> Country(code, name) }

        fun forCode(code: String?): Country? = all.firstOrNull { it.countryCode.equals(code, ignoreCase = true) }
    }
}
