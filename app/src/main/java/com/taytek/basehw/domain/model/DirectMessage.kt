package com.taytek.basehw.domain.model

data class DirectMessage(
    val id: String,
    val senderUid: String,
    val receiverUid: String,
    val body: String,
    val createdAt: Long
)
