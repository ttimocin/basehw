package com.taytek.basehw.domain.model

data class VariantHuntGroupSummary(
    val id: Long,
    val brandCode: String,
    val title: String,
    val keywords: List<String>,
    val createdAtMillis: Long
)

data class VariantHuntMasterRow(
    val masterDataId: Long,
    val modelName: String,
    val year: Int?,
    val series: String,
    val seriesNum: String,
    val toyNum: String,
    val imageUrl: String,
    val inCollection: Boolean
)
