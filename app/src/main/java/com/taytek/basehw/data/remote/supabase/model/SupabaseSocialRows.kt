package com.taytek.basehw.data.remote.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseProfileRow(
    @SerialName("firebase_uid") val firebaseUid: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("username_lower") val usernameLower: String? = null,
    @SerialName("google_username_onboarding_required") val googleUsernameOnboardingRequired: Boolean = false,
    @SerialName("google_username_onboarding_completed") val googleUsernameOnboardingCompleted: Boolean = false,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_mod") val isMod: Boolean = false,
    @SerialName("follower_count") val followerCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("post_count") val postCount: Int = 0,
    @SerialName("rules_accepted") val rulesAccepted: Boolean = false,
    @SerialName("privacy_accepted") val privacyAccepted: Boolean = false,
    @SerialName("collection_public") val collectionPublic: Boolean = false,
    @SerialName("wishlist_public") val wishlistPublic: Boolean = false,
    @SerialName("selected_avatar_id") val selectedAvatarId: Int? = null,
    @SerialName("custom_avatar_url") val customAvatarUrl: String? = null
)

@Serializable
data class SupabaseFollowRow(
    @SerialName("follower_uid") val followerUid: String,
    @SerialName("followed_uid") val followedUid: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabasePublicListingRow(
    @SerialName("firebase_uid") val firebaseUid: String,
    @SerialName("listing_id") val listingId: String,
    @SerialName("title") val title: String,
    @SerialName("image_url") val imageUrl: String?
)

@Serializable
data class SupabasePublicListingReadRow(
    @SerialName("title") val title: String
)

@Serializable
data class SupabaseMessageRow(
    @SerialName("firebase_uid") val firebaseUid: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("receiver_uid") val receiverUid: String,
    @SerialName("message_body") val messageBody: String
)

@Serializable
data class SupabaseMessageReadRow(
    @SerialName("id") val id: String,
    @SerialName("firebase_uid") val firebaseUid: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("receiver_uid") val receiverUid: String,
    @SerialName("message_body") val messageBody: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class SupabaseBlockRow(
    @SerialName("blocker_uid") val blockerUid: String,
    @SerialName("blocked_uid") val blockedUid: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseBannedUserRow(
    val id: String? = null,
    @SerialName("user_uid") val userUid: String,
    @SerialName("banned_by_uid") val bannedByUid: String,
    val reason: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseNotificationRow(
    @SerialName("id") val id: String,
    @SerialName("recipient_uid") val recipientUid: String,
    @SerialName("sender_uid") val senderUid: String? = null,
    @SerialName("type") val type: String,
    @SerialName("message") val message: String,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String
)
