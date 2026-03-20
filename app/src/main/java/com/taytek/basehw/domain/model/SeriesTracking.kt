package com.taytek.basehw.domain.model

data class SeriesTracking(
    val brand: Brand,
    val seriesName: String,
    val items: List<SeriesTrackingItem>
)

data class SeriesTrackingItem(
    val masterData: MasterData,
    val isInCollection: Boolean,
    val isInWishlist: Boolean
)
