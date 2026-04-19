package com.taytek.basehw.data.remote.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Generic DTO for asset JSON files covering both brands:
 *
 * Hot Wheels:  year, toy_num, col_num, model_name, series, series_num, image_url
 * Matchbox:    brand, year, man_num, toy_num, model_name, series, image_url
 */
@androidx.annotation.Keep
@Serializable
data class BrandCarDto(
    @SerializedName("brand") @SerialName("brand") val brand: String = "",
    @SerializedName("year") @SerialName("year") val year: String? = null,
    @SerializedName("years") @SerialName("years") val years: String? = null,
    @SerializedName("list_year") @SerialName("list_year") val listYear: Int? = null,
    @SerializedName(value = "toy_num", alternate = ["toy_no", "toy_number"]) @SerialName("toy_num") val toyNum: String = "",
    @SerializedName("col_num") @SerialName("col_num") val colNum: String = "",
    @SerializedName("serie_nr") @SerialName("serie_nr") val serieNr: String = "",
    @SerializedName("man_num") @SerialName("man_num") val manNum: String = "",
    @SerializedName("code") @SerialName("code") val code: String = "",
    @SerializedName(value = "model_name", alternate = ["title", "model"]) @SerialName("model_name") val modelName: String = "",
    @SerializedName(value = "series", alternate = ["series_name", "series_names"]) @SerialName("series") val series: String = "",
    @SerializedName("series_type") @SerialName("series_type") val seriesType: String = "",
    @SerializedName("set_name") @SerialName("set_name") val setName: String = "",
    @SerializedName("drive_type") @SerialName("drive_type") val driveType: String = "",
    @SerializedName(value = "body_color", alternate = ["color"]) @SerialName("body_color") val bodyColor: String = "",
    @SerializedName("original_brand") @SerialName("original_brand") val originalBrand: String = "",
    @SerializedName("series_num") @SerialName("series_num") val seriesNum: String = "",
    @SerializedName("page_source") @SerialName("page_source") val pageSource: String = "",
    @SerializedName("image_url") @SerialName("image_url") val imageUrl: String = "",
    @SerializedName("scale") @SerialName("scale") val scale: String = "",
    @SerializedName(value = "case", alternate = ["case_num", "mix"]) @SerialName("case") val case: String = "",
    @SerializedName("feature") @SerialName("feature") val feature: String? = null
)

// Keep old name as a typealias so existing references still compile
typealias HotWheelsCarDto = BrandCarDto
