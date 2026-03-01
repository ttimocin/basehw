package com.taytek.basehw.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class UserCarWithMaster(
    @Embedded val car: UserCarEntity,
    @Relation(
        parentColumn = "masterDataId",
        entityColumn = "id"
    )
    val master: MasterDataEntity?
)
