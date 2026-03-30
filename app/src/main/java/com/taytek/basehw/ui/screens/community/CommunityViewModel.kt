package com.taytek.basehw.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taytek.basehw.domain.model.CommunityComment
import com.taytek.basehw.domain.model.CommunityPost
import com.taytek.basehw.domain.model.User
import com.taytek.basehw.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val isFollowingProfile: Boolean = false,
    val isLoadingProfile: Boolean = false,

    // Leaderboard
    val topUsers: List<User> = emptyList(),
    val isLoadingLeaderboard: Boolean = false,
    val showRulesDialog: Boolean = false,
    val pendingCommentText: String? = null,
    val currentUser: User? = null
)

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val repository: CommunityRepository,
    private val auth: FirebaseAuth,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
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
        if (signedIn && verified) {
            loadCurrentUser(user?.uid ?: "")
            if (_uiState.value.feedPosts.isEmpty() && !_uiState.value.isLoadingFeed) {
                loadFeed()
                loadLeaderboard()
            }
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun refreshAuth() {
        viewModelScope.launch {
            auth.currentUser?.reload()?.addOnCompleteListener {
                val user = auth.currentUser
                val isTestAccount = user?.email == "googletest@basehw.net"
                val verifiedValue = user?.isEmailVerified == true || isTestAccount
                
                _uiState.value = _uiState.value.copy(
                    isSignedIn = user != null,
                    isEmailVerified = verifiedValue
                )
                if (verifiedValue) {
                    loadFeed()
                    loadLeaderboard()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    fun loadFeed() {
        _uiState.value = _uiState.value.copy(isLoadingFeed = true, error = null)
        viewModelScope.launch {
            repository.getFeedPosts().fold(
                onSuccess = { posts ->
                    _uiState.value = _uiState.value.copy(feedPosts = posts, isLoadingFeed = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoadingFeed = false)
                }
            )
        }
    }

    fun loadFollowing() {
        _uiState.value = _uiState.value.copy(isLoadingFollowing = true)
        viewModelScope.launch {
            repository.getFollowingPosts().fold(
                onSuccess = { posts ->
                    _uiState.value = _uiState.value.copy(followingPosts = posts, isLoadingFollowing = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoadingFollowing = false)
                }
            )
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId).onSuccess { isNowLiked ->
                // Update both lists
                _uiState.value = _uiState.value.copy(
                    feedPosts = _uiState.value.feedPosts.map { post ->
                        if (post.id == postId) post.copy(
                            isLikedByMe = isNowLiked,
                            likeCount = if (isNowLiked) post.likeCount + 1 else post.likeCount - 1
                        ) else post
                    },
                    followingPosts = _uiState.value.followingPosts.map { post ->
                        if (post.id == postId) post.copy(
                            isLikedByMe = isNowLiked,
                            likeCount = if (isNowLiked) post.likeCount + 1 else post.likeCount - 1
                        ) else post
                    },
                    profilePosts = _uiState.value.profilePosts.map { post ->
                        if (post.id == postId) post.copy(
                            isLikedByMe = isNowLiked,
                            likeCount = if (isNowLiked) post.likeCount + 1 else post.likeCount - 1
                        ) else post
                    }
                )
            }
        }
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
                _uiState.value = _uiState.value.copy(error = e.message)
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
        if (!appSettingsManager.hasAcceptedCommunityRules()) {
            _uiState.value = _uiState.value.copy(showRulesDialog = true, pendingCommentText = text)
            return
        }
        val postId = _uiState.value.activeCommentPostId ?: return
        viewModelScope.launch {
            repository.addComment(postId, text).onSuccess { comment ->
                _uiState.value = _uiState.value.copy(
                    activePostComments = _uiState.value.activePostComments + comment,
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
                _uiState.value = previousState.copy(error = e.message)
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

    fun loadUserProfile(uid: String) {
        _uiState.value = _uiState.value.copy(isLoadingProfile = true, profileUser = null, profilePosts = emptyList())
        viewModelScope.launch {
            repository.getUserProfile(uid).onSuccess { user ->
                _uiState.value = _uiState.value.copy(profileUser = user, isLoadingProfile = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingProfile = false)
            }

            repository.isFollowing(uid).onSuccess { following ->
                _uiState.value = _uiState.value.copy(isFollowingProfile = following)
            }
            
            // Sync rules acceptance from profile
            repository.getUserProfile(uid).onSuccess { user ->
                if (user.rulesAccepted && !appSettingsManager.hasAcceptedCommunityRules()) {
                    appSettingsManager.setAcceptedCommunityRules(true)
                }
            }
        }
    }

    fun toggleFollow(targetUid: String) {
        viewModelScope.launch {
            if (_uiState.value.isFollowingProfile) {
                repository.unfollowUser(targetUid).onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isFollowingProfile = false,
                        profileUser = _uiState.value.profileUser?.copy(
                            followerCount = (_uiState.value.profileUser?.followerCount ?: 1) - 1
                        )
                    )
                }
            } else {
                repository.followUser(targetUid).onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isFollowingProfile = true,
                        profileUser = _uiState.value.profileUser?.copy(
                            followerCount = (_uiState.value.profileUser?.followerCount ?: 0) + 1
                        )
                    )
                }
            }
        }
    }

    fun clearProfile() {
        _uiState.value = _uiState.value.copy(profileUser = null, profilePosts = emptyList(), isFollowingProfile = false)
    }

    fun dismissRulesDialog() {
        _uiState.value = _uiState.value.copy(showRulesDialog = false)
    }

    fun acceptRules() {
        viewModelScope.launch {
            repository.acceptRules().onSuccess {
                appSettingsManager.setAcceptedCommunityRules(true)
                val pendingText = _uiState.value.pendingCommentText
                _uiState.value = _uiState.value.copy(showRulesDialog = false, pendingCommentText = null)
                if (pendingText != null) {
                    addComment(pendingText)
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun loadCurrentUser(uid: String) {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            repository.getUserProfile(uid).onSuccess { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
        }
    }
}
