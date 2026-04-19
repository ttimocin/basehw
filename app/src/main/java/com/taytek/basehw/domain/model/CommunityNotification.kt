package com.taytek.basehw.domain.model

import java.time.Instant

data class CommunityNotification(
    val id: String,
    val recipientUid: String,
    val senderUid: String?,
    val type: String, // "FOLLOW" vb.
    val message: String,
    val isRead: Boolean,
    val createdAt: Instant,
    
    // Uygulama içi göstermek için (Supabase'den join'lenecek veya lokalden alınacak)
    val senderQueryData: SenderQueryData? = null
)

data class SenderQueryData(
    val username: String? = null,
    val avatarUrl: String? = null,
    val selectedAvatarId: Int? = null
)
