package com.taytek.basehw.data.remote.firebase

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.taytek.basehw.data.remote.network.SupabaseStorageDataSource
import com.taytek.basehw.data.remote.supabase.model.SupabaseFeedbackInsertRow
import com.taytek.basehw.data.remote.supabase.model.SupabasePublicListingRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseCollectionSnapshotRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseCommunityPostRow
import com.taytek.basehw.data.remote.supabase.model.SupabaseProfileRow
import com.taytek.basehw.domain.repository.AuthRepository
import com.taytek.basehw.ui.util.AvatarUtil
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val supabaseClient: SupabaseClient,
    private val supabaseStorageDataSource: SupabaseStorageDataSource
) : AuthRepository {

    private val _userFlow = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    override val currentUserFlow: Flow<FirebaseUser?> = _userFlow.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _userFlow.value = firebaseAuth.currentUser
        }
    }

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun signInWithGoogle(idToken: String): Result<com.taytek.basehw.domain.model.User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                // For Google, if we have to create a profile, we want onboarding
                val createdProfileNow = ensureProfileDefaults(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    preferredUsername = firebaseUser.displayName,
                    forceOnboarding = true 
                )
                val profile = fetchProfile(firebaseUser.uid)
                val onboardingRequired =
                    (profile?.googleUsernameOnboardingRequired == true) ||
                        (createdProfileNow && profile?.googleUsernameOnboardingCompleted != true)
            Result.success(com.taytek.basehw.domain.model.User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    username = profile?.displayName,
                    googleUsernameOnboardingRequired = onboardingRequired,
                    googleUsernameOnboardingCompleted = profile?.googleUsernameOnboardingCompleted ?: false,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    isCollectionPublic = profile?.collectionPublic ?: false,
                    isWishlistPublic = profile?.wishlistPublic ?: false,
                    selectedAvatarId = profile?.selectedAvatarId ?: 1,
                    customAvatarUrl = profile?.customAvatarUrl,
                    isAdmin = profile?.isAdmin ?: false,
                    isMod = profile?.isMod ?: false,
                    rulesAccepted = profile?.rulesAccepted ?: false,
                    privacyAccepted = profile?.privacyAccepted ?: false
                ))
            } else {
                Result.failure(Exception("Kullanıcı bilgisi alınamadı."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<com.taytek.basehw.domain.model.User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                ensureProfileDefaults(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    forceOnboarding = false // Email users have usernames
                )
                val profile = fetchProfile(firebaseUser.uid)
                Result.success(com.taytek.basehw.domain.model.User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    username = profile?.displayName,
                    googleUsernameOnboardingRequired = profile?.googleUsernameOnboardingRequired ?: false,
                    googleUsernameOnboardingCompleted = profile?.googleUsernameOnboardingCompleted ?: false,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    isCollectionPublic = profile?.collectionPublic ?: false,
                    isWishlistPublic = profile?.wishlistPublic ?: false,
                    selectedAvatarId = profile?.selectedAvatarId ?: 1,
                    customAvatarUrl = profile?.customAvatarUrl,
                    isAdmin = profile?.isAdmin ?: false,
                    isMod = profile?.isMod ?: false,
                    rulesAccepted = profile?.rulesAccepted ?: false,
                    privacyAccepted = profile?.privacyAccepted ?: false
                ))
            } else {
                Result.failure(Exception("Giriş başarısız."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, username: String): Result<com.taytek.basehw.domain.model.User> {
        return try {
            // 1. Check uniqueness again just in case
            val available = checkUsernameAvailable(username).getOrDefault(false)
            if (!available) return Result.failure(Exception("Bu kullanıcı adı zaten alınmış."))

            // 2. Create Auth user
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Kayıt başarısız."))

            // 3. Save to Supabase profile
            val normalized = normalizeUsername(username)
            val randomAvatarId = (1..AvatarUtil.getAvatarCount()).random()
            supabaseClient.from("profiles").upsert(
                SupabaseProfileRow(
                    firebaseUid = firebaseUser.uid,
                    displayName = username,
                    usernameLower = normalized,
                    email = email,
                    photoUrl = null,
                    collectionPublic = false,
                    wishlistPublic = false,
                    rulesAccepted = false,
                    privacyAccepted = false,
                    selectedAvatarId = randomAvatarId
                )
            )

            Result.success(com.taytek.basehw.domain.model.User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                username = username,
                googleUsernameOnboardingRequired = false,
                googleUsernameOnboardingCompleted = false,
                photoUrl = null,
                isCollectionPublic = false,
                isWishlistPublic = false,
                selectedAvatarId = randomAvatarId
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(): Result<com.taytek.basehw.domain.model.User> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum açılmamış."))
        return try {
            ensureProfileDefaults(
                uid = user.uid,
                email = user.email,
                photoUrl = user.photoUrl?.toString(),
                forceOnboarding = false
            )
            val profile = fetchProfile(user.uid)
            Result.success(com.taytek.basehw.domain.model.User(
                uid = user.uid,
                email = user.email ?: "",
                username = profile?.displayName,
                googleUsernameOnboardingRequired = profile?.googleUsernameOnboardingRequired ?: false,
                googleUsernameOnboardingCompleted = profile?.googleUsernameOnboardingCompleted ?: false,
                photoUrl = user.photoUrl?.toString(),
                isCollectionPublic = profile?.collectionPublic ?: false,
                isWishlistPublic = profile?.wishlistPublic ?: false,
                selectedAvatarId = profile?.selectedAvatarId ?: 1,
                customAvatarUrl = profile?.customAvatarUrl,
                isAdmin = profile?.isAdmin ?: false,
                isMod = profile?.isMod ?: false,
                rulesAccepted = profile?.rulesAccepted ?: false,
                privacyAccepted = profile?.privacyAccepted ?: false
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateVisibilitySettings(collectionPublic: Boolean, wishlistPublic: Boolean): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum açılmamış."))
        return try {
            supabaseClient.from("profiles").update({
                set("collection_public", collectionPublic)
                set("wishlist_public", wishlistPublic)
            }) {
                filter { eq("firebase_uid", user.uid) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkUsernameAvailable(username: String): Result<Boolean> {
        return try {
            val normalized = normalizeUsername(username)
            val rows = supabaseClient.from("profiles").select {
                filter { eq("username_lower", normalized) }
            }.decodeList<SupabaseProfileRow>()

            val currentUid = auth.currentUser?.uid
            val takenByOtherUser = rows.any { it.firebaseUid != currentUid }
            Result.success(!takenByOtherUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUsername(username: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum açılmamış."))
        return try {
            val normalized = normalizeUsername(username)
            val profile = fetchProfile(user.uid)
            if (profile?.usernameLower != normalized) {
                val available = checkUsernameAvailable(username).getOrDefault(false)
                if (!available) return Result.failure(Exception("Bu kullanıcı adı zaten alınmış."))
            }
            supabaseClient.from("profiles").update({
                set("display_name", username)
                set("username_lower", normalized)
                set("email", user.email ?: "")
                set("photo_url", user.photoUrl?.toString())
            }) {
                filter { eq("firebase_uid", user.uid) }
            }

            // Keep denormalized author names in sync for already-created content.
            runCatching {
                supabaseClient.from("community_posts").update({
                    set("author_username", username)
                }) {
                    filter { eq("author_uid", user.uid) }
                }
            }

            runCatching {
                supabaseClient.from("community_comments").update({
                    set("author_username", username)
                }) {
                    filter { eq("author_uid", user.uid) }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeGoogleUsernameOnboarding(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum açılmamış."))
        return try {
            supabaseClient.from("profiles").update({
                set("google_username_onboarding_required", false)
                set("google_username_onboarding_completed", true)
            }) {
                filter { eq("firebase_uid", user.uid) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchProfile(uid: String): SupabaseProfileRow? {
        return supabaseClient.from("profiles").select {
            filter { eq("firebase_uid", uid) }
            limit(1)
        }.decodeSingleOrNull<SupabaseProfileRow>()
    }

    private suspend fun ensureProfileDefaults(
        uid: String,
        email: String?,
        photoUrl: String?,
        preferredUsername: String? = null,
        forceOnboarding: Boolean = false
    ): Boolean {
        val existing = fetchProfile(uid)
        if (existing != null) {
            val normalizedDisplayName = normalizeUsername(existing.displayName)
            val nextDisplayName = when {
                !existing.usernameLower.isNullOrBlank() && normalizedDisplayName.isNotBlank() && normalizedDisplayName != existing.usernameLower -> existing.usernameLower
                !existing.displayName.isNullOrBlank() -> existing.displayName
                !existing.usernameLower.isNullOrBlank() -> existing.usernameLower
                !preferredUsername.isNullOrBlank() -> preferredUsername
                !email.isNullOrBlank() -> email.substringBefore("@")
                else -> "user_${uid.take(8)}"
            }
            val nextUsernameLower = existing.usernameLower ?: normalizeUsername(nextDisplayName)
            val needsUpdate = existing.displayName != nextDisplayName ||
                              existing.usernameLower != nextUsernameLower ||
                              (email != null && existing.email != email) ||
                              (photoUrl != null && existing.photoUrl != photoUrl)
            
            if (needsUpdate) {
                runCatching {
                    supabaseClient.from("profiles").update({
                        set("display_name", nextDisplayName)
                        set("username_lower", nextUsernameLower)
                        set("email", email ?: existing.email ?: "")
                        set("photo_url", photoUrl ?: existing.photoUrl)
                        set("privacy_accepted", existing.privacyAccepted)
                        set("collection_public", existing.collectionPublic)
                        set("wishlist_public", existing.wishlistPublic)
                    }) {
                        filter { eq("firebase_uid", uid) }
                    }
                }
            }
            // Best-effort for environments where onboarding columns exist.
            runCatching {
                supabaseClient.from("profiles").update({
                    set("google_username_onboarding_required", existing.googleUsernameOnboardingRequired)
                    set("google_username_onboarding_completed", existing.googleUsernameOnboardingCompleted)
                }) {
                    filter { eq("firebase_uid", uid) }
                }
            }
            return false
        }

        val generatedName = preferredUsername
            ?: email?.substringBefore("@")
            ?: "user_${uid.take(8)}"
        
        val randomAvatarId = (1..AvatarUtil.getAvatarCount()).random()
        
        runCatching {
            supabaseClient.from("profiles").upsert(
                buildJsonObject {
                    put("firebase_uid", uid)
                    put("display_name", generatedName)
                    put("username_lower", normalizeUsername(generatedName))
                    if (!email.isNullOrBlank()) put("email", email)
                    if (!photoUrl.isNullOrBlank()) put("photo_url", photoUrl)
                    put("collection_public", false)
                    put("wishlist_public", false)
                    put("rules_accepted", false)
                    put("privacy_accepted", false)
                    put("selected_avatar_id", randomAvatarId)
                }
            )
        }
        // Best-effort for environments where onboarding columns exist.
        runCatching {
            supabaseClient.from("profiles").update({
                set("google_username_onboarding_required", forceOnboarding)
                set("google_username_onboarding_completed", !forceOnboarding)
            }) {
                filter { eq("firebase_uid", uid) }
            }
        }
        return true
    }

    private fun normalizeUsername(username: String?): String {
        return username?.trim()?.lowercase() ?: ""
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reloadUser(): Result<FirebaseUser?> {
        return try {
            auth.currentUser?.reload()?.await()
            val user = auth.currentUser
            _userFlow.value = user // Trigger update for all listeners
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInAnonymously(): Result<com.taytek.basehw.domain.model.User> {
        return try {
            val result = auth.signInAnonymously().await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                Result.success(com.taytek.basehw.domain.model.User(
                    uid = firebaseUser.uid,
                    email = "",
                    username = null
                ))
            } else {
                Result.failure(Exception("Anonim giriş başarısız."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptPrivacyTerms(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum açılmamış."))
        return try {
            supabaseClient.from("profiles").update({
                set("privacy_accepted", true)
            }) {
                filter { eq("firebase_uid", user.uid) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı bulunamadı."))
            val uid = user.uid
            val knownImageUrls = collectUserUploadedImageUrls(uid)
            val storageDeleteResult = runCatching {
                val explicitDeleteOk = supabaseStorageDataSource.deleteByPublicUrls(knownImageUrls)
                val prefixDeleteOk = supabaseStorageDataSource.deleteAllForUserPrefix(uid)
                explicitDeleteOk && prefixDeleteOk
            }.getOrDefault(false)

            val cleanupResult = runCatching {
                supabaseClient.postgrest.rpc(
                    "delete_my_account_data",
                    buildJsonObject { put("p_uid", uid) }
                )
            }

            if (!storageDeleteResult || cleanupResult.isFailure) {
                android.util.Log.e(
                    "AuthRepository",
                    "Remote cleanup failed before Firebase delete. storageOk=$storageDeleteResult rpcError=${cleanupResult.exceptionOrNull()?.message}",
                    cleanupResult.exceptionOrNull()
                )
                return Result.failure(
                    Exception(
                        "REMOTE_CLEANUP_FAILED:" + (
                            cleanupResult.exceptionOrNull()?.message
                                ?: "storage_cleanup_failed"
                            )
                    )
                )
            }

            user.delete().await()
            auth.signOut() // Success deletion implicitly signs out, but explicit call ensures state update
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            // This is the most common reason for failure in sensitive operations
            Result.failure(Exception("REAUTH_REQUIRED"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendFeedback(feedback: com.taytek.basehw.domain.model.Feedback): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı bulunamadı."))
            supabaseClient.from("feedback_messages").insert(
                SupabaseFeedbackInsertRow(
                    firebaseUid = user.uid,
                    username = feedback.username,
                    subject = feedback.subject,
                    message = feedback.message
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfileAvatar(avatarId: Int, customAvatarUrl: String?): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum açılmamış."))
        return try {
            supabaseClient.from("profiles").update({
                set("selected_avatar_id", avatarId)
                // avatarId != 0 ise custom URL'yi temizle, avatarId == 0 ise set et
                if (avatarId != 0) {
                    set("custom_avatar_url", null as String?)
                } else if (customAvatarUrl != null) {
                    set("custom_avatar_url", customAvatarUrl)
                }
            }) {
                filter { eq("firebase_uid", user.uid) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentCustomAvatarUrl(): Result<String?> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum açılmamış."))
        return try {
            val profile = fetchProfile(user.uid)
            Result.success(profile?.customAvatarUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadCustomAvatar(uri: Uri): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum açılmamış."))
        return try {
            // SupabaseStorageDataSource doğrudan ViewModel'den çağrılacak
            // Burada sadece profile tablosunu güncelliyoruz
            // URL, ViewModel'den gelecek
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun collectUserUploadedImageUrls(uid: String): Set<String> {
        val urls = linkedSetOf<String>()

        runCatching {
            val profile = fetchProfile(uid)
            profile?.customAvatarUrl?.let(urls::add)
        }

        runCatching {
            supabaseClient.from("public_listings").select {
                filter { eq("firebase_uid", uid) }
            }.decodeList<SupabasePublicListingRow>()
                .mapNotNullTo(urls) { it.imageUrl }
        }

        runCatching {
            supabaseClient.from("community_posts").select {
                filter { eq("author_uid", uid) }
            }.decodeList<SupabaseCommunityPostRow>()
                .forEach { post ->
                    post.carImageUrl?.let(urls::add)
                    post.authorCustomAvatarUrl?.let(urls::add)
                    post.carImageUrls?.filterNotNull()?.forEach(urls::add)
                }
        }

        runCatching {
            supabaseClient.from("user_collection_snapshots").select {
                filter { eq("firebase_uid", uid) }
            }.decodeList<SupabaseCollectionSnapshotRow>()
                .forEach { snapshot ->
                    extractUrlsFromJson(snapshot.payloadText).forEach(urls::add)
                }
        }

        return urls
    }

    private fun extractUrlsFromJson(payload: String): Set<String> {
        return runCatching {
            val root = Json.parseToJsonElement(payload)
            val urls = linkedSetOf<String>()
            collectUrlsRecursive(root, urls)
            urls
        }.getOrDefault(emptySet())
    }

    private fun collectUrlsRecursive(element: JsonElement, urls: MutableSet<String>) {
        when (element) {
            is JsonObject -> element.values.forEach { value -> collectUrlsRecursive(value, urls) }
            is JsonArray -> element.forEach { value -> collectUrlsRecursive(value, urls) }
            is JsonPrimitive -> {
                if (element.isString) {
                    val value = element.content
                    if (value.contains("/storage/v1/object/public/")) {
                        urls.add(value)
                    }
                }
            }
        }
    }
}
