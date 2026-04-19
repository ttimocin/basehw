package com.taytek.basehw.ui.screens.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import java.util.concurrent.TimeUnit
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.CommunityPost
import com.taytek.basehw.ui.components.RankBadgeChip
import com.taytek.basehw.ui.theme.AppTheme
import com.taytek.basehw.ui.theme.DarkNavy
import java.text.SimpleDateFormat
import java.util.*
import com.taytek.basehw.ui.util.AvatarUtil

// Desteklenen emoji reaksiyonlar
private val REACTION_EMOJIS = listOf("👍", "❤️", "🔥", "😮", "😢", "😡")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: CommunityPost,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    currentUser: com.taytek.basehw.domain.model.User? = null,
    authorAvatars: Map<String, Pair<Int, String?>> = emptyMap(),
    onReactionSelect: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateText = getRelativeTimeText(post.createdAt)

    val isSth = post.carFeature?.lowercase() == "sth"
    val stsColor = Color(0xFFFF8C00)

    val isMyPost = post.authorUid == currentUser?.uid
    val isAdmin = currentUser?.isAdmin == true
    val isMod = currentUser?.isMod == true
    val canDelete = isMyPost || isAdmin || isMod

    var showEmojiPicker by remember { mutableStateOf(false) }

    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    val darkerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(
                if (isSth) Modifier.border(2.dp, stsColor, RoundedCornerShape(20.dp))
                else Modifier.border(
                    width = 1.dp,
                    color = AppTheme.tokens.cardBorderStandard,
                    shape = RoundedCornerShape(20.dp)
                )
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSth) 4.dp else 2.dp)
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
            Column {
            // ── Header ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUserClick(post.authorUid) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (avatarId, avatarUrl) = authorAvatars[post.authorUid]
                    ?: Pair(post.authorSelectedAvatarId, post.authorCustomAvatarUrl)

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppTheme.tokens.primaryAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarId == 0 && !avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else if (avatarId > 0) {
                        val avatarResId = AvatarUtil.getAvatarResource(avatarId)
                        Image(
                            painter = painterResource(id = avatarResId),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = post.authorUsername.take(1).uppercase(),
                            color = AppTheme.tokens.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorUsername,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (post.authorIsAdmin) {
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.admin_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else if (post.authorIsMod) {
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.mod_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                    RankBadgeChip(badge = post.authorBadge, compact = true)
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Brand chip
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            post.carBrand,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                // Delete button
                if (canDelete && onDeleteClick != null) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Car Image Carousel ────────────────────────────
            val imageUrls = post.carImageUrls.ifEmpty {
                if (post.carImageUrl.isNotBlank()) listOf(post.carImageUrl) else emptyList()
            }

            if (imageUrls.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
                    
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = imageUrls[page],
                            contentDescription = post.carModelName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (imageUrls.size > 1) {
                        Row(
                            Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(imageUrls.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) AppTheme.tokens.primaryAccent else Color.White.copy(alpha = 0.5f)
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Caption & Info ─────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (post.carModelName.isNotBlank()) {
                    Text(
                        text = post.carModelName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!post.carSeries.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${post.carSeries}${post.carYear?.let { " • $it" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (post.carFeature?.lowercase() == "sth") {
                            Text("STH", color = stsColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                        } else if (!post.carFeature.isNullOrBlank()) {
                            Text(post.carFeature.uppercase(), color = AppTheme.tokens.primaryAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (post.caption.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Reactions Bar ──────────────────────────────
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Emoji picker
            AnimatedVisibility(
                visible = showEmojiPicker,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                EmojiPickerBar(
                    selectedEmoji = post.myReaction,
                    onEmojiSelected = { emoji ->
                        showEmojiPicker = false
                        onReactionSelect?.invoke(emoji)
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reaction button
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = {
                                    if (onReactionSelect != null) {
                                        if (post.myReaction != null) {
                                            onReactionSelect(post.myReaction)
                                        } else {
                                            onReactionSelect("👍")
                                        }
                                    } else {
                                        onLikeClick()
                                    }
                                },
                                onLongClick = {
                                    if (onReactionSelect != null) {
                                        showEmojiPicker = !showEmojiPicker
                                    }
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val reactionColor = if (post.myReaction != null) AppTheme.tokens.primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        if (post.myReaction != null) {
                            Text(text = post.myReaction, fontSize = 18.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = reactionColor
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (post.totalReactionCount > 0) "${post.totalReactionCount}" else stringResource(R.string.like),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = reactionColor
                        )
                    }
                }

                // Comment
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = onCommentClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (post.commentCount > 0) "${post.commentCount}" else stringResource(R.string.comment),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (onReportClick != null && !isMyPost) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = onReportClick,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = stringResource(R.string.report_post_content_desc),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.report_post),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (post.hasReactions) {
                ReactionSummaryBar(
                    reactionCounts = post.reactionCounts,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
    }
}
}

@Composable
private fun EmojiPickerBar(
    selectedEmoji: String?,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            REACTION_EMOJIS.forEach { emoji ->
                val isSelected = emoji == selectedEmoji
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(if (isSelected) Modifier.background(AppTheme.tokens.primaryAccent.copy(alpha = 0.15f)) else Modifier)
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = if (isSelected) 24.sp else 20.sp)
                }
            }
        }
    }
}

@Composable
private fun ReactionSummaryBar(
    reactionCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val nonZeroReactions = reactionCounts.filter { it.value > 0 }
    if (nonZeroReactions.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        nonZeroReactions.forEach { (emoji, count) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = emoji, fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getRelativeTimeText(createdAtMillis: Long): String {
    if (createdAtMillis <= 0) return ""

    val now = System.currentTimeMillis()
    val diff = now - createdAtMillis

    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        seconds < 60 -> stringResource(R.string.time_just_now)
        minutes < 60 -> stringResource(R.string.time_minutes_ago, minutes.toInt())
        hours < 24 -> stringResource(R.string.time_hours_ago, hours.toInt())
        days < 7 -> stringResource(R.string.time_days_ago, days.toInt())
        else -> {
            val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
            dateFormat.format(Date(createdAtMillis))
        }
    }
}