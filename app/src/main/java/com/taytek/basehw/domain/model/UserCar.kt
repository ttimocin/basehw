package com.taytek.basehw.domain.model

import java.util.Date

data class UserCar(
    val id: Long = 0,
    val masterDataId: Long,
    val masterData: MasterData? = null,
    val isOpened: Boolean = false,
    val purchaseDate: Date? = null,
    val personalNote: String = "",
    val storageLocation: String = "",
    val firestoreId: String = "",
    val isWishlist: Boolean = false
)
