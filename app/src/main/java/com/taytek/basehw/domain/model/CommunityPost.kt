package com.taytek.basehw.domain.model

data class CommunityPost(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val carModelName: String = "",
    val carBrand: String = "",
    val carYear: Int? = null,
    val carSeries: String? = null,
    val carImageUrl: String = "",
    val caption: String = "",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: Long = 0L,
    val isLikedByMe: Boolean = false,
    val isActive: Boolean = true
)
