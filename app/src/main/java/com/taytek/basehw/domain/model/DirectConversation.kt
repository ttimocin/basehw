package com.taytek.basehw.domain.model

data class DirectConversation(
    val conversationId: String,
    val peerUid: String,
    val peerDisplayName: String,
    val lastMessage: String,
    val lastMessageAt: Long,
    val lastMessageSenderUid: String,
    val isUnread: Boolean = false
)
