package com.taytek.basehw.data.remote.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseFeedbackInsertRow(
    @SerialName("firebase_uid") val firebaseUid: String,
    @SerialName("username") val username: String,
    @SerialName("subject") val subject: String,
    @SerialName("message") val message: String
)
