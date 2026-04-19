package com.taytek.basehw.domain.model

import kotlin.math.min

data class RankCarInput(
    val brand: Brand?,
    val feature: String?,
    val condition: VehicleCondition,
    val isPremium: Boolean,
    val isCustom: Boolean,
    val quantity: Int = 1
)

data class CollectionScoreInputs(
    val totalCars: Int,
    val totalScore: Double,
    val thCount: Int,
    val sthCount: Int,
    val chaseCount: Int,
    val distinctBrands: Int
)

object CollectionRankCalculator {
    fun calculate(cars: List<RankCarInput>): CollectionScoreInputs {
        if (cars.isEmpty()) {
            return CollectionScoreInputs(
                totalCars = 0,
                totalScore = 0.0,
                thCount = 0,
                sthCount = 0,
                chaseCount = 0,
                distinctBrands = 0
            )
        }

        var totalCars = 0
        var baseScore = 0.0
        var boxedCount = 0
        var mintCount = 0
        var premiumCount = 0
        var customCount = 0
        var thCount = 0
        var sthCount = 0
        var chaseCount = 0
        val brands = mutableSetOf<Brand>()

        cars.forEach { car ->
            val quantity = car.quantity.coerceAtLeast(1)
            totalCars += quantity
            car.brand?.let { brands += it }

            baseScore += brandMultiplier(car.brand) * quantity
            if (car.condition != VehicleCondition.LOOSE) boxedCount += quantity
            if (car.condition == VehicleCondition.MINT) mintCount += quantity
            if (car.isPremium) premiumCount += quantity
            if (car.isCustom) customCount += quantity

            val feature = car.feature?.trim()?.lowercase().orEmpty()
            when {
                feature.contains("sth") -> sthCount += quantity
                feature.contains("chase") -> chaseCount += quantity
                feature == "th" || feature.contains(" treasure hunt") || feature.startsWith("th ") -> thCount += quantity
            }
        }

        val rarityBonus = min(thCount * 4.0, 80.0) + min(sthCount * 12.0, 60.0) + min(chaseCount * 10.0, 50.0)
        val boxedBonus = boxedCount * 2.0
        val mintOverlap = min(mintCount, boxedCount)
        val mintOnly = mintCount - mintOverlap
        val mintBonus = mintOnly * 1.5 + mintOverlap * 0.75
        val premiumBonus = premiumCount * 1.0
        val customBonus = customCount * 1.0
        val brandBonus = min(brands.size * 2.0, 12.0)

        return CollectionScoreInputs(
            totalCars = totalCars,
            totalScore = baseScore + rarityBonus + boxedBonus + mintBonus + premiumBonus + customBonus + brandBonus,
            thCount = thCount,
            sthCount = sthCount,
            chaseCount = chaseCount,
            distinctBrands = brands.size
        )
    }

    private fun brandMultiplier(brand: Brand?): Double {
        return when (brand) {
            Brand.HOT_WHEELS -> 1.0
            Brand.MATCHBOX -> 1.0
            Brand.MAJORETTE -> 1.1
            Brand.SIKU -> 1.1
            Brand.GREENLIGHT -> 1.2
            Brand.JADA -> 1.2
            Brand.MINI_GT -> 1.6
            Brand.KAIDO_HOUSE -> 2.0
            null -> 1.2
        }
    }
}
