package com.taytek.basehw.domain.repository

import com.taytek.basehw.domain.model.CommunityComment
import com.taytek.basehw.domain.model.CommunityPost
import com.taytek.basehw.domain.model.User
import kotlinx.coroutines.flow.Flow

interface CommunityRepository {
    suspend fun createPost(
        carModelName: String,
        carBrand: String,
        carYear: Int?,
        carSeries: String?,
        carImageUrl: String,
        caption: String,
        carFeature: String?,
        authorSelectedAvatarId: Int = 1,
        authorCustomAvatarUrl: String? = null,
        carImageUrls: List<String> = emptyList()
    ): Result<String>

    suspend fun deletePost(postId: String): Result<Unit>

    suspend fun getFeedPosts(limit: Int = 20, lastTimestamp: Long? = null): Result<List<CommunityPost>>

    suspend fun getFollowingPosts(limit: Int = 20): Result<List<CommunityPost>>

    suspend fun getUserPosts(uid: String, limit: Int = 20): Result<List<CommunityPost>>

    suspend fun toggleLike(postId: String): Result<Boolean>

    /**
     * Emoji reaksiyon ekle/değiştir/kaldır.
     * Aynı emoji tekrar gönderilirse reaksiyon kaldırılır (toggle).
     * Farklı emoji gönderilirse reaksiyon güncellenir.
     * @return (isNowReacting, currentEmoji) — isNowReacting=false ise currentEmoji null
     */
    suspend fun toggleReaction(postId: String, emoji: String): Result<Pair<Boolean, String?>>

    suspend fun addComment(postId: String, text: String): Result<CommunityComment>

    suspend fun getComments(postId: String): Result<List<CommunityComment>>

    suspend fun followUser(targetUid: String): Result<Unit>

    suspend fun unfollowUser(targetUid: String): Result<Unit>

    suspend fun isFollowing(targetUid: String): Result<Boolean>

    fun observeFollowStatus(currentUid: String, targetUid: String): Flow<Boolean>

    suspend fun getUserProfile(uid: String): Result<User>

    suspend fun getFollowingUids(): Result<List<String>>

    suspend fun getFollowersUsers(uid: String): Result<List<User>>

    suspend fun getFollowingUsers(uid: String): Result<List<User>>

    suspend fun getTopUsers(limit: Int = 20): Result<List<User>>

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>

    suspend fun acceptRules(): Result<Unit>

    suspend fun blockFollower(targetUid: String): Result<Unit>

    suspend fun getBlockedUsers(): Result<List<User>>

    suspend fun unblockUser(targetUid: String): Result<Unit>
    
    /**
     * Check if a specific user has blocked the current user
     */
    suspend fun hasUserBlockedMe(targetUid: String): Result<Boolean>

    // ── Admin Panel Functions ───────────────────────────────

    suspend fun setModerator(targetUid: String, isMod: Boolean): Result<Unit>

    suspend fun banUserFromForum(targetUid: String, reason: String? = null): Result<Unit>

    suspend fun unbanUserFromForum(targetUid: String): Result<Unit>

    suspend fun getBannedUsers(): Result<List<User>>

    suspend fun isUserBannedFromForum(uid: String): Result<Boolean>
    
    suspend fun checkIsAdmin(uid: String): Result<Boolean>
    
    suspend fun getAllUsers(): Result<List<User>>

    // ── Notifications ───────────────────────────────────────

    suspend fun getNotifications(): Result<List<com.taytek.basehw.domain.model.CommunityNotification>>
    
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>
    
    suspend fun deleteNotification(notificationId: String): Result<Unit>

    suspend fun getUnreadNotificationCount(): Result<Int>
}