package com.taytek.basehw.domain.model

import java.util.Date

data class CustomCollection(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverPhotoUrl: String? = null,
    val firestoreId: String = "",
    val createdAt: Date = Date(),
    val cars: List<UserCar> = emptyList()
)
