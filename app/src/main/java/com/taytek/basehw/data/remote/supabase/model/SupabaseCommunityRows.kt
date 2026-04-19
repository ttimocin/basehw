package com.taytek.basehw.data.remote.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseCommunityPostRow(
    val id: String = "",
    @SerialName("author_uid") val authorUid: String = "",
    @SerialName("author_username") val authorUsername: String = "User",
    @SerialName("author_is_admin") val authorIsAdmin: Boolean? = false,
    @SerialName("author_is_mod") val authorIsMod: Boolean? = false,
    @SerialName("author_selected_avatar_id") val authorSelectedAvatarId: Int? = 1,
    @SerialName("author_custom_avatar_url") val authorCustomAvatarUrl: String? = null,
    @SerialName("car_model_name") val carModelName: String? = null,
    @SerialName("car_brand") val carBrand: String? = null,
    @SerialName("car_year") val carYear: Int? = null,
    @SerialName("car_series") val carSeries: String? = null,
    @SerialName("car_image_url") val carImageUrl: String? = null,
    val caption: String? = null,
    @SerialName("car_feature") val carFeature: String? = null,
    @SerialName("like_count") val likeCount: Int? = 0, // backward compat
    @SerialName("comment_count") val commentCount: Int? = 0,
    @SerialName("reaction_counts") val reactionCounts: kotlinx.serialization.json.JsonObject? = null,
    @SerialName("is_active") val isActive: Boolean? = true,
    @SerialName("car_image_urls") val carImageUrls: List<String>? = emptyList(),
    @SerialName("created_at") val createdAt: String? = ""
)

@Serializable
data class SupabaseCommunityPostInsertRow(
    @SerialName("author_uid") val authorUid: String,
    @SerialName("author_username") val authorUsername: String,
    @SerialName("author_is_admin") val authorIsAdmin: Boolean = false,
    @SerialName("author_is_mod") val authorIsMod: Boolean = false,
    @SerialName("author_selected_avatar_id") val authorSelectedAvatarId: Int = 1,
    @SerialName("author_custom_avatar_url") val authorCustomAvatarUrl: String? = null,
    @SerialName("car_model_name") val carModelName: String,
    @SerialName("car_brand") val carBrand: String,
    @SerialName("car_year") val carYear: Int? = null,
    @SerialName("car_series") val carSeries: String? = null,
    @SerialName("car_image_url") val carImageUrl: String,
    val caption: String,
    @SerialName("car_feature") val carFeature: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class SupabaseCommunityLikeRow(
    @SerialName("post_id") val postId: String,
    @SerialName("user_uid") val userUid: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseCommunityReactionRow(
    @SerialName("post_id") val postId: String,
    @SerialName("user_uid") val userUid: String,
    val emoji: String = "👍",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseCommunityCommentRow(
    val id: String,
    @SerialName("post_id") val postId: String,
    @SerialName("author_uid") val authorUid: String,
    @SerialName("author_username") val authorUsername: String,
    @SerialName("author_is_admin") val authorIsAdmin: Boolean = false,
    @SerialName("author_is_mod") val authorIsMod: Boolean = false,
    val text: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class SupabaseCommunityCommentInsertRow(
    @SerialName("post_id") val postId: String,
    @SerialName("author_uid") val authorUid: String,
    @SerialName("author_username") val authorUsername: String,
    @SerialName("author_is_admin") val authorIsAdmin: Boolean = false,
    @SerialName("author_is_mod") val authorIsMod: Boolean = false,
    val text: String
)

@Serializable
data class SupabaseIdOnlyRow(
    val id: String
)

@Serializable
data class SupabaseSetModeratorFirebaseRequest(
    @SerialName("target_firebase_uid") val targetFirebaseUid: String,
    @SerialName("mod_status") val modStatus: Boolean
)

@Serializable
data class SupabaseCreateCommunityPostRequest(
    @SerialName("p_author_uid") val authorUid: String,
    @SerialName("p_author_username") val authorUsername: String,
    @SerialName("p_author_is_admin") val authorIsAdmin: Boolean = false,
    @SerialName("p_author_is_mod") val authorIsMod: Boolean = false,
    @SerialName("p_author_selected_avatar_id") val authorSelectedAvatarId: Int = 1,
    @SerialName("p_author_custom_avatar_url") val authorCustomAvatarUrl: String? = null,
    @SerialName("p_car_model_name") val carModelName: String,
    @SerialName("p_car_brand") val carBrand: String,
    @SerialName("p_car_year") val carYear: Int? = null,
    @SerialName("p_car_series") val carSeries: String? = null,
    @SerialName("p_car_image_url") val carImageUrl: String,
    @SerialName("p_caption") val caption: String,
    @SerialName("p_car_feature") val carFeature: String? = null,
    @SerialName("p_car_image_urls") val carImageUrls: List<String> = emptyList()
)

@Serializable
data class SupabaseCreateCommunityCommentRequest(
    @SerialName("p_post_id") val postId: String,
    @SerialName("p_author_uid") val authorUid: String,
    @SerialName("p_author_username") val authorUsername: String,
    @SerialName("p_author_is_admin") val authorIsAdmin: Boolean = false,
    @SerialName("p_author_is_mod") val authorIsMod: Boolean = false,
    @SerialName("p_text") val text: String
)
