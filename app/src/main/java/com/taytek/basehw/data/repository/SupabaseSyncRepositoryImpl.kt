package com.taytek.basehw.data.repository

import com.taytek.basehw.data.remote.supabase.model.SupabaseMessageRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseMessageReadRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseCollectionSnapshotRow
import com.taytek.basehw.data.local.AppSettingsManager
import com.taytek.basehw.data.remote.supabase.model.SupabaseProfileRow
import com.taytek.basehw.data.remote.supabase.model.SupabasePublicListingReadRow
import com.taytek.basehw.data.remote.supabase.model.SupabasePublicListingRow
import com.taytek.basehw.domain.model.DirectConversation
import com.taytek.basehw.domain.model.DirectMessage
import com.taytek.basehw.domain.repository.SupabaseSyncRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSyncRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val appSettingsManager: AppSettingsManager
) : SupabaseSyncRepository {

    override suspend fun syncProfile(
        firebaseUid: String,
        displayName: String?,
        photoUrl: String?
    ): Result<Unit> = runCatching {
        // Fetch existing profile to check if display_name is already set
        val existing = supabaseClient
            .from("profiles")
            .select {
                filter {
                    eq("firebase_uid", firebaseUid)
                }
            }
            .decodeList<SupabaseProfileRow>()
            .firstOrNull()

        if (existing == null) {
            // New user: Save provided data (Google name/photo as initial)
            val row = SupabaseProfileRow(
                firebaseUid = firebaseUid,
                displayName = displayName,
                photoUrl = photoUrl
            )
            supabaseClient.from("profiles").insert(row)
        } else {
            // Existing user: Update photo if provided, and nickname ONLY if currently empty
            supabaseClient.from("profiles").update({
                if (photoUrl != null) set("photo_url", photoUrl)
                if (existing.displayName.isNullOrBlank() && !displayName.isNullOrBlank()) {
                    set("display_name", displayName)
                }
            }) {
                filter {
                    eq("firebase_uid", firebaseUid)
                }
            }
        }
        Unit
    }

    override suspend fun publishListing(
        firebaseUid: String,
        listingId: String,
        title: String,
        imageUrl: String?
    ): Result<Unit> = runCatching {
        val row = SupabasePublicListingRow(
            firebaseUid = firebaseUid,
            listingId = listingId,
            title = title,
            imageUrl = imageUrl
        )

        supabaseClient.from("public_listings").upsert(row)
        Unit
    }

    override suspend fun deletePublicListings(firebaseUid: String): Result<Unit> = runCatching {
        supabaseClient.from("public_listings").delete {
            filter { eq("firebase_uid", firebaseUid) }
        }
        Unit
    }

    override suspend fun createMessage(
        firebaseUid: String,
        conversationId: String,
        receiverUid: String,
        messageBody: String
    ): Result<Unit> = runCatching {
        val row = SupabaseMessageRow(
            firebaseUid = firebaseUid,
            conversationId = conversationId,
            receiverUid = receiverUid,
            messageBody = messageBody
        )

        supabaseClient.from("messages").insert(row)
        Unit
    }

    override suspend fun getConversationMessages(
        firebaseUid: String,
        peerUid: String,
        limit: Int
    ): Result<List<DirectMessage>> = runCatching {
        val conversationId = buildConversationId(firebaseUid, peerUid)
        supabaseClient
            .from("messages")
            .select {
                filter {
                    eq("conversation_id", conversationId)
                }
                order("created_at", Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList<SupabaseMessageReadRow>()
            .map { row ->
                DirectMessage(
                    id = row.id,
                    senderUid = row.firebaseUid,
                    receiverUid = row.receiverUid,
                    body = row.messageBody,
                    createdAt = parseIsoTimestamp(row.createdAt)
                )
            }
    }

    override suspend fun getPublicListingTitles(
        firebaseUid: String,
        limit: Int
    ): Result<List<String>> = runCatching {
        supabaseClient
            .from("public_listings")
            .select {
                filter {
                    eq("firebase_uid", firebaseUid)
                }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<SupabasePublicListingReadRow>()
            .map { it.title }
    }

    override fun observeConversationMessages(
        firebaseUid: String,
        peerUid: String
    ): Flow<DirectMessage> = callbackFlow {
        val conversationId = buildConversationId(firebaseUid, peerUid)
        // Use a unique channel topic per subscription to avoid reusing an already-joined channel.
        val topic = "messages-$conversationId-${UUID.randomUUID()}"
        val realtimeChannel = supabaseClient.channel(topic)

        val collectJob = launch {
            realtimeChannel
                .postgresChangeFlow<PostgresAction.Insert>("public") {
                    table = "messages"
                    filter("conversation_id", FilterOperator.EQ, conversationId)
                }
                .collect { insert ->
                    val parsed = parseMessageRecord(insert.record)
                    if (parsed != null) {
                        trySend(parsed)
                    }
                }
        }

        launch { realtimeChannel.subscribe() }

        awaitClose {
            collectJob.cancel()
            launch { realtimeChannel.unsubscribe() }
        }
    }

    override suspend fun getInboxConversations(
        firebaseUid: String,
        limit: Int
    ): Result<List<DirectConversation>> = runCatching {
        val sent = supabaseClient
            .from("messages")
            .select {
                filter { eq("firebase_uid", firebaseUid) }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<SupabaseMessageReadRow>()

        val received = supabaseClient
            .from("messages")
            .select {
                filter { eq("receiver_uid", firebaseUid) }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<SupabaseMessageReadRow>()

        val merged = (sent + received)
            .sortedByDescending { parseIsoTimestamp(it.createdAt) }

        val latestByConversation = LinkedHashMap<String, SupabaseMessageReadRow>()
        for (row in merged) {
            if (!latestByConversation.containsKey(row.conversationId)) {
                latestByConversation[row.conversationId] = row
            }
        }

        val peers = latestByConversation.values
            .map { row -> if (row.firebaseUid == firebaseUid) row.receiverUid else row.firebaseUid }
            .toSet()

        val profileNameByUid = if (peers.isNotEmpty()) {
            runCatching {
                supabaseClient
                    .from("profiles_public_view")
                    .select {
                        filter {
                            isIn("firebase_uid", peers.toList())
                        }
                    }
                    .decodeList<SupabaseProfileRow>()
                    .associate { it.firebaseUid to (it.displayName ?: "User") }
            }.getOrDefault(emptyMap())
        } else {
            emptyMap()
        }


        val lastSeenAt = appSettingsManager.getCommunityInboxLastSeenAt()

        latestByConversation.values.map { row ->
            val peerUid = if (row.firebaseUid == firebaseUid) row.receiverUid else row.firebaseUid
            val messageTimestamp = parseIsoTimestamp(row.createdAt)
            val isUnread = row.firebaseUid != firebaseUid && messageTimestamp > lastSeenAt
            DirectConversation(
                conversationId = row.conversationId,
                peerUid = peerUid,
                peerDisplayName = profileNameByUid[peerUid] ?: "User",
                lastMessage = row.messageBody,
                lastMessageAt = messageTimestamp,
                lastMessageSenderUid = row.firebaseUid,
                isUnread = isUnread
            )
        }.sortedByDescending { it.lastMessageAt }
    }

    override suspend fun upsertCollectionSnapshot(
        firebaseUid: String,
        payloadText: String
    ): Result<Unit> = runCatching {
        supabaseClient.from("user_collection_snapshots").upsert(
            SupabaseCollectionSnapshotRow(
                firebaseUid = firebaseUid,
                payloadText = payloadText
            )
        )
        Unit
    }

    override suspend fun fetchCollectionSnapshot(firebaseUid: String): Result<String?> = runCatching {
        supabaseClient.from("user_collection_snapshots")
            .select {
                filter { eq("firebase_uid", firebaseUid) }
                limit(1)
            }
            .decodeSingleOrNull<SupabaseCollectionSnapshotRow>()
            ?.payloadText
    }

    override suspend fun hasCollectionSnapshot(firebaseUid: String): Result<Boolean> = runCatching {
        fetchCollectionSnapshot(firebaseUid).getOrNull().isNullOrBlank().not()
    }

    override suspend fun deleteCollectionSnapshot(firebaseUid: String): Result<Unit> = runCatching {
        supabaseClient.from("user_collection_snapshots").delete {
            filter { eq("firebase_uid", firebaseUid) }
        }
        Unit
    }

    private fun buildConversationId(uidA: String, uidB: String): String {
        return if (uidA <= uidB) "${uidA}__${uidB}" else "${uidB}__${uidA}"
    }

    private fun parseIsoTimestamp(value: String): Long {
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)
    }

    private fun parseMessageRecord(record: JsonObject): DirectMessage? {
        val id = runCatching { record["id"]?.jsonPrimitive?.content }.getOrNull() ?: return null
        val senderUid = runCatching { record["firebase_uid"]?.jsonPrimitive?.content }.getOrNull() ?: return null
        val receiverUid = runCatching { record["receiver_uid"]?.jsonPrimitive?.content }.getOrNull() ?: return null
        val body = runCatching { record["message_body"]?.jsonPrimitive?.content }.getOrNull() ?: return null
        val createdAt = runCatching { record["created_at"]?.jsonPrimitive?.content }.getOrNull() ?: return null
        return DirectMessage(
            id = id,
            senderUid = senderUid,
            receiverUid = receiverUid,
            body = body,
            createdAt = parseIsoTimestamp(createdAt)
        )
    }
}
