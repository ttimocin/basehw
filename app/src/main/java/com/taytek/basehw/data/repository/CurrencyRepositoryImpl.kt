package com.taytek.basehw.data.repository

import android.content.Context
import com.google.gson.Gson
import com.taytek.basehw.data.remote.api.CurrencyApiService
import com.taytek.basehw.domain.model.CurrencyRates
import com.taytek.basehw.domain.repository.CurrencyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val api: CurrencyApiService,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : CurrencyRepository {

    private val prefs = context.getSharedPreferences("currency_prefs", android.content.Context.MODE_PRIVATE)
    private val _rates = MutableStateFlow<CurrencyRates?>(loadCachedRates())

    override fun getRates(): Flow<CurrencyRates?> = _rates.asStateFlow()

    private fun loadCachedRates(): CurrencyRates? {
        val json = prefs.getString("cached_rates", null) ?: return null
        return try {
            gson.fromJson(json, CurrencyRates::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveRatesToCache(rates: CurrencyRates) {
        val json = gson.toJson(rates)
        prefs.edit().putString("cached_rates", json).apply()
    }

    override suspend fun refreshRates(): Result<Unit> {
        return try {
            val response = api.getLatestRates(base = "EUR", symbols = "TRY,USD,GBP,JPY,CHF,CAD,AUD,CNY")
            val domainRates = CurrencyRates(
                base = response.base,
                date = response.date,
                rates = response.rates
            )
            _rates.value = domainRates
            saveRatesToCache(domainRates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
