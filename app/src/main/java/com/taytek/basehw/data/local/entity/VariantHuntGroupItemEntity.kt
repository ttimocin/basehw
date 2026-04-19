package com.taytek.basehw.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "variant_hunt_group_items",
    primaryKeys = ["groupId", "masterDataId"],
    foreignKeys = [
        ForeignKey(
            entity = VariantHuntGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MasterDataEntity::class,
            parentColumns = ["id"],
            childColumns = ["masterDataId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["masterDataId"])
    ]
)
data class VariantHuntGroupItemEntity(
    val groupId: Long,
    val masterDataId: Long
)
