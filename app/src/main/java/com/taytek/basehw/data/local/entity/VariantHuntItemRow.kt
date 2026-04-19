package com.taytek.basehw.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class VariantHuntItemRow(
    @Embedded val master: MasterDataEntity,
    @ColumnInfo(name = "inCollection") val inCollection: Int
)
