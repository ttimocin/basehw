package com.taytek.basehw.domain.model

data class User(
    val uid: String,
    val email: String,
    val username: String? = null,
    val photoUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0,
    val rulesAccepted: Boolean = false,
    val isAdmin: Boolean = false
)
