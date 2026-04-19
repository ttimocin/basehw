package com.taytek.basehw.domain.repository

import com.taytek.basehw.domain.model.DirectMessage
import com.taytek.basehw.domain.model.DirectConversation
import kotlinx.coroutines.flow.Flow

interface SupabaseSyncRepository {
    suspend fun syncProfile(
        firebaseUid: String,
        displayName: String?,
        photoUrl: String?
    ): Result<Unit>

    suspend fun publishListing(
        firebaseUid: String,
        listingId: String,
        title: String,
        imageUrl: String?
    ): Result<Unit>

    suspend fun deletePublicListings(firebaseUid: String): Result<Unit>

    suspend fun createMessage(
        firebaseUid: String,
        conversationId: String,
        receiverUid: String,
        messageBody: String
    ): Result<Unit>

    suspend fun getConversationMessages(
        firebaseUid: String,
        peerUid: String,
        limit: Int = 100
    ): Result<List<DirectMessage>>

    suspend fun getPublicListingTitles(
        firebaseUid: String,
        limit: Int = 200
    ): Result<List<String>>

    fun observeConversationMessages(
        firebaseUid: String,
        peerUid: String
    ): Flow<DirectMessage>

    suspend fun getInboxConversations(
        firebaseUid: String,
        limit: Int = 200
    ): Result<List<DirectConversation>>

    suspend fun upsertCollectionSnapshot(
        firebaseUid: String,
        payloadText: String
    ): Result<Unit>

    suspend fun fetchCollectionSnapshot(
        firebaseUid: String
    ): Result<String?>

    suspend fun hasCollectionSnapshot(
        firebaseUid: String
    ): Result<Boolean>

    suspend fun deleteCollectionSnapshot(
        firebaseUid: String
    ): Result<Unit>
}
