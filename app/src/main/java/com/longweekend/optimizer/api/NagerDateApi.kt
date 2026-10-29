package com.longweekend.optimizer.api

import com.longweekend.optimizer.model.PublicHoliday
import retrofit2.http.GET
import retrofit2.http.Path

interface NagerDateApi {

    @GET("api/v3/PublicHolidays/{year}/{countryCode}")
    suspend fun getPublicHolidays(
        @Path("year") year: Int,
        @Path("countryCode") countryCode: String
    ): List<PublicHoliday>
}
