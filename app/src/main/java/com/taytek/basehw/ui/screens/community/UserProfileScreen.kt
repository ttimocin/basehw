@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.taytek.basehw.ui.screens.community

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.BadgeType
import com.taytek.basehw.ui.screens.community.CollectionItem
import com.taytek.basehw.domain.model.CommunityPost
import com.taytek.basehw.ui.screens.profile.ProfileViewModel
import com.taytek.basehw.ui.screens.profile.ProfileUiState
import com.taytek.basehw.ui.screens.profile.AvatarSelectionDialog
import com.taytek.basehw.ui.screens.profile.SettingsScreen
import com.taytek.basehw.ui.components.RankBadgeChip
import com.taytek.basehw.ui.theme.*
import com.taytek.basehw.ui.util.AvatarUtil
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import com.taytek.basehw.domain.model.User
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.ReadOnlyComposable

/** Brand orange in light/dark; neon pink ([ColorScheme.primary]) in Cyber so profile UI matches top bar. */
@Composable
@ReadOnlyComposable
private fun profileAccentColor(): Color =
    if (isCyberTheme()) MaterialTheme.colorScheme.primary else AppTheme.tokens.primaryAccent

@Composable
fun UserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true,
    openSettingsOnEnter: Boolean = false,
    onConsumeOpenSettings: () -> Unit = {},
    onMessageClick: (String, String) -> Unit = { _, _ -> },
    onInboxClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsOfUseClick: () -> Unit = {},
    onForumRulesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onAdminPanelClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val totalCars by profileViewModel.totalCars.collectAsState()
    val totalEstimatedValue by profileViewModel.totalValue.collectAsState()
    val currencyCode by profileViewModel.currencyCode.collectAsState()
    val currencySymbol by profileViewModel.currencySymbol.collectAsState()
    val boxedCount by profileViewModel.totalBoxed.collectAsState()
    val activeBadge by profileViewModel.activeBadge.collectAsState()
    val profileUiState by profileViewModel.uiState.collectAsState()
    val themeState by profileViewModel.themeFlow.collectAsState(initial = 0)
    val fontFamilyState by profileViewModel.fontFlow.collectAsState(initial = 0)
    val languageState by profileViewModel.languageFlow.collectAsState(initial = "")

    var showSettingsPage by rememberSaveable { mutableStateOf(openSettingsOnEnter) }
    
    // Fade-in animation for first profile screen entry to mask loading flicker
    var hasLoadedOnce by rememberSaveable { mutableStateOf(false) }
    
    // Set to true when profile is loaded from either CommunityViewModel or ProfileViewModel
    LaunchedEffect(uiState.profileUser, profileUiState.userData) {
        if (uiState.profileUser != null || profileUiState.userData != null) {
            hasLoadedOnce = true
        }
    }
    
    val contentAlpha = if (hasLoadedOnce) 1f else 0f
    val animatedAlpha by animateFloatAsState(
        targetValue = contentAlpha,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
        label = "contentAlpha"
    )

    val firebaseCurrentUser = FirebaseAuth.getInstance().currentUser
    val isMe = firebaseCurrentUser?.uid == userId

    var showFollowersDialog by rememberSaveable { mutableStateOf(false) }
    var showFollowingDialog by rememberSaveable { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<User?>(null) }
    var userToUnblock by remember { mutableStateOf<User?>(null) }

    // Only load profile if user changes or profile is empty
    // This prevents unnecessary reloads when navigating between tabs
    LaunchedEffect(userId) {
        if (viewModel.uiState.value.profileUser?.uid != userId) {
            hasLoadedOnce = false
            viewModel.loadUserProfile(userId)
        }
    }
    
    // Reload profile when avatar changes in ProfileViewModel
    var lastKnownAvatarId by rememberSaveable { mutableIntStateOf(-1) }
    var lastKnownAvatarUrl by rememberSaveable { mutableStateOf<String?>(null) }
    
    LaunchedEffect(profileUiState.userData?.selectedAvatarId, profileUiState.userData?.customAvatarUrl) {
        val data = profileUiState.userData ?: return@LaunchedEffect
        val currentAvatarId = data.selectedAvatarId ?: 1
        val currentAvatarUrl = data.customAvatarUrl
        
        // Initialize on first run
        if (lastKnownAvatarId == -1) {
            lastKnownAvatarId = currentAvatarId
            lastKnownAvatarUrl = currentAvatarUrl
            return@LaunchedEffect
        }
        
        // Reload profile when avatar actually changes
        if (lastKnownAvatarId != currentAvatarId || lastKnownAvatarUrl != currentAvatarUrl) {
            lastKnownAvatarId = currentAvatarId
            lastKnownAvatarUrl = currentAvatarUrl
            viewModel.loadUserProfile(userId)
        }
    }

    val communityUserFallback = stringResource(R.string.community_user_fallback)

    LaunchedEffect(openSettingsOnEnter, isMe) {
        if (openSettingsOnEnter && isMe) {
            showSettingsPage = true
            onConsumeOpenSettings()
        }
    }

    DisposableEffect(lifecycleOwner, isMe) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isMe) {
                viewModel.refreshInboxUnreadState()
                viewModel.loadUserProfile(userId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showSettingsPage && isMe) {
        SettingsScreen(
            uiState = uiState,
            profileUiState = profileUiState,
            profileViewModel = profileViewModel,
            themeState = themeState,
            fontFamilyState = fontFamilyState,
            languageState = languageState,
            currencyCode = currencyCode,
            onBack = { showSettingsPage = false },
            onUpdateCollectionVisibility = profileViewModel::updateCollectionVisibility,
            onUpdateWishlistVisibility = profileViewModel::updateWishlistVisibility,
            onBackup = profileViewModel::backupToCloud,
            onRestore = profileViewModel::restoreFromCloud,
            onExport = profileViewModel::onExportClick,
            onSetTheme = profileViewModel::setTheme,
            onSetFontFamily = profileViewModel::setFontFamily,
            onSetLanguage = profileViewModel::setLanguage,
            onSetCurrency = profileViewModel::setCurrency,
            onOpenPrivacyPolicy = {
                onPrivacyPolicyClick()
            },
            onOpenTermsOfUse = {
                onTermsOfUseClick()
            },
            onOpenForumRules = {
                onForumRulesClick()
            },
            onOpenSupport = {
                onSupportClick()
            },
            onLogout = {
                profileViewModel.signOut()
                showSettingsPage = false
            },
            onDeleteAccount = {
                profileViewModel.deleteAccount()
                showSettingsPage = false
            },
            onAdminPanelClick = onAdminPanelClick,
            isAuthenticated = firebaseCurrentUser != null && !firebaseCurrentUser.isAnonymous
        )
        return
    }

    if (!isMe) {
        // Başkasının profili → OtherUserProfileScreen kullan
        OtherUserProfileScreen(
            userId = userId,
            onNavigateBack = onNavigateBack,
            onMessageClick = onMessageClick,
            onAdminPanelClick = onAdminPanelClick,
            onFollowersClick = {
                viewModel.loadProfileFollowers(userId)
                showFollowersDialog = true
            },
            onFollowingClick = {
                viewModel.loadProfileFollowing(userId)
                showFollowingDialog = true
            },
            uiState = uiState,
            isFollowing = uiState.isFollowingProfile,
            isFollowActionLoading = uiState.isFollowActionLoading,
            onToggleFollow = { viewModel.toggleFollow(userId) }
        )

        if (showFollowersDialog) {
            FollowUsersDialog(
                title = stringResource(R.string.followers),
                users = uiState.profileFollowers,
                isLoading = uiState.isLoadingFollowUsers,
                onDismiss = { showFollowersDialog = false },
                isFollowersList = true,
                isMe = isMe,
                onActionClick = { /* Other profile, can't block here currently */ },
                onMessageClick = { u -> onMessageClick(u.uid, u.username ?: communityUserFallback) },
                onProfileClick = { u -> /* Navigate to profile */ }
            )
        }

        if (showFollowingDialog) {
            FollowUsersDialog(
                title = stringResource(R.string.following_label),
                users = uiState.profileFollowing,
                isLoading = uiState.isLoadingFollowUsers,
                onDismiss = { showFollowingDialog = false },
                isFollowersList = false,
                isMe = isMe,
                onActionClick = { /* Not me, can't unfollow for them */ },
                onMessageClick = { u -> onMessageClick(u.uid, u.username ?: communityUserFallback) },
                onProfileClick = { u -> /* Navigate to profile */ }
            )
        }

        return
    }

    // Avatar picker launcher - composable scope'ta tanımlanmalı
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->

        uri?.let { 

            profileViewModel.uploadCustomAvatar(it)
        } ?: android.util.Log.w("UserProfileScreen", "URI is null, avatar not selected")
    }

    // Block confirmation dialog
    if (userToBlock != null) {
        AlertDialog(
            onDismissRequest = { userToBlock = null },
            title = { Text(stringResource(R.string.block_user_title)) },
            text = { Text(stringResource(R.string.block_user_message, userToBlock?.username ?: "User")) },
            confirmButton = {
                TextButton(onClick = {
                    userToBlock?.let { viewModel.blockFollower(it.uid) }
                    userToBlock = null
                }) {
                    Text(stringResource(R.string.block_user_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToBlock = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Unblock confirmation dialog
    if (userToUnblock != null) {
        AlertDialog(
            onDismissRequest = { userToUnblock = null },
            title = { Text(stringResource(R.string.unblock_user_title)) },
            text = { Text(stringResource(R.string.unblock_user_message, userToUnblock?.username ?: "User")) },
            confirmButton = {
                TextButton(onClick = {
                    userToUnblock?.let { viewModel.unblockUser(it.uid) }
                    userToUnblock = null
                }) {
                    Text(stringResource(R.string.unblock_user_confirm), color = profileAccentColor())
                }
            },
            dismissButton = {
                TextButton(onClick = { userToUnblock = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showFollowersDialog) {
        FollowUsersDialog(
            title = stringResource(R.string.followers),
            users = uiState.profileFollowers,
            isLoading = uiState.isLoadingFollowUsers,
            onDismiss = { showFollowersDialog = false },
            isFollowersList = true,
            isMe = isMe,
            onActionClick = { u -> userToBlock = u },
            onMessageClick = { u -> onMessageClick(u.uid, u.username ?: communityUserFallback) },
            onProfileClick = { u -> /* Navigate to profile if needed */ },
            blockedUsers = uiState.blockedUsers,
            onUnblockClick = { u -> userToUnblock = u }
        )
    }

    if (showFollowingDialog) {
        FollowUsersDialog(
            title = stringResource(R.string.following_label),
            users = uiState.profileFollowing,
            isLoading = uiState.isLoadingFollowUsers,
            onDismiss = { showFollowingDialog = false },
            isFollowersList = false,
            isMe = isMe,
            onActionClick = { u -> /* not used for following list */ },
            onToggleFollowClick = { uid, isFollowing -> viewModel.toggleFollowInList(uid, isFollowing) },
            onMessageClick = { u -> onMessageClick(u.uid, u.username ?: communityUserFallback) },
            onProfileClick = { u -> /* Navigate to profile if needed */ }
        )
    }

    // Post delete dialog
    var postToDelete by remember { mutableStateOf<CommunityPost?>(null) }
    
    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text(stringResource(R.string.delete_post_title)) },
            text = { Text(stringResource(R.string.delete_post_msg, postToDelete?.carModelName ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        postToDelete?.let { viewModel.deletePost(it.id) }
                        postToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Logout handler
    val context = androidx.compose.ui.platform.LocalContext.current
    val onLogoutClick: () -> Unit = {
        // Clear credential state for auto-login
        try {
            val credentialManager = androidx.credentials.CredentialManager.create(context)
            kotlinx.coroutines.MainScope().launch {
                try {
                    credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
                } catch (e: Exception) {
                    android.util.Log.e("UserProfileScreen", "Clear credential state error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UserProfileScreen", "CredentialManager create error: ${e.message}")
        }
        profileViewModel.signOut()
    }

    if (firebaseCurrentUser != null && !firebaseCurrentUser.isAnonymous && profileUiState.showMandatoryConsentDialog) {
        MandatoryConsentDialog(
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onTermsOfUseClick = onTermsOfUseClick,
            onAccept = { profileViewModel.acceptMandatoryConsent() },
            onDecline = { profileViewModel.declineMandatoryConsent() }
        )
    }

    // Own Profile Page - Same style as OtherUserProfile but with Posts list
    OwnProfilePage(
        uiState = uiState,
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
        totalCars = totalCars,
        totalEstimatedValue = totalEstimatedValue.roundToInt(),
        currencySymbol = currencySymbol,
        boxedCount = boxedCount,
        activeBadge = activeBadge,
        onSettingsClick = {
            showSettingsPage = true
            onSettingsClick()
        },
        onStatsClick = onStatsClick,
        onInboxClick = onInboxClick,
        onNotificationsClick = onNotificationsClick,
        onFollowersClick = {
            viewModel.loadProfileFollowers(userId)
            viewModel.loadBlockedUsers()
            showFollowersDialog = true
        },
        onFollowingClick = {
            viewModel.loadProfileFollowing(userId)
            showFollowingDialog = true
        },
        onLogoutClick = onLogoutClick,
        onEditProfileClick = onEditProfileClick,
        profileViewModel = profileViewModel,
        profileUiState = profileUiState,
        avatarPickerLauncher = avatarPickerLauncher,
        contentAlpha = animatedAlpha,
        onPostLongClick = { postToDelete = it },
        userId = userId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnProfilePage(
    uiState: CommunityUiState,
    showBackButton: Boolean,
    onNavigateBack: () -> Unit,
    totalCars: Int,
    totalEstimatedValue: Int,
    currencySymbol: String,
    boxedCount: Int,
    activeBadge: BadgeType,
    onSettingsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onStatsClick: () -> Unit,
    onInboxClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onLogoutClick: () -> Unit,
    profileViewModel: ProfileViewModel,
    profileUiState: ProfileUiState,
    avatarPickerLauncher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest>,
    contentAlpha: Float = 1f,
    onPostLongClick: ((CommunityPost) -> Unit)? = null,
    userId: String
) {
    // App Theme Colors
    val bg = cyberRootBackgroundColor()
    val accent = profileAccentColor()
    val primaryText = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant

    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_profile),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = if (isCyberTheme()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    primaryText
                                }
                            )
                        }
                    }
                },
                actions = {
                    val actionIconTint = if (isCyberTheme()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        primaryText
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
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                                tint = actionIconTint
                            )
                        }
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = stringResource(id = R.string.logout),
                                tint = if (isCyberTheme()) {
                                    actionIconTint
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bg,
                    actionIconContentColor = if (isCyberTheme()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        primaryText
                    },
                    navigationIconContentColor = if (isCyberTheme()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        primaryText
                    }
                )
            )
        },
        containerColor = bg
    ) { paddingValues ->
        if (uiState.isLoadingProfile) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .alpha(contentAlpha)
            ) {
                Spacer(Modifier.height(16.dp))

                // Profile Header Card - Same style as OtherUserProfile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(
                        1.dp,
                        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                            AppTheme.tokens.cardBorderMuted
                        } else {
                            AppTheme.tokens.cardBorderStandard
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Avatar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Avatar - clickable to change
                            val showAvatarDialog = remember { mutableStateOf(false) }
                            
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clickable { showAvatarDialog.value = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(78.dp)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val selectedAvatarId = uiState.profileUser?.selectedAvatarId ?: 1
                                    val customAvatarUrl = uiState.profileUser?.customAvatarUrl

                                    if (selectedAvatarId == 0 && !customAvatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = customAvatarUrl,
                                            contentDescription = stringResource(R.string.avatar_desc),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (selectedAvatarId > 0) {
                                        val avatarResId = AvatarUtil.getAvatarResource(selectedAvatarId)
                                        Image(
                                            painter = painterResource(id = avatarResId),
                                            contentDescription = stringResource(R.string.avatar_desc),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text(
                                            text = (uiState.profileUser?.username?.take(1) ?: "?").uppercase(),
                                            color = accent,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 28.sp
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(28.dp)
                                        .shadow(6.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(accent)
                                        .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.edit),
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            // Avatar Selection Dialog
                            if (showAvatarDialog.value) {
                                AvatarSelectionDialog(
                                    onDismiss = { showAvatarDialog.value = false },
                                    onSelectDefault = { avatarId ->
                                        profileViewModel.selectDefaultAvatar(avatarId)
                                        showAvatarDialog.value = false
                                    },
                                    onSelectFromGallery = {
                                        showAvatarDialog.value = false
                                        // Launcher'ı dialog kapandıktan sonra çağır
                                        kotlinx.coroutines.MainScope().launch {
                                            kotlinx.coroutines.delay(100)
                                            avatarPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    }
                                )
                            }

                            // User Info
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.profileUser?.username ?: stringResource(R.string.community_user_fallback),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = primaryText,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (uiState.profileUser?.isAdmin == true) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.community_admin_badge),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (uiState.profileUser?.isMod == true) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFF4CAF50).copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.mod_badge),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = onEditProfileClick,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit_profile),
                                            tint = accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                RankBadgeChip(
                                    badge = activeBadge,
                                    compact = true
                                )
                                
                                Spacer(Modifier.height(8.dp))
                                
                                // Stats Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    FigmaStatItem(
                                        value = uiState.profileUser?.followerCount?.let { formatCount(it) } ?: "0",
                                        label = stringResource(R.string.followers),
                                        onClick = onFollowersClick
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    FigmaStatItem(
                                        value = uiState.profileUser?.followingCount?.let { formatCount(it) } ?: "0",
                                        label = stringResource(R.string.following_label),
                                        onClick = onFollowingClick
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    FigmaStatItem(
                                        value = (uiState.profileUser?.postCount ?: 0).toString(),
                                        label = stringResource(R.string.posts_label)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Collection Stats Cards - Same style as summary cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FigmaSummaryCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.stat_cars).uppercase(),
                        count = totalCars.toString()
                    )
                    FigmaSummaryCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.stat_value).uppercase(),
                        count = "$currencySymbol${totalEstimatedValue.toString()}"
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Statistics Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStatsClick() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = accent.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, AppTheme.tokens.cardBorderMuted)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.statistics_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                
                // Email Verification Banner for current user
                if (!profileUiState.isEmailVerified) {
                    EmailVerificationSection(
                        isLoading = profileUiState.isLoadingVerification,
                        emailSent = profileUiState.verificationEmailSent,
                        onSendVerification = profileViewModel::sendEmailVerification,
                        onCheckStatus = profileViewModel::reloadUser
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // Posts Section
                Text(
                    text = stringResource(R.string.posts_section_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Posts List
                if (uiState.profilePosts.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(1.dp, AppTheme.tokens.cardBorderStandard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = secondaryText,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.profile_no_posts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = secondaryText
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.profilePosts.forEach { post ->
                            CommunityPostCard(
                                post = post,
                                onDeleteClick = onPostLongClick
                            )
                        }
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    onDeleteClick: ((CommunityPost) -> Unit)? = null
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    val darkerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onDeleteClick != null) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { onDeleteClick(post) }
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.dp, AppTheme.tokens.cardBorderStandard)
    ) {
        Column(modifier = Modifier
            .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
            .padding(12.dp)
        ) {
            // Car info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = profileAccentColor(),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.carModelName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = post.carBrand,
                        style = MaterialTheme.typography.labelSmall,
                        color = profileAccentColor(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Like and Comment counts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like count
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = profileAccentColor(),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${post.likeCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = profileAccentColor(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Comment count
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${post.commentCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Post caption if exists
            if (post.caption.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Car image
            if (post.carImageUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    AsyncImage(
                        model = post.carImageUrl,
                        contentDescription = post.carModelName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun MandatoryConsentDialog(
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.mandatory_consent_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.mandatory_consent_desc), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onPrivacyPolicyClick) { Text(stringResource(R.string.privacy_policy)) }
                    Text("•", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onTermsOfUseClick) { Text(stringResource(R.string.terms_of_use)) }
                }
            }
        },
        confirmButton = { Button(onClick = onAccept) { Text(stringResource(R.string.accept_rules_btn)) } },
        dismissButton = { TextButton(onClick = onDecline) { Text(stringResource(R.string.cancel_btn)) } }
    )
}

@Composable
private fun FigmaStatItem(
    value: String,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCyberTheme()) Color.White else profileAccentColor()
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FigmaSummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    count: String
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    val darkerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            1.dp,
            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                AppTheme.tokens.cardBorderMuted
            } else {
                AppTheme.tokens.cardBorderStandard
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
                .padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}k"
        else -> count.toString()
    }
}

@Composable
private fun CollectionTab(uiState: CommunityUiState, useFigmaStyle: Boolean = false) {
    var showAllDialog by rememberSaveable { mutableStateOf(false) }

    if (uiState.profileUser?.isCollectionPublic != true) {
        SectionCard(
            title = stringResource(R.string.collection_summary),
            subtitle = stringResource(R.string.collection_visibility_private),
            useFigmaStyle = useFigmaStyle
        ) {
            Text(
                text = stringResource(R.string.atakas_hidden_collection_private),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    if (uiState.profileCollectionItems.isEmpty()) {
        SectionCard(
            title = stringResource(R.string.collection_summary),
            subtitle = stringResource(R.string.empty_collection_title),
            useFigmaStyle = useFigmaStyle
        ) {
            Text(
                text = stringResource(R.string.empty_collection_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Collection grid with images
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.collection_tab_title_simple).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (uiState.profileCollectionItems.size > 3) {
                Text(
                    text = stringResource(R.string.see_all).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = profileAccentColor(),
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { showAllDialog = true }
                )
            }
        }

        // 3-column grid for first 3 items
        val previewItems = uiState.profileCollectionItems.take(3)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            previewItems.forEach { item ->
                CollectionImageCard(
                    modifier = Modifier.weight(1f),
                    item = item
                )
            }
            // Fill remaining space if less than 3 items
            repeat(3 - previewItems.size) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                )
            }
        }

        // Count badge
        Text(
            text = stringResource(R.string.collection_count, uiState.profileCollectionItems.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    // "View All" dialog
    if (showAllDialog) {
        CollectionListDialog(
            items = uiState.profileCollectionItems,
            onDismiss = { showAllDialog = false }
        )
    }
}

@Composable
private fun WishlistTab(uiState: CommunityUiState, useFigmaStyle: Boolean = false) {
    if (uiState.profileUser?.isWishlistPublic != true) {
        SectionCard(
            title = stringResource(R.string.wishlist_title),
            subtitle = stringResource(R.string.wishlist_visibility_private),
            useFigmaStyle = useFigmaStyle
        ) {
            Text(
                text = stringResource(R.string.wishlist_visibility_private),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    SectionCard(
        title = stringResource(R.string.wishlist_title),
        subtitle = if (uiState.profileWishlistTitles.isNotEmpty()) {
            stringResource(R.string.empty_wishlist_subtitle)
        } else {
            stringResource(R.string.empty_wishlist_title)
        },
        useFigmaStyle = useFigmaStyle
    ) {
        if (uiState.profileWishlistTitles.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_wishlist_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.profileWishlistTitles.take(12).forEach { title ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = profileAccentColor(),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeMatchesTab(uiState: CommunityUiState, useFigmaStyle: Boolean = false) {
    SectionCard(
        title = stringResource(R.string.atakas_title),
        subtitle = stringResource(R.string.atakas_found, uiState.profileAtakasCount + uiState.profileReverseAtakasCount),
        useFigmaStyle = useFigmaStyle
    ) {
        if (uiState.profileUser?.isCollectionPublic == true) {
            Text(
                text = stringResource(R.string.atakas_found, uiState.profileAtakasCount),
                style = MaterialTheme.typography.bodyMedium,
                color = profileAccentColor(),
                fontWeight = FontWeight.SemiBold
            )
            if (uiState.profileAtakasPreview.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uiState.profileAtakasPreview.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = stringResource(R.string.atakas_hidden_collection_private),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(10.dp))

        if (uiState.profileUser?.isWishlistPublic == true) {
            Text(
                text = stringResource(R.string.trade_match_reverse_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.trade_match_reverse_found, uiState.profileReverseAtakasCount),
                style = MaterialTheme.typography.bodyMedium,
                color = profileAccentColor(),
                fontWeight = FontWeight.SemiBold
            )
            if (uiState.profileReverseAtakasPreview.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uiState.profileReverseAtakasPreview.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = stringResource(R.string.trade_match_reverse_hidden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    useFigmaStyle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    val darkerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val border = if (useFigmaStyle) {
        BorderStroke(
            1.dp,
            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                AppTheme.tokens.cardBorderMuted
            } else {
                AppTheme.tokens.cardBorderStandard
            }
        )
    } else {
        BorderStroke(
            1.dp,
            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                AppTheme.tokens.cardBorderMuted
            } else {
                AppTheme.tokens.cardBorderStandard
            }
        )
    }
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerHigh)))
            .padding(12.dp), 
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (useFigmaStyle) MaterialTheme.colorScheme.onSurface else LocalContentColor.current
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor
                )
            }
            content()
        }
    }
}

@Composable
fun FollowUsersDialog(
    title: String,
    users: List<User>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    isFollowersList: Boolean,
    isMe: Boolean,
    onActionClick: (User) -> Unit,
    onMessageClick: (User) -> Unit,
    onProfileClick: (User) -> Unit,
    blockedUsers: List<User> = emptyList(),
    onUnblockClick: (User) -> Unit = {},
    onToggleFollowClick: (String, Boolean) -> Unit = { _, _ -> }
) {
    // Track toggled (unfollowed) UIDs locally in dialog
    val toggledUids = remember { mutableStateListOf<String>() }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = profileAccentColor())
                    }
                } else if (users.isEmpty() && blockedUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.empty_list_fallback),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(users) { user ->
                            val isUnfollowed = user.uid in toggledUids
                            FollowUserRow(
                                user = user,
                                isMe = isMe,
                                isFollowersList = isFollowersList,
                                isUnfollowed = isUnfollowed,
                                onProfileClick = onProfileClick,
                                onActionClick = if (isFollowersList) {
                                    { u -> onActionClick(u) }
                                } else {
                                    { u ->
                                        val currentlyFollowing = u.uid !in toggledUids
                                        if (currentlyFollowing) toggledUids.add(u.uid) else toggledUids.remove(u.uid)
                                        onToggleFollowClick(u.uid, currentlyFollowing)
                                    }
                                },
                                onMessageClick = onMessageClick
                            )
                        }

                        // Blocked users section - only for own followers list
                        if (isMe && isFollowersList && blockedUsers.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.blocked_users_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            items(blockedUsers) { user ->
                                BlockedUserRow(
                                    user = user,
                                    onUnblockClick = onUnblockClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUserRow(
    user: User,
    isMe: Boolean,
    isFollowersList: Boolean,
    isUnfollowed: Boolean = false,
    onProfileClick: (User) -> Unit,
    onActionClick: (User) -> Unit,
    onMessageClick: (User) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick(user) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        UserAvatarSmall(user = user)

        // İsim
        Text(
            text = user.username ?: "User",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        // Butonlar
        if (isMe) {
            if (!isFollowersList) {
                // Following listesi: toggle follow/unfollow
                IconButton(onClick = { onActionClick(user) }) {
                    Icon(
                        if (isUnfollowed) Icons.Default.PersonAdd else Icons.Default.PersonRemove,
                        contentDescription = stringResource(R.string.unfollow),
                        tint = if (isUnfollowed) profileAccentColor() else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { onMessageClick(user) }) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = stringResource(R.string.message_desc),
                    tint = profileAccentColor()
                )
            }
            if (isFollowersList) {
                IconButton(onClick = { onActionClick(user) }) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = stringResource(R.string.block_user_confirm),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedUserRow(
    user: User,
    onUnblockClick: (User) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.7f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        UserAvatarSmall(user = user)

        // İsim
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username ?: "User",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Engeli Kaldır butonu
        TextButton(
            onClick = { onUnblockClick(user) },
            colors = ButtonDefaults.textButtonColors(
                contentColor = profileAccentColor()
            )
        ) {
            Icon(
                Icons.Default.LockOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.unblock_user_confirm),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UserAvatarSmall(user: User) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(profileAccentColor().copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        val selectedAvatarId = user.selectedAvatarId
        val customAvatarUrl = user.customAvatarUrl

        if (selectedAvatarId == 0 && !customAvatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = customAvatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (selectedAvatarId > 0) {
            val avatarResId = AvatarUtil.getAvatarResource(selectedAvatarId)
            Image(
                painter = painterResource(id = avatarResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = (user.username?.take(1) ?: "?").uppercase(),
                color = profileAccentColor(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AvatarSelectionDialog(
    onDismiss: () -> Unit,
    onSelectDefault: (Int) -> Unit,
    onSelectFromGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.avatar_pick_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Default avatars grid
                Text(stringResource(R.string.avatar_default_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..AvatarUtil.getAvatarCount()) {
                        val avatarResId = AvatarUtil.getAvatarResource(i)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(profileAccentColor().copy(alpha = 0.15f))
                                .clickable { onSelectDefault(i) },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = avatarResId),
                                contentDescription = stringResource(R.string.avatar_desc),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                // Gallery button
                Button(
                    onClick = onSelectFromGallery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.avatar_upload_gallery))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.avatar_cancel))
            }
        }
    )
}

@Composable
internal fun CollectionImageCard(
    modifier: Modifier = Modifier,
    item: CollectionItem
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, AppTheme.tokens.cardBorderStandard)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = profileAccentColor().copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            // Title overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(6.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
internal fun CollectionListDialog(
    items: List<CollectionItem>,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.collection_tab_title_simple),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.collection_count, items.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items.size / 2 + 1) { row ->
                        if (row * 2 < items.size) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CollectionImageCard(
                                    modifier = Modifier.weight(1f),
                                    item = items[row * 2]
                                )
                                if (row * 2 + 1 < items.size) {
                                    CollectionImageCard(
                                        modifier = Modifier.weight(1f),
                                        item = items[row * 2 + 1]
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailVerificationSection(
    isLoading: Boolean,
    emailSent: Boolean,
    onSendVerification: () -> Unit,
    onCheckStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Unpublished,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.email_not_verified),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Text(
                text = stringResource(R.string.verification_required_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (emailSent) {
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.verification_sent),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSendVerification,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    enabled = !isLoading && !emailSent
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(stringResource(R.string.send_verification_btn), style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                OutlinedButton(
                    onClick = onCheckStatus,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.check_status_btn), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

