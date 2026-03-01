package com.taytek.basehw.domain.model

data class MasterData(
    val id: Long = 0,
    val brand: Brand,
    val modelName: String,
    val series: String = "",
    val seriesNum: String = "",   // e.g. "1/4"
    val year: Int? = null,
    val color: String = "",
    val imageUrl: String = "",
    val scale: String = "1:64",
    val toyNum: String = "",      // e.g. "26004"
    val colNum: String = ""       // e.g. "001"
)
