package com.taytek.basehw.domain.model

data class CommunityPost(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorSelectedAvatarId: Int = 1,
    val authorCustomAvatarUrl: String? = null,
    val carModelName: String = "",
    val carBrand: String = "",
    val carYear: Int? = null,
    val carSeries: String? = null,
    val carImageUrl: String = "",
    val caption: String = "",
    val carFeature: String? = null,
    val authorIsAdmin: Boolean = false,
    val authorIsMod: Boolean = false,
    val authorBadge: BadgeType = BadgeType.ROOKIE,
    val likeCount: Int = 0, // @Deprecated: Use reactionCounts instead (backward compat)
    val commentCount: Int = 0,
    val createdAt: Long = 0L,
    val isLikedByMe: Boolean = false, // @Deprecated: Use myReaction instead (backward compat)
    val isActive: Boolean = true,
    // ── Emoji Reactions ──
    val reactionCounts: Map<String, Int> = emptyMap(), // emoji → count, e.g. {"👍":5, "❤️":3}
    val myReaction: String? = null, // null = no reaction, "👍", "❤️", etc.
    val carImageUrls: List<String> = emptyList()
) {
    /** Toplam reaksiyon sayısı */
    val totalReactionCount: Int get() = reactionCounts.values.sum()

    /** Reaksiyon var mı? */
    val hasReactions: Boolean get() = reactionCounts.isNotEmpty()
}
