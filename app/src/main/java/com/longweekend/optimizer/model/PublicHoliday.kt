package com.longweekend.optimizer.model

import com.google.gson.annotations.SerializedName

data class PublicHoliday(
    val date: String,
    val localName: String,
    val name: String,
    val countryCode: String,
    val fixed: Boolean = false,
    val global: Boolean = true,
    val counties: List<String>? = null,
    val launchYear: Int? = null,
    val types: List<String>? = null
)
