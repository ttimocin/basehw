package com.taytek.basehw.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic DTO for asset JSON files covering both brands:
 *
 * Hot Wheels:  year, toy_num, col_num, model_name, series, series_num, image_url
 * Matchbox:    brand, year, man_num, toy_num, model_name, series, image_url
 */
data class BrandCarDto(
    @SerializedName("brand")          val brand: String = "",
    @SerializedName("year")           val year: String? = null,
    @SerializedName("toy_num")        val toyNum: String = "",
    @SerializedName("col_num")        val colNum: String = "",
    @SerializedName("man_num")        val manNum: String = "",
    @SerializedName("code")           val code: String = "",
    @SerializedName("model_name")     val modelName: String = "",
    @SerializedName("series")         val series: String = "",
    @SerializedName("drive_type")     val driveType: String = "",
    @SerializedName("original_brand") val originalBrand: String = "",
    @SerializedName("series_num")     val seriesNum: String = "",
    @SerializedName("image_url")      val imageUrl: String = ""
)

// Keep old name as a typealias so existing references still compile
typealias HotWheelsCarDto = BrandCarDto
