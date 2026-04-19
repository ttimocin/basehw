package com.taytek.basehw.domain.model

data class User(
    val uid: String,
    val email: String,
    val username: String? = null,
    val googleUsernameOnboardingRequired: Boolean = false,
    val googleUsernameOnboardingCompleted: Boolean = false,
    val photoUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0,
    val rulesAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    val isAdmin: Boolean = false,
    val isMod: Boolean = false,
    val isCollectionPublic: Boolean = false,
    val isWishlistPublic: Boolean = false,
    val selectedAvatarId: Int = 1,
    val customAvatarUrl: String? = null,
    val isForumBanned: Boolean = false,
    val activeBadge: BadgeType = BadgeType.ROOKIE
)
