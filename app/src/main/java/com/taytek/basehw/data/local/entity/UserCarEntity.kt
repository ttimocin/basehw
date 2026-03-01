package com.taytek.basehw.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_cars",
    foreignKeys = [
        ForeignKey(
            entity = MasterDataEntity::class,
            parentColumns = ["id"],
            childColumns = ["masterDataId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["masterDataId"])]
)
data class UserCarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val masterDataId: Long,
    val isOpened: Boolean = false,
    val purchaseDateMillis: Long? = null,
    val personalNote: String = "",
    val storageLocation: String = "",
    val firestoreId: String = "",
    val isWishlist: Boolean = false
)
