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
    val isLoadingLeaderboard: Boolean = false
)

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val repository: CommunityRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        val signedIn = user != null
        val verified = user?.isEmailVerified == true
        _uiState.value = _uiState.value.copy(
            isSignedIn = signedIn,
            isEmailVerified = verified
        )
        if (signedIn && verified && _uiState.value.feedPosts.isEmpty() && !_uiState.value.isLoadingFeed) {
            loadFeed()
            loadLeaderboard()
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun refreshAuth() {
        viewModelScope.launch {
            auth.currentUser?.reload()?.addOnCompleteListener {
                val user = auth.currentUser
                _uiState.value = _uiState.value.copy(
                    isSignedIn = user != null,
                    isEmailVerified = user?.isEmailVerified == true
                )
                if (user?.isEmailVerified == true) {
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
}
