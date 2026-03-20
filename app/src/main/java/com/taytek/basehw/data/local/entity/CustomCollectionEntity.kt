package com.taytek.basehw.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_collections")
data class CustomCollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverPhotoUrl: String? = null,
    val firestoreId: String = "",
    val createdAtMillis: Long
)
