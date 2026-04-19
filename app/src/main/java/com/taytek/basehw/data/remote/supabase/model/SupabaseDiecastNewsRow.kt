package com.taytek.basehw.data.remote.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseDiecastNewsRow(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    @SerialName("title_tr") val titleTr: String? = null,
    @SerialName("body_tr") val bodyTr: String? = null,
    @SerialName("title_en") val titleEn: String? = null,
    @SerialName("body_en") val bodyEn: String? = null,
    @SerialName("title_de") val titleDe: String? = null,
    @SerialName("body_de") val bodyDe: String? = null,
    @SerialName("title_es") val titleEs: String? = null,
    @SerialName("body_es") val bodyEs: String? = null,
    @SerialName("title_fr") val titleFr: String? = null,
    @SerialName("body_fr") val bodyFr: String? = null,
    @SerialName("title_pt") val titlePt: String? = null,
    @SerialName("body_pt") val bodyPt: String? = null,
    @SerialName("title_ru") val titleRu: String? = null,
    @SerialName("body_ru") val bodyRu: String? = null,
    @SerialName("title_uk") val titleUk: String? = null,
    @SerialName("body_uk") val bodyUk: String? = null,
    @SerialName("title_ar") val titleAr: String? = null,
    @SerialName("body_ar") val bodyAr: String? = null,
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("is_published") val isPublished: Boolean = true
)
