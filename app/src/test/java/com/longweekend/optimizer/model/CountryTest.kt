package com.longweekend.optimizer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CountryTest {

    @Test
    fun allApiCountries_areBundledOnce() {
        assertEquals(204, Country.all.size)
        assertEquals(Country.all.size, Country.all.map { it.countryCode }.toSet().size)
        assertTrue(Country.all.all { it.countryCode.matches(Regex("[A-Z]{2}")) })
    }

    @Test
    fun flagEmoji_isBuiltFromRegionalIndicators() {
        assertEquals("🇭🇷", Country.forCode("HR")!!.flagEmoji) // 🇭🇷
        assertEquals("🇬🇧", Country.forCode("GB")!!.flagEmoji) // 🇬🇧
    }

    @Test
    fun displayName_followsTheAppLanguage() {
        val croatia = Country.forCode("hr")!!
        assertEquals("Croatia", croatia.displayName(Locale.ENGLISH))
        assertEquals("Hrvatska", croatia.displayName(Locale.forLanguageTag("hr")))
        assertEquals("Njemačka", Country.forCode("DE")!!.displayName(Locale.forLanguageTag("hr")))
    }

    @Test
    fun unknownCode_isNotSupported() {
        assertNull(Country.forCode("ZZ"))
        assertNull(Country.forCode(null))
    }
}
