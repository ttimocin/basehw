package com.taytek.basehw.domain.repository

import com.taytek.basehw.domain.model.CommunityComment
import com.taytek.basehw.domain.model.CommunityPost
import com.taytek.basehw.domain.model.User

interface CommunityRepository {
    suspend fun createPost(
        carModelName: String,
        carBrand: String,
        carYear: Int?,
        carSeries: String?,
        carImageUrl: String,
        caption: String
    ): Result<String>

    suspend fun deletePost(postId: String): Result<Unit>

    suspend fun getFeedPosts(limit: Int = 20): Result<List<CommunityPost>>

    suspend fun getFollowingPosts(limit: Int = 20): Result<List<CommunityPost>>

    suspend fun getUserPosts(uid: String, limit: Int = 20): Result<List<CommunityPost>>

    suspend fun toggleLike(postId: String): Result<Boolean>

    suspend fun addComment(postId: String, text: String): Result<CommunityComment>

    suspend fun getComments(postId: String): Result<List<CommunityComment>>

    suspend fun followUser(targetUid: String): Result<Unit>

    suspend fun unfollowUser(targetUid: String): Result<Unit>

    suspend fun isFollowing(targetUid: String): Result<Boolean>

    suspend fun getUserProfile(uid: String): Result<User>

    suspend fun getFollowingUids(): Result<List<String>>

    suspend fun getTopUsers(limit: Int = 20): Result<List<User>>
}
