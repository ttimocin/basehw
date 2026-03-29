package com.taytek.basehw.domain.model

data class CommunityComment(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
