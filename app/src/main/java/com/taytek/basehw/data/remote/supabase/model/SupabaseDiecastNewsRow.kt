package com.taytek.basehw.data.remote.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseDiecastNewsRow(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("is_published") val isPublished: Boolean = true
)
