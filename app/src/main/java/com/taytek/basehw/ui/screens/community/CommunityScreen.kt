package com.taytek.basehw.ui.screens.community

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.taytek.basehw.R
import com.taytek.basehw.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onUserProfileClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPageTab by remember { mutableIntStateOf(0) } // 0: Feed, 1: Following, 2: Ranking
    var postToDelete by remember { mutableStateOf<String?>(null) }

    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text(stringResource(R.string.delete_post_title)) },
            text = { Text(stringResource(R.string.delete_post_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    postToDelete?.let { viewModel.deletePost(it) }
                    postToDelete = null
                }) {
                    Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text(stringResource(R.string.delete_cancel))
                }
            }
        )
    }

    val tabTitles = listOf(
        stringResource(R.string.community_feed),
        stringResource(R.string.community_following),
        stringResource(R.string.community_ranking)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_community),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp) // Align with Home's horizontal 20dp (TopAppBar has default ~16dp)
                    )
                },
                actions = {
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier
                            .padding(end = 16.dp) // Total ~20dp with TopAppBar's default padding
                            .size(44.dp)
                            .background(
                                color = AppPrimary.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.nav_profile),
                            tint = AppPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Top Tab Row ───────────────────────────────
            TabRow(
                selectedTabIndex = selectedPageTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = AppPrimary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedPageTab == index,
                        onClick = {
                            selectedPageTab = index
                            when (index) {
                                0 -> viewModel.loadFeed()
                                1 -> viewModel.loadFollowing()
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedPageTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedPageTab == index) AppPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // ── Tab Content ───────────────────────────────
            if (uiState.isSignedIn && !uiState.isEmailVerified) {
                VerificationRequiredScreen(onRefresh = viewModel::refreshAuth)
            } else {
                when (selectedPageTab) {
                    0 -> FeedTab(
                        posts = uiState.feedPosts,
                        isLoading = uiState.isLoadingFeed,
                        isSignedIn = uiState.isSignedIn,
                        currentUser = uiState.currentUser,
                        onLikeClick = viewModel::toggleLike,
                        onCommentClick = viewModel::openComments,
                        onDeleteClick = { postToDelete = it },
                        onUserClick = onUserProfileClick
                    )
                    1 -> FollowingTab(
                        posts = uiState.followingPosts,
                        isLoading = uiState.isLoadingFollowing,
                        isSignedIn = uiState.isSignedIn,
                        currentUser = uiState.currentUser,
                        onLikeClick = viewModel::toggleLike,
                        onCommentClick = viewModel::openComments,
                        onDeleteClick = { postToDelete = it },
                        onUserClick = onUserProfileClick
                    )
                    2 -> LeaderboardTab(
                        topUsers = uiState.topUsers,
                        isLoading = uiState.isLoadingLeaderboard,
                        onUserClick = onUserProfileClick
                    )
                }
            }
        }

        // ── Comments Bottom Sheet ─────────────────────
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
                onUserClick = onUserProfileClick,
                currentUser = uiState.currentUser
            )
        }

        // ── Community Rules Dialog ────────────────────
        if (uiState.showRulesDialog) {
            CommunityRulesDialog(
                onAccept = viewModel::acceptRules,
                onDismiss = viewModel::dismissRulesDialog
            )
        }
    }
}

@Composable
fun CommunityRulesDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = AppPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.community_rules_title),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.community_rules_intro),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                val rules = listOf(
                    stringResource(R.string.rule_1),
                    stringResource(R.string.rule_2),
                    stringResource(R.string.rule_3),
                    stringResource(R.string.rule_4)
                )
                
                rules.forEach { rule ->
                    Row {
                        Text("• ", fontWeight = FontWeight.Bold, color = AppPrimary)
                        Text(rule, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.accept_rules_btn), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun FeedTab(
    posts: List<com.taytek.basehw.domain.model.CommunityPost>,
    isLoading: Boolean,
    isSignedIn: Boolean,
    currentUser: com.taytek.basehw.domain.model.User?,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    if (!isSignedIn) {
        EmptyState(stringResource(R.string.login_required_feed))
        return
    }
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppPrimary)
        }
        return
    }
    if (posts.isEmpty()) {
        EmptyState(stringResource(R.string.no_posts_yet))
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onLikeClick = { onLikeClick(post.id) },
                onCommentClick = { onCommentClick(post.id) },
                onUserClick = onUserClick,
                onDeleteClick = { onDeleteClick(post.id) },
                currentUser = currentUser
            )
        }
    }
}

// ── Following Tab ──────────────────────────────────────

@Composable
private fun FollowingTab(
    posts: List<com.taytek.basehw.domain.model.CommunityPost>,
    isLoading: Boolean,
    isSignedIn: Boolean,
    currentUser: com.taytek.basehw.domain.model.User?,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    if (!isSignedIn) {
        EmptyState(stringResource(R.string.login_required_feed))
        return
    }
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppPrimary)
        }
        return
    }
    if (posts.isEmpty()) {
        EmptyState(stringResource(R.string.no_posts_yet))
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onLikeClick = { onLikeClick(post.id) },
                onCommentClick = { onCommentClick(post.id) },
                onUserClick = onUserClick,
                onDeleteClick = { onDeleteClick(post.id) },
                currentUser = currentUser
            )
        }
    }
}

// ── Leaderboard Tab (existing logic preserved) ─────────

@Composable
private fun LeaderboardTab(
    topUsers: List<com.taytek.basehw.domain.model.User>,
    isLoading: Boolean,
    onUserClick: (String) -> Unit
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppPrimary)
        }
        return
    }

    if (topUsers.isEmpty()) {
        EmptyState(stringResource(R.string.no_posts_yet))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Podium (Top 3)
        item {
            val podiumUsers = topUsers.take(3)
            PodiumSection(podiumUsers, onUserClick)
        }

        // Rest of the list
        val remainingUsers = topUsers.drop(3)
        items(remainingUsers.size) { index ->
            val user = remainingUsers[index]
            LeaderboardItem(rank = index + 4, user = user, onUserClick = onUserClick)
        }
    }
}

// ── Shared Empty State ─────────────────────────────────

@Composable
private fun VerificationRequiredScreen(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = AppPrimary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.verification_required_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.verification_required_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
        ) {
            Text(
                text = stringResource(R.string.refresh_status),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ──────────────────────────────────────────────────────
//   LEADERBOARD COMPONENTS (preserved from original)
// ──────────────────────────────────────────────────────

@Composable
private fun LeaderboardToggle(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selectedTab == 0) AppPrimary else Color.Transparent)
                .clickable { onTabSelected(0) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "En Değerli",
                color = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selectedTab == 1) AppPrimary else Color.Transparent)
                .clickable { onTabSelected(1) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "En Çok Araba",
                color = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun PodiumSection(users: List<com.taytek.basehw.domain.model.User>, onUserClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Rank 2 (Left)
        if (users.size >= 2) {
            PodiumMember(
                user = users[1],
                rank = 2,
                borderColor = Color(0xFF94A3B8),
                imageScale = 1.0f,
                modifier = Modifier.weight(1f),
                onUserClick = onUserClick
            )
        } else if (users.size == 1) {
            Spacer(Modifier.weight(1f))
        }

        // Rank 1 (Center)
        if (users.size >= 1) {
            PodiumMember(
                user = users[0],
                rank = 1,
                borderColor = AppPrimary,
                imageScale = 1.25f,
                isGold = true,
                modifier = Modifier.weight(1.2f),
                onUserClick = onUserClick
            )
        }

        // Rank 3 (Right)
        if (users.size >= 3) {
            PodiumMember(
                user = users[2],
                rank = 3,
                borderColor = Color(0xFFB45309),
                imageScale = 1.0f,
                modifier = Modifier.weight(1f),
                onUserClick = onUserClick
            )
        } else if (users.size <= 2) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumMember(
    user: com.taytek.basehw.domain.model.User,
    rank: Int,
    borderColor: Color,
    imageScale: Float,
    modifier: Modifier = Modifier,
    isGold: Boolean = false,
    onUserClick: (String) -> Unit
) {
    Column(
        modifier = modifier.clickable { onUserClick(user.uid) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.scale(imageScale).padding(bottom = 12.dp), contentAlignment = Alignment.BottomCenter) {
            if (isGold) {
                Icon(Icons.Default.Star, contentDescription = null, tint = AppPrimary, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-24).dp).scale(1.5f))
            }
            Box(
                modifier = Modifier.size(64.dp).border(4.dp, borderColor, CircleShape).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username?.take(1)?.uppercase() ?: "?",
                    color = borderColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
            Box(modifier = Modifier.offset(y = 8.dp).background(borderColor, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(rank.toString(), color = if (isGold) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(user.username ?: "User", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (user.isAdmin) {
                Spacer(Modifier.width(2.dp))
                Text("🛡️", fontSize = 8.sp)
            }
        }
        Text("${user.postCount} ${stringResource(R.string.posts_label)}", color = AppPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun CommunityCTACard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(
                brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.surface)),
                shape = RoundedCornerShape(20.dp)
            )
            .border(width = 4.dp, brush = Brush.verticalGradient(listOf(AppPrimary, Color.Transparent)), shape = RoundedCornerShape(20.dp, 0.dp, 0.dp, 20.dp))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Topluluk", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Yeni koleksiyonerleri keşfet, nadir modelleri incele.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = AppPrimary), shape = RoundedCornerShape(12.dp)) {
                Text("Keşfet", fontWeight = FontWeight.Bold)
            }
        }
        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.align(Alignment.CenterEnd).size(120.dp).offset(x = 20.dp).graphicsLayer(alpha = 0.1f, rotationZ = 12f), tint = Color.White)
    }
}

@Composable
private fun LeaderboardItem(rank: Int, user: com.taytek.basehw.domain.model.User, onUserClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable { onUserClick(user.uid) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val rankStr = if (rank < 10) "0$rank" else rank.toString()
        Text(rankStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(AppPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.username?.take(1)?.uppercase() ?: "?",
                color = AppPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.username ?: "User", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (user.isAdmin) {
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "🛡️",
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text("${user.followerCount} ${stringResource(R.string.followers)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${user.postCount}", color = AppPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.posts_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
