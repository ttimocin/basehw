package com.taytek.basehw.ui.screens.community

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.User
import com.taytek.basehw.ui.theme.AppTheme
import com.taytek.basehw.ui.util.AvatarUtil
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    uiState: CommunityUiState,
    onNavigateBack: () -> Unit,
    onSetModerator: (String, Boolean) -> Unit,
    onBanUser: (String, String?) -> Unit,
    onUnbanUser: (String) -> Unit,
    onLoadUsers: () -> Unit,
    onLoadBannedUsers: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showBanDialog by remember { mutableStateOf<User?>(null) }
    var banReason by remember { mutableStateOf("") }
    var hasLoadedData by remember { mutableStateOf(false) }
    var userSearchQuery by remember { mutableStateOf("") }
    var bannedSearchQuery by remember { mutableStateOf("") }

    // Load data on first entry
    LaunchedEffect(Unit) {
        if (!hasLoadedData) {
            onLoadUsers()
            onLoadBannedUsers()
            hasLoadedData = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.admin_panel_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
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
                .padding(horizontal = 16.dp)
        ) {
            // Filter users based on search query
            val filteredUsers = uiState.allUsers.filter { user ->
                val query = userSearchQuery.trim()
                if (query.isEmpty()) true
                else {
                    val username = user.username?.lowercase() ?: ""
                    val email = user.email.lowercase()
                    val searchLower = query.lowercase()
                    username.contains(searchLower) || email.contains(searchLower)
                }
            }

            val filteredBannedUsers = uiState.bannedUsers.filter { user ->
                val query = bannedSearchQuery.trim()
                if (query.isEmpty()) true
                else {
                    val username = user.username?.lowercase() ?: ""
                    val email = user.email.lowercase()
                    val searchLower = query.lowercase()
                    username.contains(searchLower) || email.contains(searchLower)
                }
            }

            // Tab Row with counts
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AppTheme.tokens.primaryAccent,
                edgePadding = 0.dp,
                indicator = {},
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = AppTheme.tokens.primaryAccent,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = if (userSearchQuery.isNotBlank()) stringResource(R.string.admin_tab_members_with_count, filteredUsers.size) else stringResource(R.string.admin_tab_members),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = AppTheme.tokens.primaryAccent,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = if (bannedSearchQuery.isNotBlank()) stringResource(R.string.admin_tab_banned_with_count, filteredBannedUsers.size) else stringResource(R.string.admin_tab_banned),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> UsersTab(
                    users = filteredUsers,
                    isLoading = uiState.isLoadingAllUsers,
                    onSetModerator = onSetModerator,
                    onBanUser = { user -> showBanDialog = user },
                    onUnbanUser = onUnbanUser,
                    searchQuery = userSearchQuery,
                    onSearchQueryChange = { userSearchQuery = it }
                )
                1 -> BannedUsersTab(
                    users = filteredBannedUsers,
                    isLoading = uiState.isLoadingBannedUsers,
                    onUnbanUser = onUnbanUser,
                    searchQuery = bannedSearchQuery,
                    onSearchQueryChange = { bannedSearchQuery = it }
                )
            }
        }
    }

    // Ban Dialog
    if (showBanDialog != null) {
        AlertDialog(
            onDismissRequest = { showBanDialog = null },
            title = { Text(stringResource(R.string.admin_ban_user_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.admin_ban_reason_determining, showBanDialog?.username ?: ""))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = banReason,
                        onValueChange = { banReason = it },
                        label = { Text(stringResource(R.string.admin_ban_reason_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBanDialog?.let { user ->
                            onBanUser(user.uid, banReason.ifBlank { null })
                            banReason = ""
                        }
                        showBanDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.admin_ban_confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBanDialog = null
                    banReason = ""
                }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    // Error display
    uiState.adminPanelError?.let { error ->
        LaunchedEffect(error) {
            // Could show a snackbar here
        }
    }
}

@Composable
private fun UsersTab(
    users: List<User>,
    isLoading: Boolean,
    onSetModerator: (String, Boolean) -> Unit,
    onBanUser: (User) -> Unit,
    onUnbanUser: (String) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {}
) {
    Column {
        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.admin_search_users_hint)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(id = R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.tokens.primaryAccent)
            }
        } else if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) stringResource(R.string.admin_no_search_results) else stringResource(R.string.admin_no_users_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { user ->
                    AdminUserRow(
                        user = user,
                        onSetModerator = onSetModerator,
                        onBanUser = onBanUser,
                        onUnbanUser = onUnbanUser
                    )
                }
            }
        }
    }
}

/**
 * Wrapper composable that creates its own ViewModel - used for navigation
 * Only admins can access this panel
 */
@Composable
fun AdminPanelRoute(
    onNavigateBack: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isAdmin by remember { mutableStateOf<Boolean?>(null) }
    
    // Check admin status directly from Supabase using callback
    LaunchedEffect(Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModel.checkIsAdmin(currentUser.uid) { result ->
                isAdmin = result
                if (!result) {
                    onNavigateBack()
                }
            }
        } else {
            onNavigateBack()
        }
    }
    
    // Show loading while checking
    if (isAdmin == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppTheme.tokens.primaryAccent)
        }
        return
    }
    
    if (isAdmin != true) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.admin_access_denied), color = MaterialTheme.colorScheme.error)
        }
        return
    }
    
    // Load data on enter
    LaunchedEffect(Unit) {
        viewModel.loadAllUsers()
        viewModel.loadBannedUsers()
    }
    
    AdminPanelScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSetModerator = { uid, isMod -> viewModel.setModerator(uid, isMod) },
        onBanUser = { uid, reason -> viewModel.banUserFromForum(uid, reason) },
        onUnbanUser = { uid -> viewModel.unbanUserFromForum(uid) },
        onLoadUsers = { viewModel.loadAllUsers() },
        onLoadBannedUsers = { viewModel.loadBannedUsers() }
    )
}

@Composable
private fun BannedUsersTab(
    users: List<User>,
    isLoading: Boolean,
    onUnbanUser: (String) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {}
) {
    Column {
        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.admin_search_banned_hint)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(id = R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.tokens.primaryAccent)
            }
        } else if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) stringResource(R.string.admin_no_search_results) else stringResource(R.string.admin_no_banned_users),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { user ->
                    BannedUserRow(
                        user = user,
                        onUnbanUser = onUnbanUser
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminUserRow(
    user: User,
    onSetModerator: (String, Boolean) -> Unit,
    onBanUser: (User) -> Unit,
    onUnbanUser: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isForumBanned) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppTheme.tokens.primaryAccent.copy(alpha = 0.15f)),
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
                        color = AppTheme.tokens.primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username ?: "User",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (user.isAdmin) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.community_admin_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                fontSize = 7.sp,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (user.isMod) {
                        Surface(
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.community_mod_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                fontSize = 7.sp,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                    if (user.isForumBanned) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.admin_badge_banned_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                fontSize = 7.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.admin_posts_count_label, user.postCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (!user.isAdmin) {
                    TextButton(
                        onClick = { onSetModerator(user.uid, !user.isMod) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (user.isMod) stringResource(R.string.admin_remove_mod) else stringResource(R.string.admin_make_mod),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                if (!user.isAdmin) {
                    if (user.isForumBanned) {
                        TextButton(
                            onClick = { onUnbanUser(user.uid) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = AppTheme.tokens.primaryAccent
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.admin_unban_confirm),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    } else {
                        TextButton(
                            onClick = { onBanUser(user) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.admin_ban_confirm),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannedUserRow(
    user: User,
    onUnbanUser: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
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
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username ?: "User",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // Unban button
            TextButton(
                onClick = { onUnbanUser(user.uid) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppTheme.tokens.primaryAccent
                )
            ) {
                Icon(
                    Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.admin_unban_confirm),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
