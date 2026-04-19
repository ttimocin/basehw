package com.taytek.basehw.ui.screens.community

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.CommunityPost
import com.taytek.basehw.domain.model.User
import com.taytek.basehw.ui.components.RankBadgeChip
import com.taytek.basehw.ui.theme.*
import com.taytek.basehw.ui.util.AvatarUtil
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onMessageClick: (String, String) -> Unit = { _, _ -> },
    onAdminPanelClick: () -> Unit = {},
    onFollowClick: () -> Unit = {},
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    uiState: CommunityUiState,
    isFollowing: Boolean,
    isFollowActionLoading: Boolean,
    onToggleFollow: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val communityUserFallback = stringResource(R.string.community_user_fallback)

    val bg = MaterialTheme.colorScheme.background
    val accent = AppTheme.tokens.primaryAccent
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryText = MaterialTheme.colorScheme.onBackground

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.profileUser?.username ?: communityUserFallback,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = primaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
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
            ) {
                Spacer(Modifier.height(16.dp))

                // Profile Header Card
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
                        // Avatar + (header row with actions) + stats — actions sit top-right so stats get full width
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Avatar - Same style as UserProfileScreen
                            Box(
                                modifier = Modifier
                                    .size(92.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(78.dp)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(AppTheme.tokens.primaryAccent.copy(alpha = 0.15f)),
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
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = uiState.profileUser?.username ?: communityUserFallback,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = primaryText,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            if (uiState.profileUser?.isAdmin == true) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    color = accent.copy(alpha = 0.18f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.community_admin_badge),
                                                        color = accent,
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
                                                        text = "🛡️ MOD",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        RankBadgeChip(
                                            badge = uiState.profileUser?.activeBadge ?: com.taytek.basehw.domain.model.BadgeType.ROOKIE,
                                            compact = true
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onToggleFollow() },
                                            enabled = !isFollowActionLoading,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            if (isFollowActionLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = accent
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                                                    contentDescription = if (isFollowing) stringResource(R.string.unfollow) else stringResource(R.string.follow),
                                                    tint = if (isFollowing) secondaryText else accent,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                val username = uiState.profileUser?.username ?: communityUserFallback
                                                onMessageClick(userId, username)
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = stringResource(R.string.message_desc),
                                                tint = accent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Stats row uses full text column width (no icons beside it)
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

                // Collection & Wishlist Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FigmaSummaryCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.collection_tab_title_simple).uppercase(),
                        count = uiState.profileCollectionTitles.size.toString()
                    )
                    FigmaSummaryCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.wishlist_title_simple).uppercase(),
                        count = uiState.profileWishlistTitles.size.toString()
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Tab Bar
                val tabTitles = listOf(
                    stringResource(R.string.collection_summary),
                    stringResource(R.string.wishlist_title),
                    stringResource(R.string.atakas_title)
                )

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = accent,
                    edgePadding = 0.dp,
                    indicator = {},
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val selected = selectedTab == index
                        Tab(
                            selected = selected,
                            onClick = { selectedTab = index },
                            selectedContentColor = accent,
                            unselectedContentColor = secondaryText,
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> OtherProfileCollectionTab(uiState = uiState)
                    1 -> OtherProfileWishlistTab(uiState = uiState)
                    else -> OtherProfileTradeMatchesTab(uiState = uiState)
                }

                Spacer(Modifier.height(86.dp))
            }
        }
    }
}

// Collection Tab for Other Profile
@Composable
private fun OtherProfileCollectionTab(uiState: CommunityUiState) {
    var showAllDialog by rememberSaveable { mutableStateOf(false) }

    if (uiState.profileUser?.isCollectionPublic != true) {
        SectionCard(
            title = stringResource(R.string.collection_summary),
            subtitle = stringResource(R.string.collection_visibility_private),
            useFigmaStyle = true
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
            useFigmaStyle = true
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
                    color = AppTheme.tokens.primaryAccent,
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

// Wishlist Tab for Other Profile
@Composable
private fun OtherProfileWishlistTab(uiState: CommunityUiState) {
    if (uiState.profileUser?.isWishlistPublic != true) {
        SectionCard(
            title = stringResource(R.string.wishlist_title),
            subtitle = stringResource(R.string.wishlist_visibility_private),
            useFigmaStyle = true
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
        useFigmaStyle = true
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
                            tint = AppTheme.tokens.primaryAccent,
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

// Trade Matches Tab for Other Profile
@Composable
private fun OtherProfileTradeMatchesTab(uiState: CommunityUiState) {
    SectionCard(
        title = stringResource(R.string.atakas_title),
        subtitle = stringResource(R.string.atakas_found, uiState.profileAtakasCount + uiState.profileReverseAtakasCount),
        useFigmaStyle = true
    ) {
        if (uiState.profileUser?.isCollectionPublic == true) {
            Text(
                text = stringResource(R.string.atakas_found, uiState.profileAtakasCount),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.tokens.primaryAccent,
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
                color = AppTheme.tokens.primaryAccent,
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

// Shared helper components
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
            color = if (isCyberTheme()) Color.White else AppTheme.tokens.primaryAccent
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

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    useFigmaStyle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    val darkerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val border = if (useFigmaStyle) 
        BorderStroke(
            1.dp,
            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                AppTheme.tokens.cardBorderMuted
            } else {
                AppTheme.tokens.cardBorderStandard
            }
        )
    else 
        BorderStroke(
            1.dp,
            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                AppTheme.tokens.cardBorderMuted
            } else {
                AppTheme.tokens.cardBorderStandard
            }
        )
    
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

private fun formatCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}k"
        else -> count.toString()
    }
}
