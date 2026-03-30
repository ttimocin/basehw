package com.taytek.basehw.ui.screens.community

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.taytek.basehw.R
import com.taytek.basehw.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.profileUser?.username ?: "",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoadingProfile) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ── Profile Header ────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(AppPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (uiState.profileUser?.username?.take(1) ?: "?").uppercase(),
                                color = AppPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.profileUser?.username ?: "User",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.profileUser?.isAdmin == true) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🛡️ ADMIN",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatColumn(count = uiState.profileUser?.postCount ?: 0, label = stringResource(R.string.posts_label))
                            StatColumn(count = uiState.profileUser?.followerCount ?: 0, label = stringResource(R.string.followers))
                            StatColumn(count = uiState.profileUser?.followingCount ?: 0, label = stringResource(R.string.following_label))
                        }

                        Spacer(Modifier.height(16.dp))

                        // Follow button
                        val isMe = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == userId
                        if (!isMe) {
                            Button(
                                onClick = { viewModel.toggleFollow(userId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = if (uiState.isFollowingProfile) {
                                    ButtonDefaults.outlinedButtonColors(contentColor = AppPrimary)
                                } else {
                                    ButtonDefaults.buttonColors(containerColor = AppPrimary)
                                },
                                border = if (uiState.isFollowingProfile) {
                                    BorderStroke(1.5.dp, AppPrimary)
                                } else null
                            ) {
                                Text(
                                    text = if (uiState.isFollowingProfile) stringResource(R.string.unfollow) else stringResource(R.string.follow),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // Comments sheet
            if (uiState.activeCommentPostId != null) {
                CommentsBottomSheet(
                    comments = uiState.activePostComments,
                    isLoading = uiState.isLoadingComments,
                    currentUserUid = uiState.currentUserUid,
                    onAddComment = viewModel::addComment,
                    onDeleteComment = { commentId -> 
                        uiState.activeCommentPostId?.let { postId ->
                            viewModel.deleteComment(postId, commentId)
                        }
                    },
                    onDismiss = viewModel::closeComments,
                    onUserClick = onUserClick,
                    currentUser = uiState.currentUser
                )
            }
        }
    }
}

@Composable
private fun StatColumn(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
