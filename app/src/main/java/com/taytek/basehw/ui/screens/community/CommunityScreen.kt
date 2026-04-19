package com.taytek.basehw.ui.screens.community

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.CommunityPost
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.model.toIcon
import com.taytek.basehw.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun CommunityScreen(
    onUserProfileClick: (String) -> Unit = {},
    onInboxClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNavigateToAdminPanel: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onRanksClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val resumeCounter = remember { mutableIntStateOf(0) }

    // Track RESUME lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    resumeCounter.value++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val uiState by viewModel.uiState.collectAsState()
    val collectionCars = viewModel.collectionCarsPaged.collectAsLazyPagingItems()
    val wishlistCars = viewModel.wishlistCarsPaged.collectAsLazyPagingItems()
    var selectedPageTab by remember { mutableIntStateOf(0) }
    var postToDelete by remember { mutableStateOf<String?>(null) }
    var reportPostForDialog by remember { mutableStateOf<CommunityPost?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val appContext = LocalContext.current

    // Show errors in snackbar
    LaunchedEffect(uiState.error) {
        val err = uiState.error
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(err)
        }
    }

    LaunchedEffect(uiState.reportPostSuccessNonce) {
        if (uiState.reportPostSuccessNonce > 0) {
            snackbarHostState.showSnackbar(appContext.getString(R.string.report_post_sent))
        }
    }

    // Refresh feed avatars when screen resumes (after returning from profile)
    LaunchedEffect(resumeCounter.value) {
        if (resumeCounter.value > 0 && uiState.isSignedIn && uiState.isEmailVerified && uiState.feedPosts.isNotEmpty()) {
            // Only refresh if there are posts - reloads author avatars with fresh profile data
            viewModel.refreshAuthorAvatars()
        }
    }

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

    val postPendingReport = reportPostForDialog
    if (postPendingReport != null) {
        ReportPostDialog(
            post = postPendingReport,
            onDismiss = { reportPostForDialog = null },
            onSubmit = { reason, detail ->
                viewModel.reportCommunityPost(postPendingReport, reason, detail)
                reportPostForDialog = null
            }
        )
    }

    val tabTitles = listOf(
        stringResource(R.string.community_feed),
        stringResource(R.string.community_following),
        stringResource(R.string.community_ranking)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_community),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                },
                actions = {
                    val actionIconTint = if (isCyberTheme()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.hasUnreadNotifications) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error)
                                }
                            }
                        ) {
                            IconButton(onClick = onNotificationsClick) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = stringResource(R.string.notifications_title),
                                    tint = actionIconTint
                                )
                            }
                        }

                        BadgedBox(
                            badge = {
                                if (uiState.hasUnreadInboxMessages) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error)
                                }
                            }
                        ) {
                            IconButton(onClick = onInboxClick) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = stringResource(R.string.inbox_desc),
                                    tint = actionIconTint
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cyberRootBackgroundColor()
                )
            )
        },
        containerColor = cyberRootBackgroundColor()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedPageTab,
                containerColor = cyberRootBackgroundColor(),
                contentColor = AppTheme.tokens.primaryAccent
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
                                color = if (selectedPageTab == index) AppTheme.tokens.primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (uiState.isSignedIn && !uiState.isEmailVerified) {
                VerificationRequiredScreen(onRefresh = viewModel::refreshAuth)
            } else {
                when (selectedPageTab) {
                    0 -> FeedTab(
                        posts = uiState.feedPosts,
                        isLoading = uiState.isLoadingFeed,
                        isLoadingNextPage = uiState.isLoadingNextPage,
                        isEndReached = uiState.isFeedEndReached,
                        isCreatingPost = uiState.isCreatingPost,
                        createPostError = uiState.createPostError,
                        postCreateSuccessNonce = uiState.postCreateSuccessNonce,
                        isSignedIn = uiState.isSignedIn,
                        currentUser = uiState.currentUser,
                        collectionCars = collectionCars,
                        wishlistCars = wishlistCars,
                        onCreatePost = viewModel::createFeedPost,
                        onLikeClick = viewModel::toggleLike,
                        onReactionSelect = { postId, emoji -> viewModel.toggleReaction(postId, emoji) },
                        onCommentClick = viewModel::openComments,
                        onDeleteClick = { postToDelete = it },
                        onReportPost = { reportPostForDialog = it },
                        onUserClick = onUserProfileClick,
                        onLoadMore = viewModel::loadNextPage,
                        onRefresh = { viewModel.loadFeed() },
                        authorAvatars = uiState.authorAvatars
                    )
                    1 -> FollowingTab(
                        posts = uiState.followingPosts,
                        isLoading = uiState.isLoadingFollowing,
                        isSignedIn = uiState.isSignedIn,
                        currentUser = uiState.currentUser,
                        onLikeClick = viewModel::toggleLike,
                        onCommentClick = viewModel::openComments,
                        onDeleteClick = { postToDelete = it },
                        onReportPost = { reportPostForDialog = it },
                        onUserClick = onUserProfileClick,
                        onReactionSelect = { postId, emoji -> viewModel.toggleReaction(postId, emoji) },
                        authorAvatars = uiState.authorAvatars
                    )
                    2 -> RankingTab(
                        topUsers = uiState.topUsers,
                        isLoading = uiState.isLoadingLeaderboard,
                        onUserClick = onUserProfileClick,
                        onRanksClick = onRanksClick
                    )
                }
            }
        }

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
                Icon(Icons.Default.Gavel, contentDescription = null, tint = AppTheme.tokens.primaryAccent)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.community_rules_title), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.community_rules_intro), style = MaterialTheme.typography.bodyMedium)
                val rules = listOf(
                    stringResource(R.string.rule_1),
                    stringResource(R.string.rule_2),
                    stringResource(R.string.rule_3),
                    stringResource(R.string.rule_4)
                )
                rules.forEach { rule ->
                    Row {
                        Text("• ", fontWeight = FontWeight.Bold, color = AppTheme.tokens.primaryAccent)
                        Text(rule, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            when (LocalThemeVariant.current) {
                ThemeVariant.Cyber -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(cyberActionGradientBrush())
                            .clickable(onClick = onAccept)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.accept_rules_btn),
                            fontWeight = FontWeight.Bold,
                            color = CyberKnockoutIconTint
                        )
                    }
                }
                ThemeVariant.NeonCyan -> {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.tokens.primaryAccent,
                            contentColor = NeonCyanKnockoutIconTint
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.accept_rules_btn), fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.tokens.primaryAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.accept_rules_btn), fontWeight = FontWeight.Bold)
                    }
                }
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
private fun ReportPostDialog(
    post: CommunityPost,
    onDismiss: () -> Unit,
    onSubmit: (reasonLabel: String, extraDetail: String) -> Unit
) {
    val context = LocalContext.current
    val reasonIds = listOf(
        R.string.report_reason_profanity,
        R.string.report_reason_harmful,
        R.string.report_reason_spam,
        R.string.report_reason_harassment,
        R.string.report_reason_other
    )
    var selectedIndex by remember(post.id) { mutableIntStateOf(-1) }
    var detail by remember(post.id) { mutableStateOf("") }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.report_post_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.report_post_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${post.authorUsername} · ${post.carModelName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                reasonIds.forEachIndexed { index, resId ->
                    FilterChip(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        label = { Text(stringResource(resId)) }
                    )
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { if (it.length <= 800) detail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.report_post_details_hint)) },
                    minLines = 2,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedIndex !in reasonIds.indices) return@TextButton
                    onSubmit(context.getString(reasonIds[selectedIndex]), detail.trim())
                },
                enabled = selectedIndex >= 0
            ) {
                Text(stringResource(R.string.report_post_submit))
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FeedTab(
    posts: List<com.taytek.basehw.domain.model.CommunityPost>,
    isLoading: Boolean,
    isLoadingNextPage: Boolean,
    isEndReached: Boolean,
    isCreatingPost: Boolean,
    createPostError: String?,
    postCreateSuccessNonce: Int,
    isSignedIn: Boolean,
    currentUser: com.taytek.basehw.domain.model.User?,
    collectionCars: LazyPagingItems<UserCar>,
    wishlistCars: LazyPagingItems<UserCar>,
    onCreatePost: (String, UserCar?) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onReportPost: (CommunityPost) -> Unit,
    onUserClick: (String) -> Unit,
    onReactionSelect: ((String, String) -> Unit)? = null,
    onLoadMore: () -> Unit = {},
    onRefresh: () -> Unit = {},
    authorAvatars: Map<String, Pair<Int, String?>> = emptyMap()
) {
    if (!isSignedIn) {
        EmptyState(stringResource(R.string.login_required_feed))
        return
    }

    var postText by rememberSaveable { mutableStateOf("") }
    var selectedCar by remember { mutableStateOf<UserCar?>(null) }
    var showCarPicker by remember { mutableStateOf(false) }
    var carPickerTab by remember { mutableIntStateOf(0) }

    // Reset post fields on successful post
    LaunchedEffect(postCreateSuccessNonce) {
        if (postCreateSuccessNonce > 0) {
            postText = ""
            selectedCar = null
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = onRefresh
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Composer card
            item {
                FeedComposerCard(
                    postText = postText,
                    onPostTextChanged = { postText = it },
                    selectedCar = selectedCar,
                    onOpenPicker = { showCarPicker = true },
                    onClearSelectedCar = { selectedCar = null },
                    onCreatePost = { onCreatePost(postText, selectedCar) },
                    isCreatingPost = isCreatingPost,
                    errorMessage = createPostError
                )
            }

            if (isLoading && posts.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppTheme.tokens.primaryAccent)
                    }
                }
            } else if (posts.isEmpty()) {
                item {
                    EmptyState(stringResource(R.string.no_posts_yet))
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onLikeClick = { onLikeClick(post.id) },
                        onCommentClick = { onCommentClick(post.id) },
                        onUserClick = onUserClick,
                        onDeleteClick = { onDeleteClick(post.id) },
                        onReportClick = { onReportPost(post) },
                        currentUser = currentUser,
                        authorAvatars = authorAvatars,
                        onReactionSelect = onReactionSelect?.let { cb -> { emoji -> cb(post.id, emoji) } }
                    )
                }

                // Load more / end indicator
                item {
                    if (!isEndReached) {
                        LaunchedEffect(posts.size) {
                            onLoadMore()
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingNextPage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = AppTheme.tokens.primaryAccent
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showCarPicker) {
        CarPickerBottomSheet(
            selectedTab = carPickerTab,
            onTabChange = { carPickerTab = it },
            collectionCars = collectionCars,
            wishlistCars = wishlistCars,
            onCarSelected = { car ->
                selectedCar = car
                showCarPicker = false
            },
            onDismiss = { showCarPicker = false }
        )
    }
}

@Composable
private fun FollowingTab(
    posts: List<com.taytek.basehw.domain.model.CommunityPost>,
    isLoading: Boolean,
    isSignedIn: Boolean,
    currentUser: com.taytek.basehw.domain.model.User?,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onReportPost: (CommunityPost) -> Unit,
    onUserClick: (String) -> Unit,
    onReactionSelect: ((String, String) -> Unit)? = null,
    authorAvatars: Map<String, Pair<Int, String?>> = emptyMap()
) {
    if (!isSignedIn) {
        EmptyState(stringResource(R.string.login_required_feed))
        return
    }
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppTheme.tokens.primaryAccent)
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
                onReportClick = { onReportPost(post) },
                currentUser = currentUser,
                authorAvatars = authorAvatars,
                onReactionSelect = onReactionSelect?.let { cb -> { emoji -> cb(post.id, emoji) } }
            )
        }
    }
}

@Composable
private fun RankingTab(
    topUsers: List<com.taytek.basehw.domain.model.User>,
    isLoading: Boolean,
    onUserClick: (String) -> Unit,
    onRanksClick: () -> Unit = {}
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppTheme.tokens.primaryAccent)
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
        item {
            PodiumSection(topUsers.take(3), onUserClick)
        }

        // Ranks button
        item {
            OutlinedButton(
                onClick = onRanksClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AppTheme.tokens.primaryAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = AppTheme.tokens.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ranks_title),
                    color = AppTheme.tokens.primaryAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val remainingUsers = topUsers.drop(3)
        items(remainingUsers.size) { index ->
            LeaderboardItem(rank = index + 4, user = remainingUsers[index], onUserClick = onUserClick)
        }
    }
}

@Composable
private fun VerificationRequiredScreen(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppTheme.tokens.primaryAccent)
        Spacer(Modifier.height(24.dp))
        Text(text = stringResource(R.string.verification_required_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(text = stringResource(R.string.verification_required_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.tokens.primaryAccent)
        ) {
            Text(text = stringResource(R.string.refresh_status), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeedComposerCard(
    postText: String,
    onPostTextChanged: (String) -> Unit,
    selectedCar: UserCar?,
    onOpenPicker: () -> Unit,
    onClearSelectedCar: () -> Unit,
    onCreatePost: () -> Unit,
    isCreatingPost: Boolean,
    errorMessage: String?
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    val darkerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.tokens.cardBorderStandard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(baseColor, darkerColor)
                    )
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.feed_compose_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = postText,
                    onValueChange = onPostTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    placeholder = { Text(stringResource(R.string.feed_compose_hint)) },
                    enabled = !isCreatingPost,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
                )

                if (selectedCar != null) {
                    Spacer(Modifier.height(8.dp))
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                stringResource(
                                    R.string.feed_compose_selected,
                                    selectedCar.masterData?.modelName ?: selectedCar.manualModelName ?: stringResource(R.string.feed_compose_car_fallback)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.feed_compose_clear_selection),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onClearSelectedCar() }
                            )
                        }
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenPicker,
                        enabled = !isCreatingPost,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.feed_compose_pick_car),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onCreatePost,
                        enabled = !isCreatingPost && (postText.isNotBlank() || selectedCar != null),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isCreatingPost) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            stringResource(R.string.share_button),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarPickerBottomSheet(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    collectionCars: LazyPagingItems<UserCar>,
    wishlistCars: LazyPagingItems<UserCar>,
    onCarSelected: (UserCar) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.feed_picker_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { onTabChange(0) }, text = { Text(stringResource(R.string.nav_home)) })
                Tab(selected = selectedTab == 1, onClick = { onTabChange(1) }, text = { Text(stringResource(R.string.nav_wanted)) })
            }

            val activeCars = if (selectedTab == 0) collectionCars else wishlistCars

            if (activeCars.itemCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.feed_picker_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        count = activeCars.itemCount,
                        key = activeCars.itemKey { "${it.id}-${it.firestoreId}" }
                    ) { index ->
                        val car = activeCars[index] ?: return@items
                        val name = car.masterData?.modelName
                            ?: car.manualModelName
                            ?: stringResource(R.string.feed_compose_car_fallback)
                        val brand = car.masterData?.brand?.displayName
                            ?: car.manualBrand?.displayName
                            ?: ""
                        val brandIconRes = car.masterData?.brand?.toIcon()
                            ?: car.manualBrand?.toIcon()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCarSelected(car) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (brandIconRes != null) {
                                Image(
                                    painter = painterResource(id = brandIconRes),
                                    contentDescription = brand,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = AppTheme.tokens.primaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                if (brand.isNotBlank()) {
                                    Text(brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSection(users: List<com.taytek.basehw.domain.model.User>, onUserClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (users.size >= 2) {
            PodiumMember(user = users[1], rank = 2, borderColor = Color(0xFF94A3B8), imageScale = 1.0f, modifier = Modifier.weight(1f), onUserClick = onUserClick)
        } else if (users.size == 1) {
            Spacer(Modifier.weight(1f))
        }
        if (users.size >= 1) {
            PodiumMember(user = users[0], rank = 1, borderColor = AppTheme.tokens.primaryAccent, imageScale = 1.25f, isGold = true, modifier = Modifier.weight(1.2f), onUserClick = onUserClick)
        }
        if (users.size >= 3) {
            PodiumMember(user = users[2], rank = 3, borderColor = Color(0xFFB45309), imageScale = 1.0f, modifier = Modifier.weight(1f), onUserClick = onUserClick)
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
    Column(modifier = modifier.clickable { onUserClick(user.uid) }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.scale(imageScale).padding(bottom = 12.dp), contentAlignment = Alignment.BottomCenter) {
            if (isGold) {
                Icon(Icons.Default.Star, contentDescription = null, tint = AppTheme.tokens.primaryAccent, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-24).dp).scale(1.5f))
            }
            Box(modifier = Modifier.size(64.dp).border(4.dp, borderColor, CircleShape).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text(text = user.username?.take(1)?.uppercase() ?: "?", color = borderColor, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Box(modifier = Modifier.offset(y = 8.dp).background(borderColor, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(rank.toString(), color = if (isGold) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(user.username ?: "User", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (user.isAdmin) { Spacer(Modifier.width(2.dp)); Text("🛡️", fontSize = 8.sp) }
        }
        Text("${user.postCount} ${stringResource(R.string.posts_label)}", color = AppTheme.tokens.primaryAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun LeaderboardItem(rank: Int, user: com.taytek.basehw.domain.model.User, onUserClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).clickable { onUserClick(user.uid) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val rankStr = if (rank < 10) "0$rank" else rank.toString()
        Text(rankStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(AppTheme.tokens.primaryAccent.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Text(text = user.username?.take(1)?.uppercase() ?: "?", color = AppTheme.tokens.primaryAccent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.username ?: "User", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (user.isAdmin) {
                    Spacer(Modifier.width(4.dp))
                    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), shape = RoundedCornerShape(4.dp)) {
                        Text(text = "🛡️", fontSize = 8.sp, modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp))
                    }
                }
            }
            Text("${user.followerCount} ${stringResource(R.string.followers)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${user.postCount}", color = AppTheme.tokens.primaryAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.posts_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}