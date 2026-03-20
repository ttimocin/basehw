package com.taytek.basehw.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "collection_car_cross_ref",
    primaryKeys = ["collectionId", "userCarId"],
    foreignKeys = [
        ForeignKey(
            entity = CustomCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserCarEntity::class,
            parentColumns = ["id"],
            childColumns = ["userCarId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["userCarId"])
    ]
)
data class CollectionCarCrossRef(
    val collectionId: Long,
    val userCarId: Long
)
