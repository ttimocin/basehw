package com.taytek.basehw.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic DTO for asset JSON files covering both brands:
 *
 * Hot Wheels:  year, toy_num, col_num, model_name, series, series_num, image_url
 * Matchbox:    brand, year, man_num, toy_num, model_name, series, image_url
 */
@androidx.annotation.Keep
data class BrandCarDto(
    @SerializedName("brand")          val brand: String = "",
    @SerializedName("year")           val year: String? = null,
    @SerializedName("years")          val years: String? = null,
    @SerializedName("list_year")      val listYear: Int? = null,
    @SerializedName(value = "toy_num", alternate = ["toy_no", "toy_number"]) val toyNum: String = "",
    @SerializedName("col_num")        val colNum: String = "",
    @SerializedName("serie_nr")       val serieNr: String = "",
    @SerializedName("man_num")        val manNum: String = "",
    @SerializedName("code")           val code: String = "",
    @SerializedName(value = "model_name", alternate = ["title", "model"]) val modelName: String = "",
    @SerializedName(value = "series", alternate = ["series_name", "series_names"]) val series: String = "",
    @SerializedName("series_type")    val seriesType: String = "",
    @SerializedName("set_name")       val setName: String = "",
    @SerializedName("drive_type")     val driveType: String = "",
    @SerializedName(value = "body_color", alternate = ["color"]) val bodyColor: String = "",
    @SerializedName("original_brand") val originalBrand: String = "",
    @SerializedName("series_num")     val seriesNum: String = "",
    @SerializedName("page_source")    val pageSource: String = "",
    @SerializedName("image_url")      val imageUrl: String = "",
    @SerializedName("scale")          val scale: String = "",
    @SerializedName(value = "case", alternate = ["case_num", "mix"]) val case: String = "",
    @SerializedName("feature")        val feature: String? = null
)

// Keep old name as a typealias so existing references still compile
typealias HotWheelsCarDto = BrandCarDto
