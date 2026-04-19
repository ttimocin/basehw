package com.taytek.basehw.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.google.firebase.auth.FirebaseAuth
import com.taytek.basehw.domain.model.BadgeType
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.CollectionRankCalculator
import com.taytek.basehw.domain.model.CommunityComment
import com.taytek.basehw.domain.model.CommunityPost
import com.taytek.basehw.domain.model.Feedback
import com.taytek.basehw.domain.model.User
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.model.VehicleCondition
import com.taytek.basehw.domain.model.fromInputs
import com.taytek.basehw.domain.model.RankCarInput
import com.taytek.basehw.domain.model.SortOrder
import com.taytek.basehw.domain.repository.AuthRepository
import com.taytek.basehw.domain.repository.CommunityRepository
import com.taytek.basehw.domain.repository.SupabaseSyncRepository
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class CollectionItem(
    val title: String,
    val imageUrl: String? = null
)

data class CommunityUiState(
    val feedPosts: List<CommunityPost> = emptyList(),
    val followingPosts: List<CommunityPost> = emptyList(),
    val isLoadingFeed: Boolean = false,
    val isLoadingFollowing: Boolean = false,
    val isSignedIn: Boolean = false,
    val isEmailVerified: Boolean = false,
    val currentUserUid: String? = null,
    val error: String? = null,

    // Comments
    val activePostComments: List<CommunityComment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val activeCommentPostId: String? = null,

    // User Profile
    val profileUser: User? = null,
    val profilePosts: List<CommunityPost> = emptyList(),
    val profileFollowers: List<User> = emptyList(),
    val profileFollowing: List<User> = emptyList(),
    val isLoadingFollowUsers: Boolean = false,
    val isFollowingProfile: Boolean = false,
    val isFollowActionLoading: Boolean = false,
    val isLoadingProfile: Boolean = false,
    val profileAtakasCount: Int = 0,
    val profileAtakasPreview: List<String> = emptyList(),
    val profileReverseAtakasCount: Int = 0,
    val profileReverseAtakasPreview: List<String> = emptyList(),
    val profileCollectionTitles: List<String> = emptyList(),
    val profileWishlistTitles: List<String> = emptyList(),
    val profileCollectionItems: List<CollectionItem> = emptyList(),

    // Leaderboard
    val topUsers: List<User> = emptyList(),
    val isLoadingLeaderboard: Boolean = false,
    val showRulesDialog: Boolean = false,
    val pendingCommentText: String? = null,
    val pendingPostCaption: String? = null,
    val pendingPostCar: UserCar? = null,
    val currentUser: User? = null,
    val currentUserRankScore: Double = 0.0,
    val currentUserRankBadge: BadgeType = BadgeType.ROOKIE,

    // Pagination
    val isFeedEndReached: Boolean = false,
    val isLoadingNextPage: Boolean = false,

    // Author avatars cache: uid -> (selectedAvatarId, customAvatarUrl)
    val authorAvatars: Map<String, Pair<Int, String?>> = emptyMap(),

    // Feed composer
    val isCreatingPost: Boolean = false,
    val createPostError: String? = null,
    val postCreateSuccessNonce: Int = 0,

    // Inbox
    val hasUnreadInboxMessages: Boolean = false,
    val hasUnreadNotifications: Boolean = false,

    // Blocked users
    val blockedUsers: List<User> = emptyList(),
    val isLoadingBlockedUsers: Boolean = false,

    // Admin Panel
    val allUsers: List<User> = emptyList(),
    val isLoadingAllUsers: Boolean = false,
    val bannedUsers: List<User> = emptyList(),
    val isLoadingBannedUsers: Boolean = false,
    val isAdminPanelLoading: Boolean = false,
    val adminPanelError: String? = null,
    val isAdminOrMod: Boolean = false,

    /** Incremented when a community post report is sent to feedback_messages (for UI snackbar). */
    val reportPostSuccessNonce: Int = 0
)

@HiltViewModel
class CommunityViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: CommunityRepository,
    private val authRepository: AuthRepository,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager,
    private val userCarRepository: UserCarRepository,
    private val supabaseSyncRepository: SupabaseSyncRepository
) : ViewModel() {

    private fun getString(resId: Int): String {
        return androidx.core.content.ContextCompat.getString(context, resId)
    }

    private companion object {
        const val POST_LIMIT_COUNT = 2
        const val POST_LIMIT_WINDOW_MILLIS = 5 * 60 * 1000L
        const val COMMENT_LIMIT_COUNT = 5
        const val COMMENT_LIMIT_WINDOW_MILLIS = 60 * 1000L // 1 dakika
    }

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val userBadgeCache = mutableMapOf<String, BadgeType>()
    // UID → cached collection items; shown instantly on re-visit while fresh data loads
    private val collectionCache = mutableMapOf<String, List<CollectionItem>>()

    val collectionCarsPaged = userCarRepository
        .getCollection(sortOrder = SortOrder.DATE_ADDED_DESC)
        .cachedIn(viewModelScope)

    val wishlistCarsPaged = userCarRepository
        .getWishlist()
        .cachedIn(viewModelScope)

    private var inboxPollingJob: Job? = null
    private var followStatusJob: Job? = null

    init {
        observeAuthState()
        observeCurrentUserRank()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUserFlow.collect { user ->
                val signedIn = user != null
                
                // Google Play Store test hesabı için e-posta doğrulaması bypass'ı
                val isTestAccount = user?.email == "googletest@basehw.net"
                val verified = (user?.isEmailVerified == true) || isTestAccount
                
                _uiState.value = _uiState.value.copy(
                    isSignedIn = signedIn,
                    isEmailVerified = verified,
                    currentUserUid = user?.uid,
                    currentUser = null // Reset until fetched
                )
                if (!signedIn) {
                    stopInboxPolling()
                    clearFollowStatusObserver()
                    _uiState.value = _uiState.value.copy(hasUnreadInboxMessages = false)
                }
                if (signedIn && verified) {
                    loadCurrentUser(user.uid)
                    if (_uiState.value.feedPosts.isEmpty() && !_uiState.value.isLoadingFeed) {
                        loadFeed()
                        loadLeaderboard()
                    }
                    startInboxPolling()
                    refreshInboxUnreadState()
                    refreshNotificationsUnreadState()
                }
                
                // Also load current user on init if already signed in
                if (user != null && _uiState.value.currentUser == null) {
                    loadCurrentUser(user.uid)
                }
            }
        }
    }

    private fun observeCurrentUserRank() {
        viewModelScope.launch {
            userCarRepository.getRankCars()
                .distinctUntilChanged()
                .collectLatest { cars ->
                    val inputs = CollectionRankCalculator.calculate(cars)
                    _uiState.value = _uiState.value.copy(
                        currentUserRankScore = inputs.totalScore,
                        currentUserRankBadge = BadgeType.fromInputs(inputs)
                    )
                }
        }
    }

    fun refreshAuth() {
        viewModelScope.launch {
            authRepository.reloadUser()
        }
    }

    fun clearInboxUnreadState() {
        appSettingsManager.setCommunityInboxLastSeenAt(System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(hasUnreadInboxMessages = false)
    }

    fun refreshInboxUnreadState() {
        val myUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            supabaseSyncRepository.getInboxConversations(myUid).onSuccess { conversations ->
                val lastSeenAt = appSettingsManager.getCommunityInboxLastSeenAt()
                val hasUnread = conversations.any { conversation ->
                    conversation.lastMessageSenderUid != myUid && conversation.lastMessageAt > lastSeenAt
                }
                _uiState.value = _uiState.value.copy(hasUnreadInboxMessages = hasUnread)
            }
        }
    }

    fun refreshNotificationsUnreadState() {
        viewModelScope.launch {
            repository.getUnreadNotificationCount().onSuccess { count ->
                _uiState.value = _uiState.value.copy(hasUnreadNotifications = count > 0)
            }
        }
    }

    private fun startInboxPolling() {
        if (inboxPollingJob?.isActive == true) return
        inboxPollingJob = viewModelScope.launch {
            while (true) {
                refreshInboxUnreadState()
                refreshNotificationsUnreadState()
                delay(20_000L)
            }
        }
    }

    private fun stopInboxPolling() {
        inboxPollingJob?.cancel()
        inboxPollingJob = null
    }

    override fun onCleared() {
        stopInboxPolling()
        clearFollowStatusObserver()
        super.onCleared()
    }

    private fun observeFollowStatus(targetUid: String) {
        clearFollowStatusObserver()
        val myUid = authRepository.currentUser?.uid
        if (myUid.isNullOrBlank() || targetUid.isBlank() || myUid == targetUid) {
            _uiState.value = _uiState.value.copy(
                isFollowingProfile = false,
                isFollowActionLoading = false
            )
            return
        }

        followStatusJob = viewModelScope.launch {
            repository.observeFollowStatus(myUid, targetUid)
                .distinctUntilChanged()
                .collect { following ->
                    _uiState.value = _uiState.value.copy(
                        isFollowingProfile = following,
                        isFollowActionLoading = false
                    )
                }
        }
    }

    private fun clearFollowStatusObserver() {
        followStatusJob?.cancel()
        followStatusJob = null
    }

    fun loadFeed(isNextPage: Boolean = false) {
        if (isNextPage && (_uiState.value.isFeedEndReached || _uiState.value.isLoadingNextPage)) return
        
        if (isNextPage) {
            _uiState.value = _uiState.value.copy(isLoadingNextPage = true)
        } else {
            _uiState.value = _uiState.value.copy(isLoadingFeed = true, error = null, isFeedEndReached = false)
        }

        viewModelScope.launch {
            val lastTimestamp = if (isNextPage) _uiState.value.feedPosts.lastOrNull()?.createdAt else null
            val limit = 5
            
            repository.getFeedPosts(limit = limit, lastTimestamp = lastTimestamp).fold(
                onSuccess = { posts ->
                    val postsWithBadges = hydratePostBadges(posts)
                    val updatedPosts = if (isNextPage) _uiState.value.feedPosts + postsWithBadges else postsWithBadges
                    
                    // Load author avatars for ALL post authors (to get current avatar, not stored one)
                    val existingAvatars = _uiState.value.authorAvatars.toMutableMap()
                    val authorUids = updatedPosts
                        .map { it.authorUid }
                        .distinct()
                        .filter { !existingAvatars.containsKey(it) }
                    
                    authorUids.forEach { uid ->
                        repository.getUserProfile(uid).onSuccess { user ->
                            existingAvatars[uid] = Pair(user.selectedAvatarId, user.customAvatarUrl)
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        feedPosts = updatedPosts,
                        isLoadingFeed = false,
                        isLoadingNextPage = false,
                        isFeedEndReached = posts.size < limit,
                        authorAvatars = existingAvatars
                    )
                },
                onFailure = { e ->
                    android.util.Log.e("CommunityVM", "loadFeed FAILED: ${e.javaClass.simpleName}: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        error = mapGeneralError(e),
                        isLoadingFeed = false,
                        isLoadingNextPage = false
                    )
                }
            )
        }
    }

    fun loadNextPage() {
        loadFeed(isNextPage = true)
    }

    // Refresh author avatars without reloading posts - fetches current profile data for all post authors
    fun refreshAuthorAvatars() {
        val currentPosts = _uiState.value.feedPosts
        if (currentPosts.isEmpty()) return
        
        viewModelScope.launch {
            val authorUids = currentPosts.map { it.authorUid }.distinct()
            val updatedAvatars = _uiState.value.authorAvatars.toMutableMap()
            
            authorUids.forEach { uid ->
                // Always refresh to get the latest avatar data
                repository.getUserProfile(uid).onSuccess { user ->
                    updatedAvatars[uid] = Pair(user.selectedAvatarId, user.customAvatarUrl)
                }
            }
            
            _uiState.value = _uiState.value.copy(authorAvatars = updatedAvatars)
        }
    }

    fun loadFollowing() {
        _uiState.value = _uiState.value.copy(isLoadingFollowing = true)
        viewModelScope.launch {
            repository.getFollowingPosts().fold(
                onSuccess = { posts ->
                    _uiState.value = _uiState.value.copy(
                        followingPosts = hydratePostBadges(posts),
                        isLoadingFollowing = false
                    )
                },
                onFailure = { e ->
                    android.util.Log.e("CommunityVM", "loadFollowing FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(error = mapGeneralError(e), isLoadingFollowing = false)
                }
            )
        }
    }

    fun toggleLike(postId: String) {
        // Legacy: varsayılan olarak 👍 ile toggleReaction çağır
        toggleReaction(postId, "👍")
    }

    /**
     * Emoji reaksiyon ekle/değiştir/kaldır.
     * Basit tıkla 👍, uzun basınca emoji picker ile başka emoji seçilebilir.
     */
    fun toggleReaction(postId: String, emoji: String) {
        viewModelScope.launch {
            repository.toggleReaction(postId, emoji).onSuccess { (isNowReacting, currentEmoji) ->
                // Update all post lists with optimistic reaction state
                val updatePost: (CommunityPost) -> CommunityPost = { post ->
                    if (post.id != postId) {
                        post
                    } else {
                        computeUpdatedPost(post, isNowReacting, currentEmoji)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    feedPosts = _uiState.value.feedPosts.map(updatePost),
                    followingPosts = _uiState.value.followingPosts.map(updatePost),
                    profilePosts = _uiState.value.profilePosts.map(updatePost)
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = mapGeneralError(e))
            }
        }
    }

    /**
     * Reaksiyon sonrası post güncelleme hesaplama yardımcısı.
     */
    private fun computeUpdatedPost(
        post: CommunityPost,
        isNowReacting: Boolean,
        currentEmoji: String?
    ): CommunityPost {
        val newReactionCounts = if (isNowReacting && currentEmoji != null) {
            // Reaksiyon eklendi veya değiştirildi
            val oldEmoji = post.myReaction
            val counts = post.reactionCounts.toMutableMap()
            if (oldEmoji != null) {
                val oldCount = counts[oldEmoji] ?: 0
                if (oldCount <= 1) counts.remove(oldEmoji) else counts[oldEmoji] = oldCount - 1
            }
            counts[currentEmoji] = (counts[currentEmoji] ?: 0) + 1
            counts.toMap()
        } else {
            // Reaksiyon kaldırıldı
            val oldEmoji = post.myReaction
            if (oldEmoji != null) {
                val counts = post.reactionCounts.toMutableMap()
                val oldCount = counts[oldEmoji] ?: 0
                if (oldCount <= 1) counts.remove(oldEmoji) else counts[oldEmoji] = oldCount - 1
                counts.toMap()
            } else {
                post.reactionCounts
            }
        }

        return post.copy(
            myReaction = currentEmoji,
            isLikedByMe = isNowReacting,
            likeCount = newReactionCounts.values.sum(),
            reactionCounts = newReactionCounts
        )
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId).onSuccess {
                _uiState.value = _uiState.value.copy(
                    feedPosts = _uiState.value.feedPosts.filter { it.id != postId },
                    followingPosts = _uiState.value.followingPosts.filter { it.id != postId },
                    profilePosts = _uiState.value.profilePosts.filter { it.id != postId }
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = mapGeneralError(e))
            }
        }
    }

    /**
     * Sends a community post report to Supabase [feedback_messages] (same table as Help & Support).
     */
    fun reportCommunityPost(post: CommunityPost, reasonLabel: String, detail: String) {
        viewModelScope.launch {
            val user = _uiState.value.currentUser
            if (user == null) {
                _uiState.update { it.copy(error = getString(com.taytek.basehw.R.string.login_required_feed)) }
                return@launch
            }
            val username = user.username?.trim()?.takeIf { it.isNotBlank() }
                ?: user.email.trim().takeIf { it.isNotBlank() }
                ?: getString(com.taytek.basehw.R.string.collector_name)
            val subject = getString(com.taytek.basehw.R.string.report_post_feedback_subject)
            val message = buildString {
                appendLine("type=community_post_report")
                appendLine("post_id=${post.id}")
                appendLine("author_uid=${post.authorUid}")
                appendLine("author_username=${post.authorUsername}")
                appendLine("reason=$reasonLabel")
                if (detail.isNotBlank()) {
                    appendLine("reporter_notes=$detail")
                }
                appendLine("car_brand=${post.carBrand}")
                appendLine("car_model=${post.carModelName}")
                if (!post.carSeries.isNullOrBlank()) appendLine("car_series=${post.carSeries}")
                post.carYear?.let { appendLine("car_year=$it") }
                if (post.caption.isNotBlank()) {
                    appendLine("caption_excerpt=${post.caption.trim().take(800)}")
                }
            }
            authRepository.sendFeedback(
                Feedback(username = username, subject = subject, message = message)
            ).onSuccess {
                _uiState.update {
                    it.copy(reportPostSuccessNonce = it.reportPostSuccessNonce + 1)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = mapGeneralError(e)) }
            }
        }
    }

    fun createFeedPost(caption: String, selectedCar: UserCar?) {
        val sanitizedCaption = caption.trim()
        if (sanitizedCaption.isBlank() && selectedCar == null) {
            _uiState.value = _uiState.value.copy(createPostError = getString(com.taytek.basehw.R.string.vm_community_post_empty))
            return
        }

        val hasAcceptedRules = _uiState.value.currentUser?.rulesAccepted == true || appSettingsManager.hasAcceptedCommunityRules()
        if (!hasAcceptedRules) {
            _uiState.value = _uiState.value.copy(
                showRulesDialog = true,
                pendingCommentText = null,
                pendingPostCaption = sanitizedCaption,
                pendingPostCar = selectedCar
            )
            return
        }

        val currentUid = authRepository.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            val recentPosts = appSettingsManager
                .getCommunityPostTimestamps(currentUid)
                .filter { now - it < POST_LIMIT_WINDOW_MILLIS }

            if (recentPosts.size >= POST_LIMIT_COUNT) {
                val remainingMillis = (recentPosts.minOrNull() ?: now) + POST_LIMIT_WINDOW_MILLIS - now
                _uiState.value = _uiState.value.copy(
                    createPostError = getString(com.taytek.basehw.R.string.vm_community_post_cooldown).format(formatCooldown(remainingMillis))
                )
                return
            }
        }

        _uiState.value = _uiState.value.copy(isCreatingPost = true, createPostError = null)

        val carModelName = selectedCar?.masterData?.modelName
            ?: selectedCar?.manualModelName
            ?: ""
        val carBrand = selectedCar?.masterData?.brand?.displayName
            ?: selectedCar?.manualBrand?.displayName
            ?: "BaseHW"
        val carYear = selectedCar?.masterData?.year ?: selectedCar?.manualYear
        val carSeries = selectedCar?.masterData?.series ?: selectedCar?.manualSeries
        // Only user-owned photos — never catalog/stock master images (those stay for search/catalog UI only).
        val carImageUrl = listOf(
            selectedCar?.backupPhotoUrl,
            selectedCar?.userPhotoUrl
        ).firstOrNull { value ->
            !value.isNullOrBlank() && (value.startsWith("http://") || value.startsWith("https://"))
        } ?: ""
        val carFeature = selectedCar?.masterData?.feature ?: if (selectedCar?.isWishlist == true) "Wanted" else null
        
        // Multi-image collection
        val allImageUrls = mutableListOf<String>()
        if (carImageUrl.isNotBlank()) {
            allImageUrls.add(carImageUrl)
        }
        selectedCar?.additionalPhotos?.forEach { photo ->
            if (photo.isNotBlank() && photo != carImageUrl) {
                allImageUrls.add(photo)
            }
        }

        // Get current user's avatar info directly from repository (more reliable)
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid
            val userProfile = if (uid != null) {
                repository.getUserProfile(uid).getOrNull()
            } else null
            val avatarId = userProfile?.selectedAvatarId ?: 1
            val avatarUrl = userProfile?.customAvatarUrl

            repository.createPost(
                carModelName = carModelName,
                carBrand = carBrand,
                carYear = carYear,
                carSeries = carSeries,
                carImageUrl = carImageUrl,
                caption = sanitizedCaption,
                carFeature = carFeature,
                authorSelectedAvatarId = avatarId,
                authorCustomAvatarUrl = avatarUrl,
                carImageUrls = allImageUrls
            ).onSuccess {
                val currentUid2 = authRepository.currentUser?.uid
                if (currentUid2 != null && selectedCar != null) {
                    val listingId = selectedCar.firestoreId.takeIf { it.isNotBlank() } ?: "local_${selectedCar.id}"
                    val listingTitle = listOfNotNull(
                        selectedCar.masterData?.brand?.displayName ?: selectedCar.manualBrand?.displayName,
                        selectedCar.masterData?.modelName ?: selectedCar.manualModelName
                    ).joinToString(" ").ifBlank { carModelName }
                    supabaseSyncRepository.publishListing(
                        firebaseUid = currentUid2,
                        listingId = listingId,
                        title = listingTitle,
                        imageUrl = carImageUrl.ifBlank { null }
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isCreatingPost = false,
                    createPostError = null,
                    postCreateSuccessNonce = _uiState.value.postCreateSuccessNonce + 1
                )

                if (!uid.isNullOrBlank()) {
                    val now = System.currentTimeMillis()
                    val updated = appSettingsManager
                        .getCommunityPostTimestamps(uid)
                        .filter { now - it < POST_LIMIT_WINDOW_MILLIS } + now
                    appSettingsManager.setCommunityPostTimestamps(uid, updated)
                }

                loadFeed()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isCreatingPost = false,
                    createPostError = mapCreatePostError(e)
                )
            }
        }
    }

    private fun formatCooldown(remainingMillis: Long): String {
        val safe = remainingMillis.coerceAtLeast(0L)
        val totalSeconds = ((safe + 999L) / 1000L).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return getString(com.taytek.basehw.R.string.cooldown_format).format(minutes, seconds)
    }

    private fun mapCreatePostError(error: Throwable): String {
        val raw = (error.message ?: "").trim()
        if (raw.contains("PGRST205", ignoreCase = true)) {
            return getString(com.taytek.basehw.R.string.vm_community_service_unavailable)
        }
        if (raw.contains("example.supabase.co", ignoreCase = true) || raw.contains("Unable to resolve host", ignoreCase = true)) {
            return getString(com.taytek.basehw.R.string.vm_community_supabase_error)
        }
        
        return when(raw) {
            "ERR_FORUM_BANNED" -> getString(com.taytek.basehw.R.string.error_forum_banned)
            "ERR_MODERATION_UNAVAILABLE" -> getString(com.taytek.basehw.R.string.error_moderation_unavailable)
            "ERR_ADMIN_MOD_REQUIRED" -> getString(com.taytek.basehw.R.string.error_admin_mod_required)
            "ERR_CONTENT_RULES_VIOLATION" -> getString(com.taytek.basehw.R.string.error_content_rules_violation)
            else -> raw.ifBlank { getString(com.taytek.basehw.R.string.vm_community_post_failed) }
        }
    }
    
    private fun mapGeneralError(error: Throwable): String {
        val raw = (error.message ?: "").trim()
        return when(raw) {
            "ERR_FORUM_BANNED" -> getString(com.taytek.basehw.R.string.error_forum_banned)
            "ERR_MODERATION_UNAVAILABLE" -> getString(com.taytek.basehw.R.string.error_moderation_unavailable)
            "ERR_ADMIN_MOD_REQUIRED" -> getString(com.taytek.basehw.R.string.error_admin_mod_required)
            "ERR_CONTENT_RULES_VIOLATION" -> getString(com.taytek.basehw.R.string.error_content_rules_violation)
            else -> {
                if (raw.contains("http", ignoreCase = true) || raw.contains("supabase", ignoreCase = true) || raw.contains("timeout", ignoreCase = true) || raw.contains("unresolved", ignoreCase = true) || raw.contains("failed to connect", ignoreCase = true) || raw.contains("socket", ignoreCase = true)) {
                    "Sunucu ile bağlantı kurulamadı. Lütfen internet bağlantınızı kontrol edin."
                } else {
                    raw
                }
            }
        }
    }

    // ── Comments ───────────────────────────────────────────

    fun openComments(postId: String) {
        _uiState.value = _uiState.value.copy(
            activeCommentPostId = postId,
            isLoadingComments = true,
            activePostComments = emptyList()
        )
        viewModelScope.launch {
            repository.getComments(postId).onSuccess { comments ->
                _uiState.value = _uiState.value.copy(
                    activePostComments = comments,
                    isLoadingComments = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingComments = false)
            }
        }
    }

    fun closeComments() {
        _uiState.value = _uiState.value.copy(activeCommentPostId = null, activePostComments = emptyList())
    }

    fun addComment(text: String) {
        val hasAcceptedRules = _uiState.value.currentUser?.rulesAccepted == true || appSettingsManager.hasAcceptedCommunityRules()
        if (!hasAcceptedRules) {
            _uiState.value = _uiState.value.copy(
                showRulesDialog = true,
                pendingCommentText = text,
                pendingPostCaption = null,
                pendingPostCar = null
            )
            return
        }
        val postId = _uiState.value.activeCommentPostId ?: return

        // Comment rate limiting check
        val currentUid = authRepository.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            val recentComments = appSettingsManager
                .getCommunityCommentTimestamps(currentUid)
                .filter { now - it < COMMENT_LIMIT_WINDOW_MILLIS }

            if (recentComments.size >= COMMENT_LIMIT_COUNT) {
                val remainingMillis = (recentComments.minOrNull() ?: now) + COMMENT_LIMIT_WINDOW_MILLIS - now
                _uiState.value = _uiState.value.copy(
                    error = getString(com.taytek.basehw.R.string.vm_community_comment_cooldown).format(formatCooldown(remainingMillis))
                )
                return
            }
        }

        viewModelScope.launch {
            repository.addComment(postId, text).onSuccess { comment ->
                _uiState.value = _uiState.value.copy(
                    activePostComments = _uiState.value.activePostComments + comment,
                    error = null,
                    // Update comment count in all lists
                    feedPosts = _uiState.value.feedPosts.map { p ->
                        if (p.id == postId) p.copy(commentCount = p.commentCount + 1) else p
                    },
                    followingPosts = _uiState.value.followingPosts.map { p ->
                        if (p.id == postId) p.copy(commentCount = p.commentCount + 1) else p
                    },
                    profilePosts = _uiState.value.profilePosts.map { p ->
                        if (p.id == postId) p.copy(commentCount = p.commentCount + 1) else p
                    }
                )

                // Save comment timestamp for rate limiting
                if (!currentUid.isNullOrBlank()) {
                    val now2 = System.currentTimeMillis()
                    val updated = appSettingsManager
                        .getCommunityCommentTimestamps(currentUid)
                        .filter { now2 - it < COMMENT_LIMIT_WINDOW_MILLIS } + now2
                    appSettingsManager.setCommunityCommentTimestamps(currentUid, updated)
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = mapGeneralError(e))
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        val previousState = _uiState.value
        
        // Optimistic update
        _uiState.value = _uiState.value.copy(
            activePostComments = _uiState.value.activePostComments.filter { it.id != commentId },
            feedPosts = _uiState.value.feedPosts.map { p ->
                if (p.id == postId) p.copy(commentCount = (p.commentCount - 1).coerceAtLeast(0)) else p
            },
            followingPosts = _uiState.value.followingPosts.map { p ->
                if (p.id == postId) p.copy(commentCount = (p.commentCount - 1).coerceAtLeast(0)) else p
            },
            profilePosts = _uiState.value.profilePosts.map { p ->
                if (p.id == postId) p.copy(commentCount = (p.commentCount - 1).coerceAtLeast(0)) else p
            }
        )

        viewModelScope.launch {
            repository.deleteComment(postId, commentId).onFailure { e ->
                // Rollback on failure
                _uiState.value = previousState.copy(error = mapGeneralError(e))
            }
        }
    }

    fun loadLeaderboard() {
        _uiState.value = _uiState.value.copy(isLoadingLeaderboard = true)
        viewModelScope.launch {
            repository.getTopUsers(20).onSuccess { users ->
                _uiState.value = _uiState.value.copy(topUsers = users, isLoadingLeaderboard = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingLeaderboard = false)
            }
        }
    }

    // ── User Profile ──────────────────────────────────────

    fun loadProfileFollowers(uid: String) {
        _uiState.value = _uiState.value.copy(isLoadingFollowUsers = true)
        viewModelScope.launch {
            repository.getFollowersUsers(uid).onSuccess { users ->
                _uiState.value = _uiState.value.copy(
                    profileFollowers = users,
                    isLoadingFollowUsers = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingFollowUsers = false)
            }
        }
    }

    fun loadProfileFollowing(uid: String) {
        _uiState.value = _uiState.value.copy(isLoadingFollowUsers = true)
        viewModelScope.launch {
            repository.getFollowingUsers(uid).onSuccess { users ->
                _uiState.value = _uiState.value.copy(
                    profileFollowing = users,
                    isLoadingFollowUsers = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingFollowUsers = false)
            }
        }
    }

    fun loadUserProfile(uid: String) {
        observeFollowStatus(uid)

        val isRefresh = _uiState.value.profileUser?.uid == uid
        val cachedItems   = collectionCache[uid]      ?: emptyList()
        val cachedBadge   = userBadgeCache[uid]

        val initialState = if (isRefresh) {
            _uiState.value.copy(isFollowActionLoading = true)
        } else {
            _uiState.value.copy(
                isLoadingProfile      = true,
                isFollowActionLoading = true,
                profileUser           = null,
                profilePosts          = emptyList(),
                profileFollowers      = emptyList(),
                profileFollowing      = emptyList(),
                profileAtakasCount         = 0,
                profileAtakasPreview       = emptyList(),
                profileReverseAtakasCount  = 0,
                profileReverseAtakasPreview = emptyList(),
                // Show cached data instantly while fresh data loads
                profileCollectionTitles = cachedItems.map { it.title },
                profileCollectionItems  = cachedItems,
                profileWishlistTitles   = emptyList()
            )
        }
        _uiState.value = initialState

        // ── Launch all fetches in parallel as separate coroutines ───────
        // Profile
        viewModelScope.launch {
            repository.getUserProfile(uid).onSuccess { user ->
                val displayBadge = cachedBadge ?: user.activeBadge
                _uiState.value = _uiState.value.copy(
                    profileUser      = user.copy(activeBadge = displayBadge),
                    isLoadingProfile = false
                )
                appSettingsManager.setAcceptedCommunityRules(user.rulesAccepted)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingProfile = false)
            }
        }

        // Posts
        viewModelScope.launch {
            repository.getUserPosts(uid, limit = 50).onSuccess { posts ->
                _uiState.value = _uiState.value.copy(profilePosts = hydratePostBadges(posts))
            }
        }

        // Snapshot → collection + badge (single network call for both)
        viewModelScope.launch {
            val snapshotText = supabaseSyncRepository.fetchCollectionSnapshot(uid).getOrNull()
            if (snapshotText != null) {
                val parsed = parseSnapshotTitles(snapshotText)
                if (parsed.collectionItems.isNotEmpty() || parsed.wishlistItems.isNotEmpty()) {
                    collectionCache[uid] = parsed.collectionItems
                    _uiState.value = _uiState.value.copy(
                        profileCollectionTitles = parsed.collectionTitles,
                        profileWishlistTitles   = parsed.wishlistTitles,
                        profileCollectionItems  = parsed.collectionItems
                    )
                }
                val rankCars = parseSnapshotRankCars(snapshotText)
                val badge    = BadgeType.fromInputs(CollectionRankCalculator.calculate(rankCars))
                userBadgeCache[uid] = badge
                val current = _uiState.value.profileUser
                if (current != null && current.uid == uid) {
                    _uiState.value = _uiState.value.copy(
                        profileUser = current.copy(activeBadge = badge)
                    )
                }
            } else {
                val publicTitles = supabaseSyncRepository.getPublicListingTitles(uid).getOrDefault(emptyList())
                val publicItems  = publicTitles.distinct().map { CollectionItem(title = it) }
                if (publicItems.isNotEmpty()) collectionCache[uid] = publicItems
                _uiState.value = _uiState.value.copy(
                    profileCollectionTitles = publicTitles.distinct(),
                    profileWishlistTitles   = emptyList(),
                    profileCollectionItems  = publicItems
                )
            }
            computeAtakasMatch(uid)
        }
    }


    private suspend fun computeAtakasMatch(targetUid: String) {
        val targetUser = _uiState.value.profileUser
        val myCars = userCarRepository.getAllCarsWithMasterList()

        val myWantedNormalized = myCars
            .filter { it.isWishlist }
            .mapNotNull { car ->
                normalizeForAtakas(
                    buildAtakasTitle(
                        masterBrand = car.masterData?.brand?.displayName,
                        masterModelName = car.masterData?.modelName,
                        manualBrand = car.manualBrand?.displayName,
                        manualModelName = car.manualModelName
                    )
                )
            }
            .toSet()

        val myCollectionNormalized = myCars
            .filter { !it.isWishlist }
            .mapNotNull { car ->
                normalizeForAtakas(
                    buildAtakasTitle(
                        masterBrand = car.masterData?.brand?.displayName,
                        masterModelName = car.masterData?.modelName,
                        manualBrand = car.manualBrand?.displayName,
                        manualModelName = car.manualModelName
                    )
                )
            }
            .toSet()

        val collectionNormalizedToDisplay = linkedMapOf<String, String>()
        _uiState.value.profileCollectionTitles.forEach { title ->
            val normalized = normalizeForAtakas(title)
            if (normalized != null && !collectionNormalizedToDisplay.containsKey(normalized)) {
                collectionNormalizedToDisplay[normalized] = title
            }
        }

        val wishlistNormalizedToDisplay = linkedMapOf<String, String>()
        _uiState.value.profileWishlistTitles.forEach { title ->
            val normalized = normalizeForAtakas(title)
            if (normalized != null && !wishlistNormalizedToDisplay.containsKey(normalized)) {
                wishlistNormalizedToDisplay[normalized] = title
            }
        }

        val forwardMatches = if (targetUser?.isCollectionPublic == true) {
            myWantedNormalized.intersect(collectionNormalizedToDisplay.keys)
        } else {
            emptySet()
        }
        val reverseMatches = if (targetUser?.isWishlistPublic == true) {
            myCollectionNormalized.intersect(wishlistNormalizedToDisplay.keys)
        } else {
            emptySet()
        }

        val forwardPreview = forwardMatches.mapNotNull { collectionNormalizedToDisplay[it] }.take(3)
        val reversePreview = reverseMatches.mapNotNull { wishlistNormalizedToDisplay[it] }.take(3)

        _uiState.value = _uiState.value.copy(
            profileAtakasCount = forwardMatches.size,
            profileAtakasPreview = forwardPreview,
            profileReverseAtakasCount = reverseMatches.size,
            profileReverseAtakasPreview = reversePreview
        )
    }

    private suspend fun loadSharedListings(uid: String) {
        val snapshotTitles = supabaseSyncRepository.fetchCollectionSnapshot(uid)
            .getOrNull()
            ?.let { parseSnapshotTitles(it) }
            ?: SharedListingTitles()

        if (snapshotTitles.collectionItems.isNotEmpty() || snapshotTitles.wishlistItems.isNotEmpty()) {
            collectionCache[uid] = snapshotTitles.collectionItems
            _uiState.value = _uiState.value.copy(
                profileCollectionTitles = snapshotTitles.collectionTitles,
                profileWishlistTitles = snapshotTitles.wishlistTitles,
                profileCollectionItems = snapshotTitles.collectionItems
            )
            return
        }

        val publicTitles = supabaseSyncRepository.getPublicListingTitles(uid).getOrDefault(emptyList())
        val publicItems = publicTitles.distinct().map { CollectionItem(title = it) }
        if (publicItems.isNotEmpty()) collectionCache[uid] = publicItems
        _uiState.value = _uiState.value.copy(
            profileCollectionTitles = publicTitles.distinct(),
            profileWishlistTitles = emptyList(),
            profileCollectionItems = publicItems
        )
    }

    private fun parseSnapshotTitles(payloadText: String): SharedListingTitles {
        return runCatching {
            val root = JSONObject(payloadText)
            val cars = root.optJSONArray("cars") ?: return SharedListingTitles()

            val collectionItems = mutableListOf<com.taytek.basehw.ui.screens.community.CollectionItem>()
            val wishlistItems = mutableListOf<com.taytek.basehw.ui.screens.community.CollectionItem>()

            for (index in 0 until cars.length()) {
                val car = cars.optJSONObject(index) ?: continue
                val isWishlist = car.optBoolean("isWishlist", false)
                val titleParts = listOfNotNull(
                    car.optString("masterBrand").takeIf { it.isNotBlank() },
                    car.optString("masterModelName").takeIf { it.isNotBlank() },
                    car.optString("manualBrand").takeIf { it.isNotBlank() },
                    car.optString("manualModelName").takeIf { it.isNotBlank() }
                )
                val title = titleParts.take(2).joinToString(" ").trim()
                if (title.isBlank()) continue
                
                // User photos only — no catalog/stock master URLs in community profile listings.
                val imageUrl = listOf(
                    "backupPhotoUrl",
                    "userPhotoUrl",
                    "carImageUrl"
                ).mapNotNull { key ->
                    car.optString(key).takeIf { car.has(key) }
                }.firstOrNull { candidate ->
                    candidate.toValidRemoteImageUrl() != null
                }?.toValidRemoteImageUrl()
                
                val collectionItem = com.taytek.basehw.ui.screens.community.CollectionItem(title = title, imageUrl = imageUrl)
                if (isWishlist) wishlistItems.add(collectionItem) else collectionItems.add(collectionItem)
            }

            SharedListingTitles(
                collectionItems = collectionItems,
                wishlistItems = wishlistItems
            )
        }.getOrElse { SharedListingTitles() }
    }

    private suspend fun hydratePostBadges(posts: List<CommunityPost>): List<CommunityPost> {
        if (posts.isEmpty()) return posts
        val authorBadges = resolveBadges(posts.map { it.authorUid }.toSet())
        return posts.map { post ->
            post.copy(authorBadge = authorBadges[post.authorUid] ?: post.authorBadge)
        }
    }

    private suspend fun resolveBadges(uids: Set<String>): Map<String, BadgeType> {
        if (uids.isEmpty()) return emptyMap()
        val resolved = mutableMapOf<String, BadgeType>()
        val pending = uids.filterNot { userBadgeCache.containsKey(it) }
        uids.forEach { uid ->
            userBadgeCache[uid]?.let { resolved[uid] = it }
        }
        pending.forEach { uid ->
            val badge = loadBadgeFromSnapshot(uid)
            userBadgeCache[uid] = badge
            resolved[uid] = badge
        }
        return resolved
    }

    private suspend fun loadBadgeFromSnapshot(uid: String): BadgeType {
        val payload = supabaseSyncRepository.fetchCollectionSnapshot(uid).getOrNull() ?: return BadgeType.ROOKIE
        val cars = parseSnapshotRankCars(payload)
        val inputs = CollectionRankCalculator.calculate(cars)
        return BadgeType.fromInputs(inputs)
    }

    private fun parseSnapshotRankCars(payloadText: String): List<RankCarInput> {
        return runCatching {
            val root = JSONObject(payloadText)
            val cars = root.optJSONArray("cars") ?: return emptyList()
            buildList {
                for (index in 0 until cars.length()) {
                    val car = cars.optJSONObject(index) ?: continue
                    if (car.optBoolean("isWishlist", false)) continue
                    val masterBrand = car.optString("masterBrand").takeIf { it.isNotBlank() }
                    val manualBrand = car.optString("manualBrand").takeIf { it.isNotBlank() }
                    val brand = (masterBrand ?: manualBrand)?.let { raw ->
                        try {
                            Brand.valueOf(raw)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    val feature = car.optString("feature").takeIf { it.isNotBlank() }
                        ?: car.optString("masterFeature").takeIf { it.isNotBlank() }
                    val condition = VehicleCondition.fromString(
                        car.optString("condition").takeIf { it.isNotBlank() }
                    )
                    val isPremium = car.optBoolean("manualIsPremium", false)
                    val isCustom = car.optBoolean("isCustom", false)
                    val quantity = car.optInt("quantity", 1).coerceAtLeast(1)
                    add(
                        RankCarInput(
                            brand = brand,
                            feature = feature,
                            condition = condition,
                            isPremium = isPremium,
                            isCustom = isCustom,
                            quantity = quantity
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private suspend fun enrichProfileBadge(uid: String) {
        val badge = userBadgeCache[uid] ?: loadBadgeFromSnapshot(uid).also { userBadgeCache[uid] = it }
        val current = _uiState.value.profileUser ?: return
        if (current.uid != uid) return
        _uiState.value = _uiState.value.copy(profileUser = current.copy(activeBadge = badge))
    }

    private data class SharedListingTitles(
        val collectionItems: List<com.taytek.basehw.ui.screens.community.CollectionItem> = emptyList(),
        val wishlistItems: List<com.taytek.basehw.ui.screens.community.CollectionItem> = emptyList()
    ) {
        val collectionTitles: List<String> get() = collectionItems.map { it.title }
        val wishlistTitles: List<String> get() = wishlistItems.map { it.title }
    }

    private fun String?.toValidRemoteImageUrl(): String? {
        val normalized = this?.trim()?.takeUnless { raw ->
            raw.isBlank() || raw.equals("null", ignoreCase = true)
        } ?: return null
        return normalized.takeIf {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
        }
    }

    private fun normalizeForAtakas(raw: String?): String? {
        val cleaned = raw
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9]+"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
        return cleaned?.takeIf { it.isNotBlank() }
    }

    private fun buildAtakasTitle(
        masterBrand: String?,
        masterModelName: String?,
        manualBrand: String?,
        manualModelName: String?
    ): String? {
        val titleParts = listOfNotNull(
            masterBrand?.takeIf { it.isNotBlank() }
                ?: manualBrand?.takeIf { it.isNotBlank() },
            masterModelName?.takeIf { it.isNotBlank() }
                ?: manualModelName?.takeIf { it.isNotBlank() }
        )
        return titleParts.joinToString(" ").takeIf { it.isNotBlank() }
    }

    fun toggleFollow(targetUid: String) {
        if (_uiState.value.isFollowActionLoading) return

        val wasFollowing = _uiState.value.isFollowingProfile
        _uiState.value = _uiState.value.copy(isFollowActionLoading = true, error = null)

        viewModelScope.launch {
            val actionResult = if (wasFollowing) {
                repository.unfollowUser(targetUid)
            } else {
                repository.followUser(targetUid)
            }

            actionResult.onSuccess {
                val refreshedProfile = repository.getUserProfile(targetUid).getOrNull()
                _uiState.value = _uiState.value.copy(
                    isFollowingProfile = !wasFollowing,
                    profileUser = refreshedProfile ?: _uiState.value.profileUser,
                    isFollowActionLoading = false
                )
                loadFollowing()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isFollowActionLoading = false,
                    error = mapGeneralError(e)
                )
            }
        }
    }

    fun clearProfile() {
        clearFollowStatusObserver()
        _uiState.value = _uiState.value.copy(
            profileUser = null,
            profilePosts = emptyList(),
            isFollowingProfile = false,
            profileAtakasCount = 0,
            profileAtakasPreview = emptyList(),
            profileReverseAtakasCount = 0,
            profileReverseAtakasPreview = emptyList(),
            profileCollectionTitles = emptyList(),
            profileWishlistTitles = emptyList()
        )
    }

    fun toggleFollowInList(targetUid: String, isCurrentlyFollowing: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyFollowing) {
                repository.unfollowUser(targetUid).onSuccess {
                    // Refresh profile to update following count
                    val myUid = authRepository.currentUser?.uid ?: return@onSuccess
                    repository.getUserProfile(myUid).onSuccess { user ->
                        _uiState.value = _uiState.value.copy(profileUser = user)
                    }
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = mapGeneralError(e)
                    )
                }
            } else {
                repository.followUser(targetUid).onSuccess {
                    val myUid = authRepository.currentUser?.uid ?: return@onSuccess
                    repository.getUserProfile(myUid).onSuccess { user ->
                        _uiState.value = _uiState.value.copy(profileUser = user)
                    }
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = mapGeneralError(e)
                    )
                }
            }
        }
    }

    fun blockFollower(targetUid: String) {
        viewModelScope.launch {
            // Optimistic update: remove from followers list immediately
            _uiState.value = _uiState.value.copy(
                profileFollowers = _uiState.value.profileFollowers.filter { it.uid != targetUid }
            )

            repository.blockFollower(targetUid).onSuccess {
                // Refresh current user's profile to update follower count
                val myUid = authRepository.currentUser?.uid ?: return@onSuccess
                repository.getUserProfile(myUid).onSuccess { user ->
                    _uiState.value = _uiState.value.copy(profileUser = user)
                }
                // Refresh blocked users list
                loadBlockedUsers()
            }.onFailure { e ->
                // Rollback: re-load followers
                val myUid = authRepository.currentUser?.uid
                if (myUid != null) {
                    loadProfileFollowers(myUid)
                }
                _uiState.value = _uiState.value.copy(
                    error = mapGeneralError(e)
                )
            }
        }
    }

    fun loadBlockedUsers() {
        _uiState.value = _uiState.value.copy(isLoadingBlockedUsers = true)
        viewModelScope.launch {
            repository.getBlockedUsers().onSuccess { users ->
                _uiState.value = _uiState.value.copy(
                    blockedUsers = users,
                    isLoadingBlockedUsers = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingBlockedUsers = false)
            }
        }
    }

    // ── Admin Panel Functions ──────────────────────────────

    fun loadAllUsers() {
        _uiState.value = _uiState.value.copy(isLoadingAllUsers = true, adminPanelError = null)
        viewModelScope.launch {
            repository.getAllUsers().onSuccess { users ->
                _uiState.value = _uiState.value.copy(
                    allUsers = users,
                    isLoadingAllUsers = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoadingAllUsers = false,
                    adminPanelError = mapGeneralError(e)
                )
            }
        }
    }

    fun loadBannedUsers() {
        _uiState.value = _uiState.value.copy(isLoadingBannedUsers = true, adminPanelError = null)
        viewModelScope.launch {
            repository.getBannedUsers().onSuccess { users ->
                _uiState.value = _uiState.value.copy(
                    bannedUsers = users,
                    isLoadingBannedUsers = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoadingBannedUsers = false,
                    adminPanelError = mapGeneralError(e)
                )
            }
        }
    }

    fun checkIsAdmin(uid: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.checkIsAdmin(uid).onSuccess { isAdmin ->
                callback(isAdmin)
            }.onFailure {
                callback(false)
            }
        }
    }

    fun setModerator(targetUid: String, isMod: Boolean) {
        viewModelScope.launch {
            repository.setModerator(targetUid, isMod).onSuccess {
                // Reload users to reflect changes
                loadAllUsers()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(adminPanelError = mapGeneralError(e))
            }
        }
    }

    fun banUserFromForum(targetUid: String, reason: String? = null) {
        viewModelScope.launch {
            repository.banUserFromForum(targetUid, reason).onSuccess {
                // Reload users and banned users
                loadAllUsers()
                loadBannedUsers()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(adminPanelError = mapGeneralError(e))
            }
        }
    }

    fun unbanUserFromForum(targetUid: String) {
        viewModelScope.launch {
            repository.unbanUserFromForum(targetUid).onSuccess {
                // Reload users and banned users
                loadAllUsers()
                loadBannedUsers()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(adminPanelError = mapGeneralError(e))
            }
        }
    }

    fun clearAdminPanelError() {
        _uiState.value = _uiState.value.copy(adminPanelError = null)
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            // Optimistic update
            _uiState.value = _uiState.value.copy(
                blockedUsers = _uiState.value.blockedUsers.filter { it.uid != targetUid }
            )

            repository.unblockUser(targetUid).onFailure { e ->
                // Rollback
                loadBlockedUsers()
                _uiState.value = _uiState.value.copy(
                    error = mapGeneralError(e)
                )
            }
        }
    }

    fun dismissRulesDialog() {
        _uiState.value = _uiState.value.copy(showRulesDialog = false)
    }

    fun acceptRules() {
        viewModelScope.launch {
            repository.acceptRules().onSuccess {
                appSettingsManager.setAcceptedCommunityRules(true)
                val pendingText = _uiState.value.pendingCommentText
                val pendingPostCaption = _uiState.value.pendingPostCaption
                val pendingPostCar = _uiState.value.pendingPostCar
                _uiState.value = _uiState.value.copy(
                    currentUser = _uiState.value.currentUser?.copy(rulesAccepted = true),
                    showRulesDialog = false,
                    pendingCommentText = null,
                    pendingPostCaption = null,
                    pendingPostCar = null
                )

                // Give the UI state a tiny moment to settle or use a local variable to trigger
                // to avoid race conditions where the next call might still see the old state.
                if (!pendingText.isNullOrBlank()) {
                    addComment(pendingText)
                } else if (!pendingPostCaption.isNullOrBlank() || pendingPostCar != null) {
                    createFeedPost(pendingPostCaption.orEmpty(), pendingPostCar)
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = mapGeneralError(e))
            }
        }
    }

    private fun loadCurrentUser(uid: String) {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            repository.getUserProfile(uid).onSuccess { user ->
                appSettingsManager.setAcceptedCommunityRules(user.rulesAccepted)
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
        }
    }
}
