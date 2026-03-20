package com.taytek.basehw.domain.model

import com.google.firebase.Timestamp

data class Feedback(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val subject: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
