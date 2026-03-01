package com.taytek.basehw.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FandomQueryResponse(
    @SerializedName("batchcomplete") val batchComplete: String? = null,
    @SerializedName("continue") val continueData: ContinueData? = null,
    @SerializedName("query") val query: QueryData? = null
)

data class ContinueData(
    @SerializedName("gcmcontinue") val gcmContinue: String? = null,
    @SerializedName("cmcontinue") val cmContinue: String? = null,
    @SerializedName("continue") val continueProp: String? = null
)

data class QueryData(
    @SerializedName("pages") val pages: Map<String, PageData>? = null,
    @SerializedName("categorymembers") val categoryMembers: List<CategoryMember>? = null
)

data class PageData(
    @SerializedName("pageid") val pageId: Int = 0,
    @SerializedName("ns") val ns: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("thumbnail") val thumbnail: Thumbnail? = null,
    @SerializedName("pageimage") val pageImage: String? = null
)

data class Thumbnail(
    @SerializedName("source") val source: String = "",
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0
) {
    /**
     * Converts a thumbnail URL to high-resolution by removing width constraint.
     * e.g: .../220px-image.jpg → .../image.jpg
     */
    fun toHighResUrl(): String {
        return source
            .replace(Regex("/\\d+px-"), "/")
            .replace("thumb/", "")
    }
}

data class CategoryMember(
    @SerializedName("pageid") val pageId: Int = 0,
    @SerializedName("ns") val ns: Int = 0,
    @SerializedName("title") val title: String = ""
)
