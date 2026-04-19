package com.taytek.basehw.ui.screens.profile

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.taytek.basehw.R
import com.taytek.basehw.ui.theme.AppTheme
import com.taytek.basehw.ui.theme.CyberKnockoutIconTint
import com.taytek.basehw.ui.theme.LocalThemeVariant
import com.taytek.basehw.ui.theme.NeonCyanKnockoutIconTint
import com.taytek.basehw.ui.theme.ThemeVariant
import com.taytek.basehw.ui.theme.cyberRootBackgroundColor
import com.taytek.basehw.ui.theme.isDarkThemeUi
import com.taytek.basehw.ui.theme.neonCyanActionGradientBrush
import kotlinx.coroutines.launch
import android.content.Context
import android.content.ContextWrapper

private fun findActivity(context: Context): Activity? {
    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

private data class NonUserProfileColors(
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val inactiveText: Color,
    val cardBackground: Color,
    val outlinedButtonBackground: Color,
    val outlinedButtonBorder: Color,
    val outlinedButtonText: Color,
    val featureIcon: Color
)

@Composable
private fun nonUserProfileColors(): NonUserProfileColors {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return NonUserProfileColors(
        background = scheme.background,
        primaryText = scheme.onBackground,
        secondaryText = scheme.onSurfaceVariant,
        inactiveText = scheme.onSurfaceVariant.copy(alpha = if (isDark) 0.8f else 0.9f),
        cardBackground = if (isDark) scheme.surfaceVariant.copy(alpha = 0.35f) else Color.Transparent,
        outlinedButtonBackground = scheme.surface,
        outlinedButtonBorder = scheme.outlineVariant.copy(alpha = if (isDark) 0.55f else 0.8f),
        outlinedButtonText = scheme.onSurface,
        featureIcon = if (isDark) AppTheme.tokens.primaryAccent else scheme.primary
    )
}

@Composable
private fun getNonUserCardGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NonUserProfileScreen(
    onSettingsClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsOfUseClick: () -> Unit = {},
    onForumRulesClick: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val themeState by viewModel.themeFlow.collectAsState(initial = 0)
    val fontFamilyState by viewModel.fontFlow.collectAsState(initial = 0)
    val languageState by viewModel.languageFlow.collectAsState(initial = "")
    val currencyCode by viewModel.currencyCode.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Local UI flags
    var googleUsernameInput by rememberSaveable { mutableStateOf("") }
    var showSettingsPage by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val webClientId = remember(context) {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            context.getString(resId)
        } else {
            ""
        }
    }

    fun handleCredentialResult(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            viewModel.signInWithGoogle(credential.idToken)
        } else if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
        }
    }

    val handleSignIn = {
        if (webClientId.isBlank()) {
            viewModel.setErrorMessage(context.getString(R.string.profile_web_client_error))
        } else {
            coroutineScope.launch {
                try {
                    val activity = findActivity(context) ?: run {
                        viewModel.setErrorMessage(context.getString(R.string.profile_activity_not_found))
                        return@launch
                    }
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(request = request, context = activity)
                    handleCredentialResult(result)
                } catch (e: NoCredentialException) {
                    viewModel.setErrorMessage(context.getString(R.string.profile_login_error))
                } catch (e: GetCredentialException) {
                    val rawMessage = e.message.orEmpty()
                    val normalized = rawMessage.lowercase()
                    // User explicitly cancels account picker: do not surface noisy error.
                    if (normalized.contains("cancel") || normalized.contains("selector")) {
                        viewModel.setErrorMessage(null)
                    } else {
                        viewModel.setErrorMessage(
                            context.getString(R.string.profile_login_failed_template, rawMessage)
                        )
                    }
                }
            }
        }
    }

    if (showSettingsPage) {
        SettingsScreen(
            uiState = com.taytek.basehw.ui.screens.community.CommunityUiState(),
            profileUiState = uiState,
            profileViewModel = viewModel,
            themeState = themeState,
            fontFamilyState = fontFamilyState,
            languageState = languageState,
            currencyCode = currencyCode,
            onBack = { showSettingsPage = false },
            onUpdateCollectionVisibility = viewModel::updateCollectionVisibility,
            onUpdateWishlistVisibility = viewModel::updateWishlistVisibility,
            onBackup = viewModel::backupToCloud,
            onRestore = viewModel::restoreFromCloud,
            onExport = viewModel::onExportClick,
            onSetTheme = viewModel::setTheme,
            onSetFontFamily = viewModel::setFontFamily,
            onSetLanguage = viewModel::setLanguage,
            onSetCurrency = viewModel::setCurrency,
            onOpenPrivacyPolicy = onPrivacyPolicyClick,
            onOpenTermsOfUse = onTermsOfUseClick,
            onOpenForumRules = onForumRulesClick,
            onOpenSupport = onSettingsClick,
            onLogout = { viewModel.signOut() },
            onDeleteAccount = { viewModel.deleteAccount() },
            onAdminPanelClick = {},
            isAuthenticated = currentUser != null && currentUser?.isAnonymous == false
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = cyberRootBackgroundColor(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Preempt insets to allow manual padding
    ) { padding ->
        FigmaNonUserProfileContent(
            padding = padding,
            uiState = uiState,
            onHandleGoogleSignIn = { handleSignIn() },
            onShowAuthSelection = { viewModel.setShowAuthOptionSelection(true) },
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onTermsOfUseClick = onTermsOfUseClick,
            onSettingsClick = { showSettingsPage = true }
        )
    }

    // --- DIALOGS ---
    if (uiState.showAuthOptionSelection || uiState.showEmailAuthFields) {
        AuthenticationDialog(
            uiState = uiState,
            showAuthOptionSelection = uiState.showAuthOptionSelection,
            isRegisterMode = uiState.isRegisterMode,
            email = uiState.email,
            username = uiState.username,
            password = uiState.password,
            isPasswordVisible = uiState.isPasswordVisible,
            onDismiss = { viewModel.setShowAuthOptionSelection(false); viewModel.setShowEmailAuthFields(false) },
            onEmailChange = { viewModel.updateEmail(it) },
            onUsernameChange = { viewModel.updateAuthUsername(it) },
            onPasswordChange = { viewModel.updatePassword(it) },
            onTogglePasswordVisibility = { viewModel.togglePasswordVisibility() },
            onToggleRegisterMode = { viewModel.toggleRegisterMode() },
            onEmailAuthSubmit = {
                if (uiState.isRegisterMode) viewModel.signUpWithEmail(uiState.email, uiState.password, uiState.username)
                else viewModel.signInWithEmail(uiState.email, uiState.password)
            },
            onResetPassword = { viewModel.resetPassword(if (uiState.email.isNotBlank()) uiState.email else "") },
            onGoToEmailAuth = { mode ->
                viewModel.setRegisterMode(mode)
                viewModel.setShowAuthOptionSelection(false)
                viewModel.setShowEmailAuthFields(true)
            },
            onGoBackToSelection = { viewModel.setShowEmailAuthFields(false); viewModel.setShowAuthOptionSelection(true) },
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onTermsOfUseClick = onTermsOfUseClick,
            onToggleRememberMe = { viewModel.toggleRememberMe() }
        )
    }

    if (currentUser != null && uiState.showMandatoryConsentDialog) {
        MandatoryConsentDialog(
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onTermsOfUseClick = onTermsOfUseClick,
            onAccept = { viewModel.acceptMandatoryConsent() },
            onDecline = { viewModel.declineMandatoryConsent() }
        )
    }

    if (uiState.showUsernamePrompt) {
        UsernamePromptDialog(
            googleUsernameInput = googleUsernameInput,
            uiState = uiState,
            onUsernameChange = {
                val sanitized = it.take(8)
                googleUsernameInput = sanitized
                viewModel.checkUsernameAvailability(sanitized)
            },
            onSave = { viewModel.assignUsername(googleUsernameInput) },
            onSkip = { viewModel.skipUsernameSelection() }
        )
    }
}



// Ana içerik - Figma tasarımı (responsive, weight-based layout)
@Composable
private fun FigmaNonUserProfileContent(
    padding: PaddingValues,
    uiState: ProfileUiState,
    onHandleGoogleSignIn: () -> Unit,
    onShowAuthSelection: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    // In-app bottom navigation bar height
    val bottomNavHeight = 72.dp
    val colors = nonUserProfileColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = bottomNavHeight)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top: Settings icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 1.dp), // Daha sağa yaklaştır
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(36.dp) // Butonun dokunma alanını büyüt
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = colors.primaryText,
                    modifier = Modifier.size(24.dp) // İkonu biraz küçült
                )
            }
        }

        // Flexible space before logo (reduced)
        Spacer(modifier = Modifier.weight(0.20f)) // was 0.35f, moved logo and below up

        // Logo
        AsyncImage(
            model = R.drawable.nonuser,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.38f).aspectRatio(1f)
        )

        Spacer(modifier = Modifier.weight(0.35f)) // was 0.5f, moved up

        // Title & Description
        Text(
            text = stringResource(R.string.nonuser_login_cloud_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.primaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.nonuser_login_cloud_desc),
            fontSize = 13.sp,
            color = colors.secondaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(0.6f))

        // Buttons
        ThemePrimaryActionButton(
            onClick = onHandleGoogleSignIn,
            text = stringResource(R.string.nonuser_signin_google),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        ThemeSecondaryOutlinedActionButton(
            onClick = onShowAuthSelection,
            text = stringResource(R.string.nonuser_login_signup),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )

        Spacer(modifier = Modifier.weight(0.6f))

        // Feature cards (compact inline row)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactFeatureCard(
                icon = Icons.Filled.CloudUpload,
                label = stringResource(R.string.nonuser_feature_backup),
                modifier = Modifier.weight(1f).height(92.dp),
                backgroundColor = colors.cardBackground,
                iconColor = colors.featureIcon,
                textColor = colors.primaryText
            )
            CompactFeatureCard(
                icon = Icons.Filled.People,
                label = stringResource(R.string.nonuser_feature_community),
                modifier = Modifier.weight(1f).height(92.dp),
                backgroundColor = colors.cardBackground,
                iconColor = colors.featureIcon,
                textColor = colors.primaryText
            )
            CompactFeatureCard(
                icon = Icons.Filled.CloudDownload,
                label = stringResource(R.string.nonuser_feature_restore),
                modifier = Modifier.weight(1f).height(92.dp),
                backgroundColor = colors.cardBackground,
                iconColor = colors.featureIcon,
                textColor = colors.primaryText
            )
        }

        Spacer(modifier = Modifier.weight(0.4f))

        // Bottom links
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrivacyPolicyClick, contentPadding = PaddingValues(4.dp)) {
                Text(stringResource(R.string.privacy_policy), fontSize = 10.sp, color = colors.inactiveText)
            }
            Text(" • ", fontSize = 10.sp, color = colors.inactiveText)
            TextButton(onClick = onTermsOfUseClick, contentPadding = PaddingValues(4.dp)) {
                Text(stringResource(R.string.terms_of_use), fontSize = 10.sp, color = colors.inactiveText)
            }
        }

        // Error messages
        ErrorMessages(uiState)
    }
}

// Compact feature card for horizontal row
@Composable
private fun CompactFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    iconColor: Color,
    textColor: Color
) {
    val isDark = isDarkThemeUi()
    val cardGradient = getNonUserCardGradient()
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isDark) Modifier.background(backgroundColor)
                else Modifier.background(cardGradient)
            )
            .border(
                border = BorderStroke(1.5.dp, if (isDark) AppTheme.tokens.primaryAccent.copy(alpha = 0.2f) else AppTheme.tokens.primaryAccent.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = iconColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}



@Composable
private fun ThemePrimaryActionButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val shell = LocalThemeVariant.current
    val shape = RoundedCornerShape(12.dp)
    val (backgroundBrush, textColor) = when (shell) {
        ThemeVariant.Cyber -> Brush.linearGradient(
            colors = listOf(AppTheme.tokens.selectionIconTint, AppTheme.tokens.primaryAccent)
        ) to CyberKnockoutIconTint
        ThemeVariant.NeonCyan -> neonCyanActionGradientBrush() to NeonCyanKnockoutIconTint
        else -> Brush.linearGradient(
            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
        ) to MaterialTheme.colorScheme.onPrimary
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundBrush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ThemeSecondaryOutlinedActionButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val shell = LocalThemeVariant.current
    val isNeon = shell == ThemeVariant.Cyber || shell == ThemeVariant.NeonCyan
    val accent = if (isNeon) AppTheme.tokens.selectionIconTint else MaterialTheme.colorScheme.primary
    val innerBg = if (isNeon) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(innerBg)
            .border(
                width = 1.dp,
                color = accent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            textAlign = TextAlign.Center
        )
    }
}



// Dialog components
@Composable
fun AuthenticationDialog(
    uiState: ProfileUiState,
    showAuthOptionSelection: Boolean,
    isRegisterMode: Boolean,
    email: String,
    username: String,
    password: String,
    isPasswordVisible: Boolean,
    onDismiss: () -> Unit,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleRegisterMode: () -> Unit,
    onEmailAuthSubmit: () -> Unit,
    onResetPassword: () -> Unit,
    onGoToEmailAuth: (Boolean) -> Unit,
    onGoBackToSelection: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit,
    onToggleRememberMe: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showAuthOptionSelection) {
                    Text(text = stringResource(R.string.login_signup_btn), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeSecondaryOutlinedActionButton(
                            onClick = { onGoToEmailAuth(false) },
                            text = stringResource(R.string.login_btn),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )
                        ThemePrimaryActionButton(
                            onClick = { onGoToEmailAuth(true) },
                            text = stringResource(R.string.register_btn),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    ConsentLinksOnly(onPrivacyPolicyClick = onPrivacyPolicyClick, onTermsOfUseClick = onTermsOfUseClick)
                    ErrorMessages(uiState)
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onGoBackToSelection) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.profile_back)) }
                        Text(text = if (isRegisterMode) stringResource(R.string.register_btn) else stringResource(R.string.login_btn), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(value = email, onValueChange = onEmailChange, label = { Text(stringResource(R.string.email_label)) }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Email, null) }, shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    if (isRegisterMode) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = username, onValueChange = { onUsernameChange(it.take(8)) }, label = { Text(stringResource(R.string.username_label)) }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.AlternateEmail, null) }, isError = uiState.isUsernameAvailable == false, supportingText = { Text(if (uiState.isUsernameAvailable == false) stringResource(R.string.username_taken) else stringResource(R.string.username_hint)) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    val isPassValid = com.taytek.basehw.domain.util.AuthValidator.isPasswordValid(password)
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(R.string.password_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = onTogglePasswordVisibility) { Icon(if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = isRegisterMode && password.isNotEmpty() && !isPassValid,
                        supportingText = {
                            if (isRegisterMode && password.isNotEmpty() && !isPassValid) {
                                Text(stringResource(R.string.password_criteria_error), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggleRememberMe() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = uiState.rememberMe, onCheckedChange = { onToggleRememberMe() })
                        Text(text = stringResource(R.string.remember_me), style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(16.dp))
                    val isSubmitEnabled = if (isRegisterMode) {
                        !uiState.isLoading && email.isNotBlank() && isPassValid && username.length in 3..8 && uiState.isUsernameAvailable == true
                    } else {
                        !uiState.isLoading && email.isNotBlank() && password.length >= 6
                    }
                    Button(
                        onClick = onEmailAuthSubmit,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = isSubmitEnabled
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        else Text(if (isRegisterMode) stringResource(R.string.register_btn) else stringResource(R.string.login_btn))
                    }
                    TextButton(onClick = onToggleRegisterMode, modifier = Modifier.fillMaxWidth()) {
                        Text(text = if (isRegisterMode) stringResource(R.string.have_account) else stringResource(R.string.no_account), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    if (!isRegisterMode) {
                        TextButton(onClick = onResetPassword, modifier = Modifier.fillMaxWidth()) {
                            Text(text = stringResource(R.string.forgot_password), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    ConsentLinksOnly(onPrivacyPolicyClick = onPrivacyPolicyClick, onTermsOfUseClick = onTermsOfUseClick)
                    ErrorMessages(uiState)
                }
            }
        }
    }
}

@Composable
fun UsernamePromptDialog(
    googleUsernameInput: String,
    uiState: ProfileUiState,
    onUsernameChange: (String) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.select_username_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.select_username_desc))
                OutlinedTextField(value = googleUsernameInput, onValueChange = onUsernameChange, label = { Text(stringResource(R.string.username_label)) }, isError = uiState.isUsernameAvailable == false, supportingText = { if (uiState.isUsernameAvailable == false) Text(stringResource(R.string.username_taken)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !uiState.isLoading && uiState.isUsernameAvailable == true && googleUsernameInput.length in 3..8) {
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text(stringResource(R.string.save_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip, enabled = !uiState.isLoading) { Text(stringResource(R.string.skip_btn)) }
        }
    )
}

@Composable
fun AuthConsentCheckbox(
    consentGranted: Boolean,
    onToggleConsent: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onToggleConsent() }, verticalAlignment = Alignment.Top) {
                Checkbox(checked = consentGranted, onCheckedChange = { onToggleConsent() }, modifier = Modifier.offset(y = (-4).dp))
                Text(text = stringResource(R.string.consent_text_with_links), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(4.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onPrivacyPolicyClick, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(stringResource(R.string.privacy_policy), style = MaterialTheme.typography.labelSmall)
                    }
                    Text("•", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = onTermsOfUseClick, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(stringResource(R.string.terms_of_use), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsentLinksOnly(
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = onPrivacyPolicyClick, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) {
            Text(stringResource(R.string.privacy_policy), style = MaterialTheme.typography.labelSmall)
        }
        Text("•", style = MaterialTheme.typography.labelSmall)
        TextButton(onClick = onTermsOfUseClick, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) {
            Text(stringResource(R.string.terms_of_use), style = MaterialTheme.typography.labelSmall)
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
