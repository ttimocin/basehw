package com.taytek.basehw.ui.screens.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taytek.basehw.R
import com.taytek.basehw.ui.util.AvatarUtil
import com.taytek.basehw.ui.theme.AppTheme

@Composable
fun ErrorMessages(uiState: ProfileUiState) {
    uiState.error?.let { msg ->
        Text(
            text = msg,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun AvatarSelectionDialog(
    onDismiss: () -> Unit,
    onSelectDefault: (Int) -> Unit,
    onSelectFromGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.avatar_pick_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.avatar_default_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                // Using Column + two Rows to display 8 avatars neatly
                val avatarCount = AvatarUtil.getAvatarCount()
                val chunkedAvatars = (1..avatarCount).toList().chunked(4)
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedAvatars.forEach { rowIds ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowIds.forEach { i ->
                                val avatarResId = AvatarUtil.getAvatarResource(i)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppTheme.tokens.primaryAccent.copy(alpha = 0.15f))
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
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
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
