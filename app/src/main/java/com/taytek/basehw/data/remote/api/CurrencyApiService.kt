package com.taytek.basehw.data.remote.api

import com.taytek.basehw.data.remote.dto.CurrencyResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApiService {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") base: String = "EUR",
        @Query("to") symbols: String = "TRY,USD,GBP,JPY,CHF,CAD,AUD,CNY"
    ): CurrencyResponse
}
