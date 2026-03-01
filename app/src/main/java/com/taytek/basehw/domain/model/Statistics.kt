package com.taytek.basehw.domain.model

data class BoxStatusStats(
    val isOpened: Boolean,
    val count: Int
)

data class BrandStats(
    val brand: Brand,
    val count: Int
)
