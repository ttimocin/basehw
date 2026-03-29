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
    val masterDataId: Long?,
    val manualModelName: String? = null,
    val manualBrand: String? = null,
    val manualSeries: String? = null,
    val manualSeriesNum: String? = null,
    val manualYear: Int? = null,
    val manualScale: String? = null,
    val manualIsPremium: Boolean? = null,
    val isOpened: Boolean = false,
    val purchaseDateMillis: Long? = null,
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
    val additionalPhotosBackup: List<String> = emptyList()
)
