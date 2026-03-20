package com.taytek.basehw.domain.model

data class User(
    val uid: String,
    val email: String,
    val username: String? = null,
    val photoUrl: String? = null
)
