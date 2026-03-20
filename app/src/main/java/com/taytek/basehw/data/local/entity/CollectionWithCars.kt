package com.taytek.basehw.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class CollectionWithCars(
    @Embedded val collection: CustomCollectionEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CollectionCarCrossRef::class,
            parentColumn = "collectionId",
            entityColumn = "userCarId"
        ),
        entity = UserCarEntity::class
    )
    val cars: List<UserCarWithMaster>
)
