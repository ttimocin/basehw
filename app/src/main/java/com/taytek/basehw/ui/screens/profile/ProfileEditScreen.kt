package com.taytek.basehw.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.taytek.basehw.R
import com.taytek.basehw.ui.theme.AppTheme
import com.taytek.basehw.ui.util.AvatarUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onNavigateBack: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val userData = uiState.userData
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    var usernameInput by rememberSaveable(userData?.username) { mutableStateOf(userData?.username.orEmpty()) }
    var showAvatarDialog by rememberSaveable { mutableStateOf(false) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { profileViewModel.uploadCustomAvatar(it) }
    }

    LaunchedEffect(uiState.error, uiState.syncStatusMsg) {
        val message = when {
            uiState.error == "EMAIL_EMPTY_ERROR" -> context.getString(R.string.email_empty_error)
            uiState.syncStatusMsg == "PASSWORD_RESET_SENT" -> context.getString(R.string.password_reset_sent)
            uiState.syncStatusMsg == "USERNAME_TAKEN" -> context.getString(R.string.username_taken)
            !uiState.error.isNullOrBlank() -> uiState.error
            else -> null
        }
        message?.let {
            snackbarHostState.showSnackbar(it)
            profileViewModel.clearMessages()
        }
    }

    val isEmailPasswordUser = remember(firebaseUser) {
        val providers = firebaseUser?.providerData?.map { it.providerId }.orEmpty()
        val isGoogleOnly = providers.contains("google.com") && !providers.contains("password")
        !isGoogleOnly && firebaseUser?.isAnonymous == false && !firebaseUser.email.isNullOrBlank()
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.inverseSurface,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.edit_profile), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background)
            )
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && userData == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = stringResource(R.string.profile_edit_avatar_title), fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(AppTheme.tokens.primaryAccent.copy(alpha = 0.15f))
                                        .border(1.dp, AppTheme.tokens.primaryAccent.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val selectedAvatarId = userData?.selectedAvatarId ?: 1
                                    val customAvatarUrl = userData?.customAvatarUrl
                                    if (!customAvatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = customAvatarUrl,
                                            contentDescription = stringResource(R.string.avatar_desc),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        val avatarResId = AvatarUtil.getAvatarResource(selectedAvatarId)
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(id = avatarResId),
                                            contentDescription = stringResource(R.string.avatar_desc),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }

                                Button(onClick = { showAvatarDialog = true }) {
                                    Text(text = stringResource(R.string.edit_avatar_action))
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Email, contentDescription = null)
                                Text(text = stringResource(R.string.profile_edit_email_title), fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedTextField(
                                value = userData?.email.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Person, contentDescription = null)
                                Text(text = stringResource(R.string.profile_edit_username_title), fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = {
                                    val sanitized = it.take(8)
                                    usernameInput = sanitized
                                    profileViewModel.checkUsernameAvailability(sanitized)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                                placeholder = { Text(stringResource(R.string.username_hint)) }
                            )
                            Button(
                                onClick = { profileViewModel.saveEditedUsername(usernameInput.trim()) },
                                enabled = usernameInput.trim().isNotBlank() && usernameInput.trim() != (userData?.username ?: "")
                            ) {
                                Text(text = stringResource(R.string.save_btn))
                            }
                        }
                    }

                    if (isEmailPasswordUser) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.LockReset, contentDescription = null)
                                    Text(text = stringResource(R.string.profile_edit_password_title), fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = stringResource(R.string.profile_edit_password_help),
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { profileViewModel.resetPassword(userData?.email.orEmpty()) },
                                    enabled = !userData?.email.isNullOrBlank()
                                ) {
                                    Text(text = stringResource(R.string.forgot_password))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAvatarDialog) {
        AvatarSelectionDialog(
            onDismiss = { showAvatarDialog = false },
            onSelectDefault = { avatarId ->
                profileViewModel.selectDefaultAvatar(avatarId)
                showAvatarDialog = false
            },
            onSelectFromGallery = {
                showAvatarDialog = false
                avatarPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }
}
