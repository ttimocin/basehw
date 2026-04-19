package com.taytek.basehw.data.remote.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseCollectionSnapshotRow(
    @SerialName("firebase_uid") val firebaseUid: String,
    @SerialName("payload_text") val payloadText: String,
    @SerialName("updated_at") val updatedAt: String? = null
)
