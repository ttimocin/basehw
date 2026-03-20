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
    private val customCollectionRepository: CustomCollectionRepository
) {
    operator fun invoke(): Flow<List<BadgeType>> = combine(
        combine(
            userCarRepository.getTotalCarsCount(),
            userCarRepository.getBrandCounts(),
            userCarRepository.getHwTierStats(),
            userCarRepository.getBoxStatusCounts()
        ) { totalCars, brandStats, hwTier, boxStats ->
            BadgeInputs(
                totalCars = totalCars,
                premiumCount = hwTier.premiumCount,
                matchboxCount = brandStats.firstOrNull { it.brand == Brand.MATCHBOX }?.count ?: 0,
                brandsRepresented = brandStats.count { it.count > 0 },
                totalBoxed = boxStats.firstOrNull { !it.isOpened }?.count ?: 0
            )
        },
        combine(
            userCarRepository.getTotalPurchasePrice(),
            userCarRepository.getTotalEstimatedValue(),
            customCollectionRepository.getAllCollections()
        ) { purchase, estimated, collections ->
            FinancialInputs(
                totalPurchase = purchase,
                totalEstimated = estimated,
                folderCount = collections.size
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

        // Financial
        if (finance.totalEstimated >= 500.0) earned += BadgeType.INVESTOR
        if (finance.totalPurchase >= 2000.0) earned += BadgeType.BIG_SPENDER

        // Folders
        if (finance.folderCount >= 3) earned += BadgeType.CURATOR

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
    val totalBoxed: Int
)

private data class FinancialInputs(
    val totalPurchase: Double,
    val totalEstimated: Double,
    val folderCount: Int
)
