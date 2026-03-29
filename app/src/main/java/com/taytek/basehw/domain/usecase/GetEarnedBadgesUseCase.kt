package com.taytek.basehw.domain.usecase

import com.taytek.basehw.domain.model.BadgeType
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.repository.CustomCollectionRepository
import com.taytek.basehw.domain.repository.UserCarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetEarnedBadgesUseCase @Inject constructor(
    private val userCarRepository: UserCarRepository,
    private val customCollectionRepository: CustomCollectionRepository,
    private val currencyRepository: com.taytek.basehw.domain.repository.CurrencyRepository,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager
) {
    operator fun invoke(): Flow<List<BadgeType>> = combine(
        combine(
            userCarRepository.getTotalCarsCount(),
            userCarRepository.getBrandCounts(),
            userCarRepository.getHwTierStats(),
            userCarRepository.getBoxStatusCounts(),
            userCarRepository.getCustomStats()
        ) { totalCars, brandStats, hwTier, boxStats, customStats ->
            BadgeInputs(
                totalCars = totalCars,
                premiumCount = hwTier.premiumCount,
                matchboxCount = brandStats.firstOrNull { it.brand == Brand.MATCHBOX }?.count ?: 0,
                brandsRepresented = brandStats.count { it.count > 0 },
                totalBoxed = boxStats.firstOrNull { !it.isOpened }?.count ?: 0,
                customCount = customStats.customCount
            )
        },
        combine(
            userCarRepository.getTotalPurchasePrice(),
            userCarRepository.getTotalEstimatedValue(),
            currencyRepository.getRates(),
            appSettingsManager.currencyFlow
        ) { purchase, estimated, rates, currentCurrency ->
            // Convert everything to USD for consistent badge evaluation
            val usdRate = if (currentCurrency == "USD") 1.0 else rates?.rates?.get("USD") ?: 1.0
            val eurRate = if (currentCurrency == "EUR") 1.0 else rates?.rates?.get(currentCurrency) ?: 1.0
            
            // Current value in EUR = BaseValue / eurRate
            // Current value in USD = (BaseValue / eurRate) * usdRate
            val conversionFactor = usdRate / (if (eurRate == 0.0) 1.0 else eurRate)
            
            FinancialInputs(
                totalPurchaseUsd = purchase * conversionFactor,
                totalEstimatedUsd = estimated * conversionFactor
            )
        }
    ) { inputs, finance ->
        val earned = mutableListOf<BadgeType>()

        // Collection Size
        if (inputs.totalCars >= 1)   earned += BadgeType.FIRST_CAR
        if (inputs.totalCars >= 10)  earned += BadgeType.TEN_CARS
        if (inputs.totalCars >= 50)  earned += BadgeType.FIFTY_CARS
        if (inputs.totalCars >= 100) earned += BadgeType.HUNDRED_CARS

        // Premium
        if (inputs.premiumCount >= 5) earned += BadgeType.PREMIUM_HUNTER

        // Brand specific
        if (inputs.matchboxCount >= 10) earned += BadgeType.MATCHBOX_FAN
        if (inputs.brandsRepresented >= 3) earned += BadgeType.MULTI_BRAND

        // Financial (USD based)
        if (finance.totalEstimatedUsd >= 100.0) earned += BadgeType.SILVER_COLLECTOR
        if (finance.totalEstimatedUsd >= 1000.0) earned += BadgeType.GOLDEN_COLLECTOR

        // Custom
        if (inputs.customCount >= 10) earned += BadgeType.CUSTOM_MAKER

        // Box status
        val ratio = if (inputs.totalCars > 0) inputs.totalBoxed.toDouble() / inputs.totalCars else 0.0
        if (inputs.totalCars >= 5 && ratio >= 0.8) earned += BadgeType.MOC_COLLECTOR

        earned
    }
}

private data class BadgeInputs(
    val totalCars: Int,
    val premiumCount: Int,
    val matchboxCount: Int,
    val brandsRepresented: Int,
    val totalBoxed: Int,
    val customCount: Int
)

private data class FinancialInputs(
    val totalPurchaseUsd: Double,
    val totalEstimatedUsd: Double
)
