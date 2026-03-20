package com.taytek.basehw.domain.repository

import com.taytek.basehw.domain.model.CurrencyRates
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {
    fun getRates(): Flow<CurrencyRates?>
    suspend fun refreshRates(): Result<Unit>
}
