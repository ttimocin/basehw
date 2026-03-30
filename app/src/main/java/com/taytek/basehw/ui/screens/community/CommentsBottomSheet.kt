package com.taytek.basehw.ui.screens.community

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.taytek.basehw.R
import com.taytek.basehw.domain.model.CommunityComment
import com.taytek.basehw.ui.theme.AppPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    comments: List<CommunityComment>,
    isLoading: Boolean,
    currentUserUid: String?,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit,
    currentUser: com.taytek.basehw.domain.model.User? = null
) {
    var commentText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    var commentToDelete by remember { mutableStateOf<CommunityComment?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        sheetMaxWidth = Dp.Unspecified
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Title
            Text(
                text = "${stringResource(R.string.comment)} (${comments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider()

            // Comments list area - uses fill = false so it wraps when empty
            Box(modifier = Modifier.weight(1f, fill = false)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppPrimary)
                    }
                } else if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_comments_yet),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(comments) { comment ->
                            val isCommentOwner = comment.authorUid == currentUser?.uid
                            val isAdmin = currentUser?.isAdmin == true
                            
                            CommentItem(
                                comment = comment,
                                dateFormat = dateFormat,
                                canDelete = isCommentOwner || isAdmin,
                                onUserClick = onUserClick,
                                onLongClick = { commentToDelete = comment }
                            )
                        }
                    }
                }
            }

            // Input
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text(stringResource(R.string.write_comment)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppPrimary,
                        cursorColor = AppPrimary
                    )
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText.trim())
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AppPrimary
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = stringResource(R.string.send))
                }
            }
        }
    }

    // Confirmation Dialog
    if (commentToDelete != null) {
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text(stringResource(R.string.delete_comment_confirm)) },
            text = { Text(stringResource(R.string.delete_comment_confirmation)) },
            confirmButton = {
                TextButton(onClick = { onDeleteComment(commentToDelete!!.id); commentToDelete = null }) {
                    Text(stringResource(R.string.delete_comment_confirm), color = AppPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { commentToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentItem(
    comment: CommunityComment,
    dateFormat: SimpleDateFormat,
    canDelete: Boolean,
    onUserClick: (String) -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = if (canDelete) onLongClick else null
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppPrimary.copy(alpha = 0.1f))
                .clickable { onUserClick(comment.authorUid) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.authorUsername.take(1).uppercase(),
                color = AppPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorUsername,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onUserClick(comment.authorUid) }
                )
                if (comment.authorIsAdmin) {
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "🛡️ ADMIN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 7.sp,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (comment.createdAt > 0) dateFormat.format(Date(comment.createdAt)) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
