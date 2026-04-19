package com.taytek.basehw.domain.model

data class BoxStatusStats(
    val condition: String,
    val count: Int
)

data class BrandStats(
    val brand: Brand,
    val count: Int
)

/** Hot Wheels koleksiyonundaki Regular ve Premium ayrımı */
data class HwTierStats(
    val regularCount: Int,
    val premiumCount: Int
)
