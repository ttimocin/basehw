package com.taytek.basehw.data.repository

import android.util.Log
import android.util.Base64

import com.google.firebase.auth.FirebaseAuth
import com.taytek.basehw.data.remote.supabase.model.SupabaseBannedUserRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseCommunityCommentRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseCreateCommunityCommentRequest
import com.taytek.basehw.data.remote.supabase.model.SupabaseCreateCommunityPostRequest
import com.taytek.basehw.data.remote.supabase.model.SupabaseCommunityLikeRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseCommunityReactionRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseCommunityPostRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseFollowRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseBlockRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseIdOnlyRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseProfileRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseSetModeratorFirebaseRequest
import com.taytek.basehw.domain.model.CommunityComment
import com.taytek.basehw.domain.model.CommunityPost
import com.taytek.basehw.domain.model.User
import com.taytek.basehw.domain.repository.CommunityRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val auth: FirebaseAuth,
    private val contentModerator: com.taytek.basehw.domain.util.ContentModerator
) : CommunityRepository {

    private val currentUid: String?
        get() = auth.currentUser?.uid

    // ── Blocked Users Helper ────────────────────────────────

    private suspend fun getBlockedUids(): Set<String> {
        val uid = currentUid ?: return emptySet()
        return runCatching {
            supabaseClient.from("blocked_users").select {
                filter { eq("blocker_uid", uid) }
            }.decodeList<SupabaseBlockRow>().map { it.blockedUid }.toSet()
        }.getOrDefault(emptySet())
    }

    // ── Posts ───────────────────────────────────────────────

    override suspend fun createPost(
        carModelName: String,
        carBrand: String,
        carYear: Int?,
        carSeries: String?,
        carImageUrl: String,
        caption: String,
        carFeature: String?,
        authorSelectedAvatarId: Int,
        authorCustomAvatarUrl: String?,
        carImageUrls: List<String>
    ): Result<String> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))

        // Check if user is banned from forum
        val isBanned = isUserBannedFromForum(uid).getOrDefault(false)
        if (isBanned) {
            return Result.failure(Exception("ERR_FORUM_BANNED"))
        }

        ensureRulesAccepted(uid).onFailure { return Result.failure(it) }
        
        // AI Content Moderation (Anonymized: only caption is sent)
        val moderationResult = contentModerator.validateContent(caption).fold(
            onSuccess = { result -> result },
            onFailure = { e -> 
                Log.w("CommunityRepo", "Moderation API unavailable in createPost, blocking post", e)
                return Result.failure(Exception("ERR_MODERATION_UNAVAILABLE"))
            }
        )

        if (!moderationResult.is_safe) {
            return Result.failure(Exception(moderationResult.reason ?: "ERR_CONTENT_RULES_VIOLATION"))
        }

        return try {
            val profile = fetchProfile(uid)
            val username = profile?.displayName ?: auth.currentUser?.displayName ?: "User"
            val authorIsAdmin = profile?.isAdmin ?: false
            val authorIsMod = profile?.isMod ?: false

            val inserted = supabaseClient.postgrest.rpc(
                "create_community_post",
                SupabaseCreateCommunityPostRequest(
                    authorUid = uid,
                    authorUsername = username,
                    authorIsAdmin = authorIsAdmin,
                    authorIsMod = authorIsMod,
                    authorSelectedAvatarId = authorSelectedAvatarId,
                    authorCustomAvatarUrl = authorCustomAvatarUrl,
                    carModelName = carModelName,
                    carBrand = carBrand,
                    carYear = carYear,
                    carSeries = carSeries,
                    carImageUrl = carImageUrl,
                    caption = caption,
                    carFeature = carFeature,
                    carImageUrls = carImageUrls
                )
            ).decodeSingle<SupabaseIdOnlyRow>()

            Result.success(inserted.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFeedPosts(limit: Int, lastTimestamp: Long?): Result<List<CommunityPost>> {
        return try {
            val blockedUids = getBlockedUids()
            val rows = supabaseClient.from("community_posts").select {
                filter {
                    if (lastTimestamp != null) {
                        lt("created_at", Instant.ofEpochMilli(lastTimestamp).toString())
                    }
                }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<SupabaseCommunityPostRow>()



            // Filter out posts from blocked users
            val filteredRows = if (blockedUids.isNotEmpty()) {
                rows.filter { it.authorUid !in blockedUids }
            } else {
                rows
            }

            val posts = filteredRows.map { row -> row.toDomainPost() }
            val livePosts = applyAuthorStatus(posts)

            // Check reactions for current user (fallback to like flags if reactions table not available)
            val uid = currentUid
            Result.success(applyReactionFlags(livePosts, uid))
        } catch (e: Exception) {
            Log.e("CommunityRepo", "getFeedPosts error", e)
            Result.failure(e)
        }
    }

    override suspend fun getFollowingPosts(limit: Int): Result<List<CommunityPost>> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val followingUids = getFollowingUids().getOrDefault(emptyList())
            if (followingUids.isEmpty()) return Result.success(emptyList())
            
            val blockedUids = getBlockedUids()
            val filteredFollowingUids = if (blockedUids.isNotEmpty()) {
                followingUids.filter { it !in blockedUids }
            } else {
                followingUids
            }
            
            if (filteredFollowingUids.isEmpty()) return Result.success(emptyList())

            val rows = supabaseClient.from("community_posts").select {
                filter {
                    isIn("author_uid", filteredFollowingUids)
                }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<SupabaseCommunityPostRow>()

            val posts = rows.map { it.toDomainPost() }
            val livePosts = applyAuthorStatus(posts)
            Result.success(applyReactionFlags(livePosts, uid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserPosts(uid: String, limit: Int): Result<List<CommunityPost>> {
        return try {
            val blockedUids = getBlockedUids()
            // If viewing own posts, don't filter
            val shouldFilter = uid != currentUid && blockedUids.contains(uid)
            if (shouldFilter) return Result.success(emptyList())
            
            val rows = supabaseClient.from("community_posts").select {
                filter {
                    eq("author_uid", uid)
                }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<SupabaseCommunityPostRow>()

            val posts = rows.map { it.toDomainPost() }
            val livePosts = applyAuthorStatus(posts)

            val myUid = currentUid
            Result.success(applyReactionFlags(livePosts, myUid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Likes (Legacy) ───────────────────────────────────

    override suspend fun toggleLike(postId: String): Result<Boolean> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val existing = supabaseClient.from("community_likes").select {
                filter {
                    eq("post_id", postId)
                    eq("user_uid", uid)
                }
                limit(1)
            }.decodeList<SupabaseCommunityLikeRow>().isNotEmpty()

            if (existing) {
                supabaseClient.from("community_likes").delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_uid", uid)
                    }
                }
                Result.success(false)
            } else {
                supabaseClient.from("community_likes").insert(
                    SupabaseCommunityLikeRow(postId = postId, userUid = uid)
                )
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Reactions ─────────────────────────────────────────

    override suspend fun toggleReaction(postId: String, emoji: String): Result<Pair<Boolean, String?>> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            // Mevcut reaksiyonu kontrol et
            val existing = supabaseClient.from("community_reactions").select {
                filter {
                    eq("post_id", postId)
                    eq("user_uid", uid)
                }
                limit(1)
            }.decodeList<SupabaseCommunityReactionRow>().firstOrNull()

            if (existing != null) {
                if (existing.emoji == emoji) {
                    // Aynı emoji → reaksiyonu kaldır (toggle off)
                    supabaseClient.from("community_reactions").delete {
                        filter {
                            eq("post_id", postId)
                            eq("user_uid", uid)
                        }
                    }
                    Result.success(Pair(false, null))
                } else {
                    // Farklı emoji → güncelle
                    supabaseClient.from("community_reactions").update({
                        set("emoji", emoji)
                    }) {
                        filter {
                            eq("post_id", postId)
                            eq("user_uid", uid)
                        }
                    }
                    Result.success(Pair(true, emoji))
                }
            } else {
                // Yeni reaksiyon ekle
                supabaseClient.from("community_reactions").insert(
                    SupabaseCommunityReactionRow(postId = postId, userUid = uid, emoji = emoji)
                )
                Result.success(Pair(true, emoji))
            }
        } catch (e: Exception) {
            Log.e("CommunityRepo", "toggleReaction error", e)
            Result.failure(e)
        }
    }

    // ── Comments ───────────────────────────────────────────

    override suspend fun addComment(postId: String, text: String): Result<CommunityComment> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))

        // Check if user is banned from forum
        val isBanned = isUserBannedFromForum(uid).getOrDefault(false)
        if (isBanned) {
            return Result.failure(Exception("ERR_FORUM_BANNED"))
        }

        ensureRulesAccepted(uid).onFailure { return Result.failure(it) }
        
        // AI Content Moderation (Anonymized: only text is sent)
        val moderationResult = contentModerator.validateContent(text).fold(
            onSuccess = { result -> result },
            onFailure = { e -> 
                Log.w("CommunityRepo", "Moderation API unavailable in addComment, blocking comment", e)
                return Result.failure(Exception("ERR_MODERATION_UNAVAILABLE"))
            }
        )

        if (!moderationResult.is_safe) {
            return Result.failure(Exception(moderationResult.reason ?: "ERR_CONTENT_RULES_VIOLATION"))
        }

        return try {
            val profile = fetchProfile(uid)
            val username = profile?.displayName ?: auth.currentUser?.displayName ?: "User"
            val isAdmin = profile?.isAdmin ?: false
            val isMod = profile?.isMod ?: false

            val inserted = supabaseClient.postgrest.rpc(
                "create_community_comment",
                SupabaseCreateCommunityCommentRequest(
                    postId = postId,
                    authorUid = uid,
                    authorUsername = username,
                    authorIsAdmin = isAdmin,
                    authorIsMod = isMod,
                    text = text
                )
            ).decodeSingle<SupabaseCommunityCommentRow>()

            Result.success(inserted.toDomainComment())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getComments(postId: String): Result<List<CommunityComment>> {
        return try {
            val blockedUids = getBlockedUids()
            val comments = supabaseClient.from("community_comments").select {
                filter { eq("post_id", postId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<SupabaseCommunityCommentRow>()
            
            // Filter out comments from blocked users
            val filteredComments = if (blockedUids.isNotEmpty()) {
                comments.filter { it.authorUid !in blockedUids }
            } else {
                comments
            }.map { it.toDomainComment() }

            val liveComments = applyCommentAuthorStatus(filteredComments)
            Result.success(liveComments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Follow ─────────────────────────────────────────────

    override suspend fun followUser(targetUid: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        if (uid == targetUid) return Result.failure(Exception("Cannot follow yourself"))
        return try {
            // Check if either user has blocked the other
            val isBlocked = supabaseClient.from("blocked_users").select {
                filter {
                    or {
                        and {
                            eq("blocker_uid", targetUid)
                            eq("blocked_uid", uid)
                        }
                        and {
                            eq("blocker_uid", uid)
                            eq("blocked_uid", targetUid)
                        }
                    }
                }
                limit(1)
            }.decodeList<SupabaseBlockRow>().isNotEmpty()

            if (isBlocked) {
                return Result.failure(Exception("Bu kullanıcıyı takip edemezsiniz."))
            }

            if (isFollowing(targetUid).getOrDefault(false)) return Result.success(Unit)
            supabaseClient.from("follows").insert(
                SupabaseFollowRow(followerUid = uid, followedUid = targetUid)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(targetUid: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            supabaseClient.from("follows").delete {
                filter {
                    eq("follower_uid", uid)
                    eq("followed_uid", targetUid)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFollowing(targetUid: String): Result<Boolean> {
        val uid = currentUid ?: return Result.success(false)
        return try {
            val follows = supabaseClient.from("follows").select {
                filter {
                    eq("follower_uid", uid)
                    eq("followed_uid", targetUid)
                }
                limit(1)
            }.decodeList<SupabaseFollowRow>()
            Result.success(follows.isNotEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeFollowStatus(currentUid: String, targetUid: String): Flow<Boolean> = callbackFlow {
        if (currentUid.isBlank() || targetUid.isBlank() || currentUid == targetUid) {
            trySend(false)
            close()
            return@callbackFlow
        }

        suspend fun emitCurrentStatus() {
            val following = queryFollowStatus(currentUid, targetUid)
            trySend(following)
        }

        emitCurrentStatus()

        // Realtime token can be invalid for websocket (missing role/exp).
        // Keep follow status stable by polling regardless of realtime state.
        val pollingJob = launch {
            while (isActive) {
                delay(2500)
                emitCurrentStatus()
            }
        }

        val canUseRealtime = hasRealtimeJwtClaims()

        // Use separate channels for insert and delete to avoid lifecycle issues.
        val insertTopic = "follow-insert-$currentUid-$targetUid-${UUID.randomUUID()}"
        val deleteTopic = "follow-delete-$currentUid-$targetUid-${UUID.randomUUID()}"
        val insertChannel = if (canUseRealtime) supabaseClient.channel(insertTopic) else null
        val deleteChannel = if (canUseRealtime) supabaseClient.channel(deleteTopic) else null

        val insertJob = if (insertChannel != null) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                insertChannel
                    .postgresChangeFlow<PostgresAction.Insert>("public") {
                        table = "follows"
                        filter("follower_uid", FilterOperator.EQ, currentUid)
                        filter("followed_uid", FilterOperator.EQ, targetUid)
                    }
                    .collect {
                        emitCurrentStatus()
                    }
            }
        } else null

        val deleteJob = if (deleteChannel != null) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                deleteChannel
                    .postgresChangeFlow<PostgresAction.Delete>("public") {
                        table = "follows"
                        filter("follower_uid", FilterOperator.EQ, currentUid)
                        filter("followed_uid", FilterOperator.EQ, targetUid)
                    }
                    .collect {
                        emitCurrentStatus()
                    }
            }
        } else null

        if (insertChannel != null && deleteChannel != null) {
            runCatching {
                insertChannel.subscribe()
                deleteChannel.subscribe()
            }.onFailure {
                Log.w("CommunityRepo", "Realtime follow subscription failed, polling fallback active.", it)
            }
        } else {
            Log.i("CommunityRepo", "Realtime follow disabled for current token claims; polling fallback active.")
        }

        awaitClose {
            pollingJob.cancel()
            insertJob?.cancel()
            deleteJob?.cancel()
            launch { 
                runCatching { insertChannel?.unsubscribe() }
                runCatching { deleteChannel?.unsubscribe() }
            }
        }
    }

    private suspend fun hasRealtimeJwtClaims(): Boolean {
        val token = runCatching {
            auth.currentUser?.getIdToken(false)?.await()?.token
        }.getOrNull() ?: return false

        val payloadPart = token.split('.').getOrNull(1) ?: return false
        val payloadJson = runCatching {
            val decoded = Base64.decode(
                payloadPart,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            String(decoded, Charsets.UTF_8)
        }.getOrNull() ?: return false

        return payloadJson.contains("\"role\"") && payloadJson.contains("\"exp\"")
    }

    override suspend fun getFollowingUids(): Result<List<String>> {
        val uid = currentUid ?: return Result.success(emptyList())
        return try {
            val follows = supabaseClient.from("follows").select {
                filter { eq("follower_uid", uid) }
                order("created_at", Order.DESCENDING)
            }.decodeList<SupabaseFollowRow>()
            val uids = follows.map { it.followedUid }
            Result.success(uids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFollowersUsers(uid: String): Result<List<User>> {
        return try {
            val follows = supabaseClient.from("follows").select {
                filter { eq("followed_uid", uid) }
                order("created_at", Order.DESCENDING)
            }.decodeList<SupabaseFollowRow>()
            
            val followerUids = follows.map { it.followerUid }
            if (followerUids.isEmpty()) return Result.success(emptyList())
            
            val profilesMap = mutableMapOf<String, SupabaseProfileRow>()
            // Not: İdeal olarak batch işlemi yapılır ama supabase için in filtreleme yapabiliriz
            // Profiles tablosunda "firebase_uid" üzerinden arama yapacağız.
            val rows = supabaseClient.from("profiles_public_view").select {
                filter { isIn("firebase_uid", followerUids) }
            }.decodeList<SupabaseProfileRow>()
            
            rows.forEach { profilesMap[it.firebaseUid] = it }
            
            // Get banned users for isForumBanned flag
            val bannedUids = runCatching {
                supabaseClient.from("banned_users").select {
                    filter { isIn("user_uid", followerUids) }
                }.decodeList<SupabaseBannedUserRow>().map { it.userUid }.toSet()
            }.getOrDefault(emptySet())

            val users = followerUids.mapNotNull { fUid -> 
                profilesMap[fUid]?.let { profile ->
                    User(
                        uid = profile.firebaseUid,
                        email = profile.email ?: "",
                        username = profile.displayName,
                        photoUrl = profile.photoUrl,
                        followerCount = profile.followerCount,
                        followingCount = profile.followingCount,
                        postCount = profile.postCount,
                        rulesAccepted = profile.rulesAccepted,
                        isAdmin = profile.isAdmin,
                        isMod = profile.isMod,
                        isCollectionPublic = profile.collectionPublic,
                        isWishlistPublic = profile.wishlistPublic,
                        selectedAvatarId = profile.selectedAvatarId ?: 1,
                        customAvatarUrl = profile.customAvatarUrl,
                        isForumBanned = bannedUids.contains(fUid)
                    )
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFollowingUsers(uid: String): Result<List<User>> {
        return try {
            val follows = supabaseClient.from("follows").select {
                filter { eq("follower_uid", uid) }
                order("created_at", Order.DESCENDING)
            }.decodeList<SupabaseFollowRow>()
            
            val followedUids = follows.map { it.followedUid }
            if (followedUids.isEmpty()) return Result.success(emptyList())
            
            val profilesMap = mutableMapOf<String, SupabaseProfileRow>()
            val rows = supabaseClient.from("profiles_public_view").select {
                filter { isIn("firebase_uid", followedUids) }
            }.decodeList<SupabaseProfileRow>()
            
            rows.forEach { profilesMap[it.firebaseUid] = it }
            
            // Get banned users for isForumBanned flag
            val bannedUids = runCatching {
                supabaseClient.from("banned_users").select {
                    filter { isIn("user_uid", followedUids) }
                }.decodeList<SupabaseBannedUserRow>().map { it.userUid }.toSet()
            }.getOrDefault(emptySet())

            val users = followedUids.mapNotNull { fUid -> 
                profilesMap[fUid]?.let { profile ->
                    User(
                        uid = profile.firebaseUid,
                        email = profile.email ?: "",
                        username = profile.displayName,
                        photoUrl = profile.photoUrl,
                        followerCount = profile.followerCount,
                        followingCount = profile.followingCount,
                        postCount = profile.postCount,
                        rulesAccepted = profile.rulesAccepted,
                        isAdmin = profile.isAdmin,
                        isMod = profile.isMod,
                        isCollectionPublic = profile.collectionPublic,
                        isWishlistPublic = profile.wishlistPublic,
                        selectedAvatarId = profile.selectedAvatarId ?: 1,
                        customAvatarUrl = profile.customAvatarUrl,
                        isForumBanned = bannedUids.contains(fUid)
                    )
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── User Profile ───────────────────────────────────────

    override suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val profile = fetchProfile(uid) ?: return Result.failure(Exception("User not found"))
            val isForumBanned = isUserBannedFromForum(uid).getOrDefault(false)
            val user = User(
                uid = uid,
                email = profile.email ?: "",
                username = profile.displayName,
                photoUrl = profile.photoUrl,
                followerCount = profile.followerCount,
                followingCount = profile.followingCount,
                postCount = profile.postCount,
                rulesAccepted = profile.rulesAccepted,
                isAdmin = profile.isAdmin,
                isMod = profile.isMod,
                isCollectionPublic = profile.collectionPublic,
                isWishlistPublic = profile.wishlistPublic,
                selectedAvatarId = profile.selectedAvatarId ?: 1,
                customAvatarUrl = profile.customAvatarUrl,
                isForumBanned = isForumBanned
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTopUsers(limit: Int): Result<List<User>> {
        return try {
            val rows = supabaseClient.from("profiles_public_view").select {
                order("post_count", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<SupabaseProfileRow>()

            val users = rows.map { row ->
                User(
                    uid = row.firebaseUid,
                    email = row.email ?: "",
                    username = row.displayName,
                    photoUrl = row.photoUrl,
                    followerCount = row.followerCount,
                    followingCount = row.followingCount,
                    postCount = row.postCount,
                    rulesAccepted = row.rulesAccepted,
                    isAdmin = row.isAdmin,
                    isCollectionPublic = row.collectionPublic,
                    isWishlistPublic = row.wishlistPublic,
                    selectedAvatarId = row.selectedAvatarId ?: 1,
                    customAvatarUrl = row.customAvatarUrl
                )
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "getTopUsers error", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val post = supabaseClient.from("community_posts").select {
                filter { eq("id", postId) }
                limit(1)
            }.decodeSingleOrNull<SupabaseCommunityPostRow>()
                ?: return Result.failure(Exception("Post not found"))

            // Check if current user is admin or mod
            val profile = fetchProfile(uid)
            val isAdmin = profile?.isAdmin ?: false
            val isMod = profile?.isMod ?: false

            if (post.authorUid == uid) {
                // Own post: direct delete (RLS allows it)
                supabaseClient.from("community_posts").delete {
                    filter { eq("id", postId) }
                }
            } else if (isAdmin || isMod) {
                // Admin/mod deleting someone else's post: use RPC
                supabaseClient.postgrest.rpc(
                    "admin_delete_community_post",
                    kotlinx.serialization.json.buildJsonObject {
                        put("p_post_id", kotlinx.serialization.json.JsonPrimitive(postId))
                    }
                )
            } else {
                return Result.failure(Exception("Bu gönderiyi silme yetkiniz yok."))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "deletePost error", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {

            val comment = supabaseClient.from("community_comments").select {
                filter {
                    eq("id", commentId)
                    eq("post_id", postId)
                }
                limit(1)
            }.decodeSingleOrNull<SupabaseCommunityCommentRow>()
                ?: return Result.failure(Exception("Yorum bulunamadı."))

            val profile = fetchProfile(uid)
            val isAdmin = profile?.isAdmin ?: false
            val isMod = profile?.isMod ?: false

            // Comment author or post owner can delete directly (RLS allows it)
            // Admin/mod deleting someone else's comment: use RPC
            if (uid == comment.authorUid) {
                // Own comment: direct delete (RLS allows it)
                supabaseClient.from("community_comments").delete {
                    filter {
                        eq("id", commentId)
                        eq("post_id", postId)
                    }
                }
            } else if (isAdmin || isMod) {
                // Admin/mod: use RPC to delete (bypasses RLS owner check)
                supabaseClient.postgrest.rpc(
                    "admin_delete_community_comment",
                    kotlinx.serialization.json.buildJsonObject {
                        put("p_comment_id", kotlinx.serialization.json.JsonPrimitive(commentId))
                    }
                )
            } else {
                // Post owner deleting a comment on their post: check via RLS
                val post = supabaseClient.from("community_posts").select {
                    filter { eq("id", postId) }
                    limit(1)
                }.decodeSingleOrNull<SupabaseCommunityPostRow>()
                    ?: return Result.failure(Exception("Post bulunamadı."))

                if (uid == post.authorUid) {
                    supabaseClient.from("community_comments").delete {
                        filter {
                            eq("id", commentId)
                            eq("post_id", postId)
                        }
                    }
                } else {
                    return Result.failure(Exception("Bu yorumu silme yetkiniz yok."))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "deleteComment error", e)
            Result.failure(e)
        }
    }

    override suspend fun acceptRules(): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            supabaseClient.from("profiles").update({
                set("rules_accepted", true)
            }) {
                filter { eq("firebase_uid", uid) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Admin Panel Functions ───────────────────────────────

    override suspend fun setModerator(targetUid: String, isMod: Boolean): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        val adminProfile = fetchProfile(uid)
        if (adminProfile?.isAdmin != true) {
            return Result.failure(Exception("Bu işlem için admin yetkisi gerekiyor."))
        }

        return try {
            supabaseClient.postgrest.rpc(
                "set_user_moderator_by_firebase_uid",
                SupabaseSetModeratorFirebaseRequest(targetUid, isMod)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "setModerator error", e)
            Result.failure(e)
        }
    }

    override suspend fun banUserFromForum(targetUid: String, reason: String?): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))

        return try {
            supabaseClient.postgrest.rpc(
                "admin_ban_user",
                kotlinx.serialization.json.buildJsonObject {
                    put("p_admin_uid", kotlinx.serialization.json.JsonPrimitive(uid))
                    put("p_target_uid", kotlinx.serialization.json.JsonPrimitive(targetUid))
                    put("p_reason", kotlinx.serialization.json.JsonPrimitive(reason))
                }
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "banUserFromForum error", e)
            Result.failure(e)
        }
    }

    override suspend fun unbanUserFromForum(targetUid: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))

        return try {
            supabaseClient.postgrest.rpc(
                "admin_unban_user",
                kotlinx.serialization.json.buildJsonObject {
                    put("p_admin_uid", kotlinx.serialization.json.JsonPrimitive(uid))
                    put("p_target_uid", kotlinx.serialization.json.JsonPrimitive(targetUid))
                }
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "unbanUserFromForum error", e)
            Result.failure(e)
        }
    }

    override suspend fun getBannedUsers(): Result<List<User>> {
        val uid = currentUid ?: return Result.success(emptyList())
        val profile = fetchProfile(uid)
        val isAdmin = profile?.isAdmin ?: false
        val isMod = profile?.isMod ?: false

        if (!isAdmin && !isMod) {
            return Result.failure(Exception("ERR_ADMIN_MOD_REQUIRED"))
        }

        return try {
            val bannedRows = supabaseClient.from("banned_users").select {
                order("created_at", Order.DESCENDING)
            }.decodeList<SupabaseBannedUserRow>()

            val bannedUids = bannedRows.map { it.userUid }
            if (bannedUids.isEmpty()) return Result.success(emptyList())

            val rows = supabaseClient.from("profiles_public_view").select {
                filter { isIn("firebase_uid", bannedUids) }
            }.decodeList<SupabaseProfileRow>()

            val profilesMap = rows.associateBy { it.firebaseUid }

            val users = bannedUids.mapNotNull { bUid ->
                profilesMap[bUid]?.let { prof ->
                    User(
                        uid = prof.firebaseUid,
                        email = prof.email ?: "",
                        username = prof.displayName,
                        photoUrl = prof.photoUrl,
                        followerCount = prof.followerCount,
                        followingCount = prof.followingCount,
                        postCount = prof.postCount,
                        rulesAccepted = prof.rulesAccepted,
                        isAdmin = prof.isAdmin,
                        isMod = prof.isMod,
                        isCollectionPublic = prof.collectionPublic,
                        isWishlistPublic = prof.wishlistPublic,
                        selectedAvatarId = prof.selectedAvatarId ?: 1,
                        customAvatarUrl = prof.customAvatarUrl,
                        isForumBanned = true
                    )
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "getBannedUsers error", e)
            Result.failure(e)
        }
    }

    override suspend fun isUserBannedFromForum(uid: String): Result<Boolean> {
        return try {
            val banned = supabaseClient.from("banned_users").select {
                filter { eq("user_uid", uid) }
                limit(1)
            }.decodeList<SupabaseBannedUserRow>().isNotEmpty()
            Result.success(banned)
        } catch (e: Exception) {
            // Tablo yoksa veya başka bir hata olursa, kullanıcı banlı DEĞİL varsay
            // Bu, uygulama çökmesini önler ve kullanıcıların foruma erişimini sağlar
            if (e.message?.contains("Could not find the table") == true || 
                e.message?.contains("banned_users") == true) {
                Log.w("CommunityRepo", "banned_users table not found in Supabase. Please run the SQL migration. Assuming user is NOT banned.")
                Result.success(false)
            } else {
                Log.e("CommunityRepo", "isUserBannedFromForum error", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun checkIsAdmin(uid: String): Result<Boolean> {
        return try {
            val profile = supabaseClient.from("profiles").select {
                filter { eq("firebase_uid", uid) }
                limit(1)
            }.decodeList<SupabaseProfileRow>().firstOrNull()
            Result.success(profile?.isAdmin == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllUsers(): Result<List<User>> {
        val uid = currentUid ?: return Result.success(emptyList())
        val profile = fetchProfile(uid)
        if (profile?.isAdmin != true && profile?.isMod != true) {
            return Result.failure(Exception("ERR_ADMIN_MOD_REQUIRED"))
        }

        return try {
            val rows = supabaseClient.from("profiles_public_view").select {
                order("created_at", Order.DESCENDING)
            }.decodeList<SupabaseProfileRow>()

            val bannedUids = runCatching {
                supabaseClient.from("banned_users").select {
                }.decodeList<SupabaseBannedUserRow>().map { it.userUid }.toSet()
            }.getOrDefault(emptySet())

            val users = rows.map { prof ->
                User(
                    uid = prof.firebaseUid,
                    email = prof.email ?: "",
                    username = prof.displayName,
                    photoUrl = prof.photoUrl,
                    followerCount = prof.followerCount,
                    followingCount = prof.followingCount,
                    postCount = prof.postCount,
                    rulesAccepted = prof.rulesAccepted,
                    isAdmin = prof.isAdmin,
                    isMod = prof.isMod,
                    isCollectionPublic = prof.collectionPublic,
                    isWishlistPublic = prof.wishlistPublic,
                    selectedAvatarId = prof.selectedAvatarId ?: 1,
                    customAvatarUrl = prof.customAvatarUrl,
                    isForumBanned = bannedUids.contains(prof.firebaseUid)
                )
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "getAllUsers error", e)
            Result.failure(e)
        }
    }

    override suspend fun blockFollower(targetUid: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        if (uid == targetUid) return Result.failure(Exception("Cannot block yourself"))
        return try {
            // 1. Remove the follow relationship (targetUid was following uid)
            val wasFollowing = supabaseClient.from("follows").select {
                filter {
                    eq("follower_uid", targetUid)
                    eq("followed_uid", uid)
                }
                limit(1)
            }.decodeList<SupabaseFollowRow>().isNotEmpty()

            supabaseClient.from("follows").delete {
                filter {
                    eq("follower_uid", targetUid)
                    eq("followed_uid", uid)
                }
            }

            // 2. Insert block record to prevent re-following
            supabaseClient.from("blocked_users").insert(
                SupabaseBlockRow(blockerUid = uid, blockedUid = targetUid)
            )


            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "blockFollower error", e)
            Result.failure(e)
        }
    }

    override suspend fun getBlockedUsers(): Result<List<User>> {
        val uid = currentUid ?: return Result.success(emptyList())
        return try {
            val blockRows = supabaseClient.from("blocked_users").select {
                filter { eq("blocker_uid", uid) }
                order("created_at", Order.DESCENDING)
            }.decodeList<SupabaseBlockRow>()

            val blockedUids = blockRows.map { it.blockedUid }
            if (blockedUids.isEmpty()) return Result.success(emptyList())

            val rows = supabaseClient.from("profiles_public_view").select {
                filter { isIn("firebase_uid", blockedUids) }
            }.decodeList<SupabaseProfileRow>()

            val profilesMap = rows.associateBy { it.firebaseUid }

            val users = blockedUids.mapNotNull { bUid ->
                profilesMap[bUid]?.let { profile ->
                    User(
                        uid = profile.firebaseUid,
                        email = profile.email ?: "",
                        username = profile.displayName,
                        photoUrl = profile.photoUrl,
                        followerCount = profile.followerCount,
                        followingCount = profile.followingCount,
                        postCount = profile.postCount,
                        rulesAccepted = profile.rulesAccepted,
                        isAdmin = profile.isAdmin,
                        isCollectionPublic = profile.collectionPublic,
                        isWishlistPublic = profile.wishlistPublic,
                        selectedAvatarId = profile.selectedAvatarId ?: 1,
                        customAvatarUrl = profile.customAvatarUrl
                    )
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "getBlockedUsers error", e)
            Result.failure(e)
        }
    }

    override suspend fun unblockUser(targetUid: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            supabaseClient.from("blocked_users").delete {
                filter {
                    eq("blocker_uid", uid)
                    eq("blocked_uid", targetUid)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "unblockUser error", e)
            Result.failure(e)
        }
    }

    override suspend fun hasUserBlockedMe(targetUid: String): Result<Boolean> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            // Check if targetUid has blocked uid (current user)
            val isBlocked = supabaseClient.from("blocked_users").select {
                filter {
                    eq("blocker_uid", targetUid)
                    eq("blocked_uid", uid)
                }
                limit(1)
            }.decodeList<SupabaseBlockRow>().isNotEmpty()
            
            Result.success(isBlocked)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "hasUserBlockedMe error", e)
            Result.failure(e)
        }
    }

    // ── Helpers ─────────────────────────────────────────────

    private suspend fun applyLikeFlags(posts: List<CommunityPost>, uid: String?): List<CommunityPost> {
        if (uid == null || posts.isEmpty()) return posts
        val postIds = posts.map { it.id }
        val likes = supabaseClient.from("community_likes").select {
            filter {
                eq("user_uid", uid)
                isIn("post_id", postIds)
            }
        }.decodeList<SupabaseCommunityLikeRow>()
        val likedIds = likes.map { it.postId }.toSet()
        return posts.map { it.copy(isLikedByMe = likedIds.contains(it.id)) }
    }

    /**
     * Mevcut kullanıcının reaksiyonlarını posts listesine uygular.
     * community_reactions tablosundan kullanıcının reaksiyonlarını çeker ve
     * her postun myReaction alanını günceller.
     */
    private suspend fun applyReactionFlags(posts: List<CommunityPost>, uid: String?): List<CommunityPost> {
        if (uid == null || posts.isEmpty()) return posts
        return try {
            val postIds = posts.map { it.id }
            val reactions = supabaseClient.from("community_reactions").select {
                filter {
                    eq("user_uid", uid)
                    isIn("post_id", postIds)
                }
            }.decodeList<SupabaseCommunityReactionRow>()
            val reactionMap = reactions.associate { it.postId to it.emoji }
            posts.map { post ->
                val myEmoji = reactionMap[post.id]
                post.copy(
                    myReaction = myEmoji,
                    // Backward compat: isLikedByMe ve likeCount
                    isLikedByMe = myEmoji != null,
                    likeCount = post.reactionCounts.values.sum()
                )
            }
        } catch (e: Exception) {
            Log.w("CommunityRepo", "applyReactionFlags: community_reactions table not available yet, using like flags", e)
            // Fallback: reaction tablosu yoksa like flags kullan
            applyLikeFlags(posts, uid)
        }
    }

    private fun SupabaseCommunityPostRow.toDomainPost(): CommunityPost {
        // JSONB reaction_counts → Map<String, Int>
        val reactionMap = parseReactionCounts(reactionCounts ?: kotlinx.serialization.json.buildJsonObject {})

        return CommunityPost(
            id = id,
            authorUid = authorUid,
            authorUsername = authorUsername,
            authorSelectedAvatarId = authorSelectedAvatarId ?: 1,
            authorCustomAvatarUrl = authorCustomAvatarUrl,
            carModelName = carModelName ?: "Unknown",
            carBrand = carBrand ?: "",
            carYear = carYear,
            carSeries = carSeries,
            carImageUrl = carImageUrl ?: "",
            caption = caption ?: "",
            carFeature = carFeature,
            authorIsAdmin = authorIsAdmin ?: false,
            authorIsMod = authorIsMod ?: false,
            likeCount = likeCount ?: 0,
            commentCount = commentCount ?: 0,
            createdAt = parseIsoTimestamp(createdAt ?: ""),
            isLikedByMe = false,
            isActive = isActive ?: true,
            reactionCounts = reactionMap,
            myReaction = null,
            carImageUrls = carImageUrls ?: emptyList()
        )
    }

    /**
     * Supabase JSONB reaction_counts alanını Map<String, Int> olarak parse eder.
     * Örnek: {"👍": 5, "❤️": 3}
     */
    private fun parseReactionCounts(json: kotlinx.serialization.json.JsonObject): Map<String, Int> {
        return try {
            json.entries.associate { (key, value) ->
                key to ((value as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 0)
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun SupabaseCommunityCommentRow.toDomainComment(): CommunityComment {
        return CommunityComment(
            id = id,
            authorUid = authorUid,
            authorUsername = authorUsername,
            authorIsAdmin = authorIsAdmin,
            authorIsMod = authorIsMod,
            text = text,
            createdAt = parseIsoTimestamp(createdAt)
        )
    }

    private fun parseIsoTimestamp(value: String): Long {
        return try {
            Instant.parse(value).toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    private suspend fun fetchProfile(uid: String): SupabaseProfileRow? {
        return supabaseClient.from("profiles").select {
            filter { eq("firebase_uid", uid) }
            limit(1)
        }.decodeSingleOrNull<SupabaseProfileRow>()
    }

    private suspend fun ensureRulesAccepted(uid: String): Result<Unit> {
        val profile = fetchProfile(uid)
            ?: return Result.failure(Exception("Kullanıcı profili bulunamadı. Lütfen tekrar giriş yapın."))
        if (!profile.rulesAccepted) {
            return Result.failure(Exception("Paylaşım ve yorum için topluluk kurallarını kabul etmelisiniz."))
        }
        return Result.success(Unit)
    }

    private suspend fun queryFollowStatus(currentUid: String, targetUid: String): Boolean {
        return supabaseClient.from("follows").select {
            filter {
                eq("follower_uid", currentUid)
                eq("followed_uid", targetUid)
            }
            limit(1)
        }.decodeList<SupabaseFollowRow>().isNotEmpty()
    }

    private suspend fun applyAuthorStatus(posts: List<CommunityPost>): List<CommunityPost> {
        if (posts.isEmpty()) return posts
        
        return try {
            val authorUids = posts.map { it.authorUid }.distinct()
            val profiles = supabaseClient.from("profiles_public_view").select {
                filter { isIn("firebase_uid", authorUids) }
            }.decodeList<SupabaseProfileRow>().associateBy { it.firebaseUid }
            
            posts.map { post ->
                val profile = profiles[post.authorUid]
                if (profile != null) {
                    post.copy(
                        authorIsAdmin = profile.isAdmin,
                        authorIsMod = profile.isMod
                    )
                } else {
                    post
                }
            }
        } catch (e: Exception) {
            Log.e("CommunityRepo", "applyAuthorStatus error", e)
            posts
        }
    }

    private suspend fun applyCommentAuthorStatus(comments: List<CommunityComment>): List<CommunityComment> {
        if (comments.isEmpty()) return comments
        
        return try {
            val authorUids = comments.map { it.authorUid }.distinct()
            val profiles = supabaseClient.from("profiles_public_view").select {
                filter { isIn("firebase_uid", authorUids) }
            }.decodeList<SupabaseProfileRow>().associateBy { it.firebaseUid }
            
            comments.map { comment ->
                val profile = profiles[comment.authorUid]
                if (profile != null) {
                    comment.copy(
                        authorIsAdmin = profile.isAdmin,
                        authorIsMod = profile.isMod
                    )
                } else {
                    comment
                }
            }
        } catch (e: Exception) {
            Log.e("CommunityRepo", "applyCommentAuthorStatus error", e)
            comments
        }
    }

    // ── Notifications ─────────────────────────────────────

    override suspend fun getNotifications(): Result<List<com.taytek.basehw.domain.model.CommunityNotification>> {
        val uid = currentUid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val notifications = supabaseClient.from("notifications").select {
                filter { eq("recipient_uid", uid) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(50)
            }.decodeList<com.taytek.basehw.data.remote.supabase.model.SupabaseNotificationRow>()

            val sendersUids = notifications.mapNotNull { it.senderUid }.distinct()
            val profiles = if (sendersUids.isNotEmpty()) {
                supabaseClient.from("profiles_public_view").select {
                    filter { isIn("firebase_uid", sendersUids) }
                }.decodeList<SupabaseProfileRow>().associateBy { it.firebaseUid }
            } else {
                emptyMap()
            }

            val result = notifications.map { row ->
                val profile = row.senderUid?.let { profiles[it] }
                com.taytek.basehw.domain.model.CommunityNotification(
                    id = row.id,
                    recipientUid = row.recipientUid,
                    senderUid = row.senderUid,
                    type = row.type,
                    message = row.message,
                    isRead = row.isRead,
                    createdAt = java.time.Instant.parse(row.createdAt),
                    senderQueryData = profile?.let { p ->
                        com.taytek.basehw.domain.model.SenderQueryData(
                            username = p.usernameLower ?: p.displayName,
                            avatarUrl = p.customAvatarUrl ?: p.photoUrl,
                            selectedAvatarId = p.selectedAvatarId
                        )
                    }
                )
            }
            Result.success(result)
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepo", "getNotifications error", e)
            Result.failure(e)
        }
    }

    override suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not logged in"))
        return try {
            supabaseClient.from("notifications").update(
                {
                    set("is_read", true)
                }
            ) {
                filter {
                    eq("id", notificationId)
                    eq("recipient_uid", uid)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not logged in"))
        return try {
            supabaseClient.from("notifications").delete {
                filter {
                    eq("id", notificationId)
                    eq("recipient_uid", uid)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadNotificationCount(): Result<Int> {
        val uid = currentUid ?: return Result.failure(Exception("Not logged in"))
        return try {
            // Using a simple list decode since API count behavior varies, count queries are better but this ensures type safety in current jan SDK
            val unreadRows = supabaseClient.from("notifications").select {
                filter {
                    eq("recipient_uid", uid)
                    eq("is_read", false)
                }
            }.decodeList<com.taytek.basehw.data.remote.supabase.model.SupabaseNotificationRow>()
            Result.success(unreadRows.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
