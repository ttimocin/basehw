package com.taytek.basehw.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variant_hunt_groups",
    indices = [
        Index(value = ["brand"]),
        Index(value = ["isActive"]),
        Index(value = ["seedMasterDataId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = MasterDataEntity::class,
            parentColumns = ["id"],
            childColumns = ["seedMasterDataId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class VariantHuntGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val brand: String,
    val title: String,
    val seedMasterDataId: Long,
    val seedUserCarId: Long?,
    /** One keyword per line (internal storage). */
    val keywordsDelimited: String,
    val createdAtMillis: Long,
    val completedAtMillis: Long?,
    val isActive: Boolean = true
)
