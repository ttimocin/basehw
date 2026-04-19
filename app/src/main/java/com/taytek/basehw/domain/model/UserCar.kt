package com.taytek.basehw.domain.model

import java.util.Date

data class UserCar(
    val id: Long = 0,
    val masterDataId: Long?,
    val masterData: MasterData? = null,
    val manualModelName: String? = null,
    val manualBrand: Brand? = null,
    val manualYear: Int? = null,
    val manualSeries: String? = null,
    val manualSeriesNum: String? = null,
    val manualScale: String? = null,
    val manualIsPremium: Boolean? = null,
    val condition: VehicleCondition = VehicleCondition.MINT,
    val purchaseDate: Date? = null,
    val personalNote: String = "",
    val storageLocation: String = "",
    val firestoreId: String = "",
    val isWishlist: Boolean = false,
    val userPhotoUrl: String? = null,
    val backupPhotoUrl: String? = null,
    val purchasePrice: Double? = null,
    val estimatedValue: Double? = null,
    val isFavorite: Boolean = false,
    val isSeriesOnly: Boolean = false,
    val isCustom: Boolean = false,
    val quantity: Int = 1,
    val additionalPhotos: List<String> = emptyList(),
    val additionalPhotosBackup: List<String> = emptyList(),
    val hwCardType: HwCardType? = null
)
