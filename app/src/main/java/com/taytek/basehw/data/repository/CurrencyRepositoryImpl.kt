package com.taytek.basehw.data.repository

import com.taytek.basehw.data.remote.api.CurrencyApiService
import com.taytek.basehw.domain.model.CurrencyRates
import com.taytek.basehw.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val api: CurrencyApiService
) : CurrencyRepository {

    private val _rates = MutableStateFlow<CurrencyRates?>(null)

    override fun getRates(): Flow<CurrencyRates?> = _rates.asStateFlow()

    override suspend fun refreshRates(): Result<Unit> {
        return try {
            val response = api.getLatestRates(base = "EUR", symbols = "TRY,USD,GBP,JPY,CHF,CAD,AUD,CNY")
            val domainRates = CurrencyRates(
                base = response.base,
                date = response.date,
                rates = response.rates
            )
            _rates.value = domainRates
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
