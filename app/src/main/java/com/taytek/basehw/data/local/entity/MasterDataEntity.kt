package com.taytek.basehw.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "master_data",
    indices = [
        Index(value = ["brand", "modelName"]),
        Index(value = ["brand"]),
        Index(value = ["dataSource", "year", "modelName"]),
        Index(value = ["feature"]),
        Index(value = ["brand", "toyNum"]),
        Index(value = ["brand", "modelName", "year"])
    ]
)
data class MasterDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val brand: String,
    val modelName: String,
    val series: String = "",
    val seriesNum: String = "",
    val year: Int? = null,
    val color: String = "",
    val imageUrl: String = "",
    val scale: String = "1:64",
    val toyNum: String = "",
    val colNum: String = "",
    val isPremium: Boolean = false,
    val dataSource: String = "",
    val caseNum: String = "",
    val feature: String? = null,
    val category: String? = null
)

