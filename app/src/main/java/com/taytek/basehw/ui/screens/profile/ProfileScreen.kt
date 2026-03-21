package com.taytek.basehw.ui.screens.profile

import androidx.lifecycle.asFlow

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.taytek.basehw.ui.theme.AppPrimary
import com.taytek.basehw.ui.theme.DarkNavy
import kotlinx.coroutines.launch

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onStatisticsClick: () -> Unit,
    onSupportClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val themeState by viewModel.themeFlow.collectAsState(initial = 0)
    val languageState by viewModel.languageFlow.collectAsState(initial = "")
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val workManager = remember { androidx.work.WorkManager.getInstance(context) }
    
    // Local UI flags that don't need persistence in VM
    var googleUsernameInput by rememberSaveable { mutableStateOf("") }
    
    val statisticsViewModel: com.taytek.basehw.ui.screens.statistics.StatisticsViewModel = hiltViewModel()
    val totalCars by viewModel.totalCars.collectAsState()
    val totalEstimatedValue by viewModel.totalValue.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val boxedCount by viewModel.totalBoxed.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    
    // ... rest of the setup
    val webClientId = try { 
        context.getString(context.resources.getIdentifier("default_web_client_id", "string", context.packageName)) 
    } catch(e: Exception) { "" }

    val handleSignIn = {
        // ... (existing handleSignIn code)
        if (webClientId.isBlank()) {
            Log.e("Auth", "Web Client ID bulunamadı! Lütfen google-services.json eklendiğinden emin olun.")
        } else {
            coroutineScope.launch {
                val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request: GetCredentialRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                try {
                    if (!uiState.consentGranted) {
                        viewModel.setErrorMessage(context.getString(com.taytek.basehw.R.string.accept_terms_error))
                        return@launch
                    }
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context as Activity
                    )
                    
                    val credential = result.credential
                    if (credential is GoogleIdTokenCredential) {
                        viewModel.signInWithGoogle(credential.idToken)
                    } else if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                    }

                } catch (e: GetCredentialException) {
                    Log.e("Auth", "Giriş penceresi iptal edildi veya hata: ${e.message}")
                }
            }
        }
    }

    val exportSuccessMsg = stringResource(com.taytek.basehw.R.string.export_success)

    LaunchedEffect(uiState.syncStatusMsg) {
        if (uiState.syncStatusMsg == "EXPORT_SUCCESS") {
            snackbarHostState.showSnackbar(exportSuccessMsg)
            viewModel.clearMessages()
        }
    }

    val excelExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.let { outputStream ->
                viewModel.exportToExcel(outputStream)
            }
        }
    }



    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ProfileScreenContent(
            padding = padding,
            scrollState = scrollState,
            currentUser = currentUser,
            uiState = uiState,
            totalCars = totalCars,
            totalValue = totalEstimatedValue,
            currencySymbol = currencySymbol,
            currencyCode = currencyCode,
            boxedCount = boxedCount,
            themeState = themeState,
            languageState = languageState,
            viewModel = viewModel,
            workManager = workManager,
            onStatisticsClick = onStatisticsClick,
            onSupportClick = onSupportClick,
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onTermsOfUseClick = onTermsOfUseClick,
            excelExportLauncher = excelExportLauncher,
            onShowAuthSelection = { viewModel.setShowAuthOptionSelection(true) }
        )
    }

    // --- DIALOGS ---
    if ((uiState.showAuthOptionSelection || uiState.showEmailAuthFields) && currentUser == null) {
        AuthenticationDialog(
            uiState = uiState,
            showAuthOptionSelection = uiState.showAuthOptionSelection,
            isRegisterMode = uiState.isRegisterMode,
            email = uiState.email,
            username = uiState.username,
            password = uiState.password,
            isPasswordVisible = uiState.isPasswordVisible,
            consentGranted = uiState.consentGranted,
            onDismiss = { viewModel.setShowAuthOptionSelection(false); viewModel.setShowEmailAuthFields(false) },
            onHandleGoogleSignIn = { handleSignIn() },
            onEmailChange = { viewModel.updateEmail(it) },
            onUsernameChange = { viewModel.updateAuthUsername(it) },
            onPasswordChange = { viewModel.updatePassword(it) },
            onTogglePasswordVisibility = { viewModel.togglePasswordVisibility() },
            onToggleRegisterMode = { viewModel.toggleRegisterMode() },
            onToggleConsent = { viewModel.toggleConsent() },
            onEmailAuthSubmit = { 
                if (uiState.isRegisterMode) viewModel.signUpWithEmail(uiState.email, uiState.password, uiState.username, uiState.consentGranted)
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

    if (uiState.showUsernamePrompt) {
        UsernamePromptDialog(
            googleUsernameInput = googleUsernameInput,
            uiState = uiState,
            onUsernameChange = { googleUsernameInput = it; viewModel.checkUsernameAvailability(it) },
            onSave = { viewModel.assignUsername(googleUsernameInput) },
            onSkip = { viewModel.skipUsernameSelection() }
        )
    }

    if (uiState.isEditingUsername) {
        EditUsernameDialog(
            userData = uiState.userData,
            isLoading = uiState.isLoading,
            isUsernameAvailable = uiState.isUsernameAvailable,
            onDismiss = { viewModel.setEditingUsername(false) },
            onSave = { viewModel.saveEditedUsername(it) },
            onCheckAvailability = { viewModel.checkUsernameAvailability(it) }
        )
    }
}

// --- DIALOG HELPERS ---

@Composable
fun AuthenticationDialog(
    uiState: ProfileUiState,
    showAuthOptionSelection: Boolean,
    isRegisterMode: Boolean,
    email: String,
    username: String,
    password: String,
    isPasswordVisible: Boolean,
    consentGranted: Boolean,
    onDismiss: () -> Unit,
    onHandleGoogleSignIn: () -> Unit,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleRegisterMode: () -> Unit,
    onToggleConsent: () -> Unit,
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
                containerColor = if (MaterialTheme.colorScheme.background == DarkNavy)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showAuthOptionSelection) {
                    Text(text = stringResource(com.taytek.basehw.R.string.login_signup_btn), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onHandleGoogleSignIn, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                        if (uiState.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        else Text(stringResource(com.taytek.basehw.R.string.login_google), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { onGoToEmailAuth(false) }, 
                            modifier = Modifier.fillMaxWidth().height(52.dp), 
                            shape = RoundedCornerShape(14.dp)
                        ) { 
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.login_btn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            ) 
                        }
                        Button(
                            onClick = { onGoToEmailAuth(true) }, 
                            modifier = Modifier.fillMaxWidth().height(52.dp), 
                            shape = RoundedCornerShape(14.dp), 
                            enabled = uiState.consentGranted
                        ) { 
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.register_btn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            ) 
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    AuthConsentCheckbox(
                        consentGranted = consentGranted,
                        onToggleConsent = onToggleConsent,
                        onPrivacyPolicyClick = onPrivacyPolicyClick,
                        onTermsOfUseClick = onTermsOfUseClick
                    )
                    ErrorMessages(uiState)
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onGoBackToSelection) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                        Text(text = if (isRegisterMode) stringResource(com.taytek.basehw.R.string.register_btn) else stringResource(com.taytek.basehw.R.string.login_btn), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = email, 
                        onValueChange = onEmailChange, 
                        label = { Text(stringResource(com.taytek.basehw.R.string.email_label)) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        leadingIcon = { Icon(Icons.Filled.Email, null) }, 
                        shape = RoundedCornerShape(12.dp), 
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (isRegisterMode) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = username, onValueChange = onUsernameChange, label = { Text(stringResource(com.taytek.basehw.R.string.username_label)) }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.AlternateEmail, null) }, isError = uiState.isUsernameAvailable == false, supportingText = { Text(if (uiState.isUsernameAvailable == false) stringResource(com.taytek.basehw.R.string.username_taken) else stringResource(com.taytek.basehw.R.string.username_hint)) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    val isPassValid = com.taytek.basehw.domain.util.AuthValidator.isPasswordValid(password)
                    OutlinedTextField(
                        value = password, 
                        onValueChange = onPasswordChange, 
                        label = { Text(stringResource(com.taytek.basehw.R.string.password_label)) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        leadingIcon = { Icon(Icons.Filled.Lock, null) }, 
                        visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(), 
                        trailingIcon = { IconButton(onClick = onTogglePasswordVisibility) { Icon(if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } }, 
                        shape = RoundedCornerShape(12.dp), 
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = isRegisterMode && password.isNotEmpty() && !isPassValid,
                        supportingText = {
                            if (isRegisterMode && password.isNotEmpty() && !isPassValid) {
                                Text(stringResource(com.taytek.basehw.R.string.password_criteria_error), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggleRememberMe() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = uiState.rememberMe, onCheckedChange = { onToggleRememberMe() })
                        Text(text = stringResource(com.taytek.basehw.R.string.remember_me), style = MaterialTheme.typography.bodyMedium)
                    }

                    if (isRegisterMode) {
                        Spacer(Modifier.height(16.dp))
                        AuthConsentCheckbox(
                            consentGranted = consentGranted,
                            onToggleConsent = onToggleConsent,
                            onPrivacyPolicyClick = onPrivacyPolicyClick,
                            onTermsOfUseClick = onTermsOfUseClick
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    val isSubmitEnabled = if (isRegisterMode) {
                        !uiState.isLoading && email.isNotBlank() && isPassValid && username.length >= 3 && uiState.isUsernameAvailable == true && consentGranted
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
                        else Text(if (isRegisterMode) stringResource(com.taytek.basehw.R.string.register_btn) else stringResource(com.taytek.basehw.R.string.login_btn))
                    }
                    TextButton(
                        onClick = onToggleRegisterMode,
                        modifier = Modifier.fillMaxWidth()
                    ) { 
                        Text(
                            text = if (isRegisterMode) stringResource(com.taytek.basehw.R.string.have_account) else stringResource(com.taytek.basehw.R.string.no_account),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ) 
                    }
                    if (!isRegisterMode) { 
                        TextButton(
                            onClick = onResetPassword,
                            modifier = Modifier.fillMaxWidth()
                        ) { 
                            Text(
                                text = stringResource(com.taytek.basehw.R.string.forgot_password),
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        } 
                    }
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
        title = { Text(stringResource(com.taytek.basehw.R.string.select_username_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(com.taytek.basehw.R.string.select_username_desc))
                OutlinedTextField(value = googleUsernameInput, onValueChange = onUsernameChange, label = { Text(stringResource(com.taytek.basehw.R.string.username_label)) }, isError = uiState.isUsernameAvailable == false, supportingText = { if (uiState.isUsernameAvailable == false) Text(stringResource(com.taytek.basehw.R.string.username_taken)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !uiState.isLoading && uiState.isUsernameAvailable == true && googleUsernameInput.length >= 3) {
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text(stringResource(com.taytek.basehw.R.string.save_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip, enabled = !uiState.isLoading) { Text(stringResource(com.taytek.basehw.R.string.skip_btn)) }
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
                Text(text = stringResource(com.taytek.basehw.R.string.consent_text_with_links), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(4.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onPrivacyPolicyClick, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) { 
                        Text(stringResource(com.taytek.basehw.R.string.privacy_policy), style = MaterialTheme.typography.labelSmall) 
                    }
                    Text("•", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = onTermsOfUseClick, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) { 
                        Text(stringResource(com.taytek.basehw.R.string.terms_of_use), style = MaterialTheme.typography.labelSmall) 
                    }
                }
            }
        }
    }
}

@Composable
fun EditUsernameDialog(
    userData: com.taytek.basehw.domain.model.User?,
    isLoading: Boolean,
    isUsernameAvailable: Boolean?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onCheckAvailability: (String) -> Unit
) {
    var editingUsernameInput by remember { mutableStateOf(userData?.username ?: "") }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(com.taytek.basehw.R.string.edit_username_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = editingUsernameInput, onValueChange = { editingUsernameInput = it; onCheckAvailability(it) }, label = { Text(stringResource(com.taytek.basehw.R.string.username_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (isUsernameAvailable == true && editingUsernameInput.length >= 3) {
                    Text(stringResource(com.taytek.basehw.R.string.available), color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                } else if (isUsernameAvailable == false && editingUsernameInput.length >= 3) {
                    Text(stringResource(com.taytek.basehw.R.string.unavailable), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(editingUsernameInput) }, enabled = !isLoading && isUsernameAvailable == true && editingUsernameInput.length >= 3 && editingUsernameInput != userData?.username) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text(stringResource(com.taytek.basehw.R.string.save_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text(stringResource(com.taytek.basehw.R.string.cancel_btn)) }
        }
    )
}

@Composable
fun ProfileScreenContent(
    padding: PaddingValues,
    scrollState: androidx.compose.foundation.ScrollState,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    uiState: ProfileUiState,
    totalCars: Int,
    totalValue: Double,
    currencySymbol: String,
    currencyCode: String,
    boxedCount: Int,
    themeState: Int,
    languageState: String,
    viewModel: ProfileViewModel,
    workManager: androidx.work.WorkManager,
    onStatisticsClick: () -> Unit,
    onSupportClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit,
    excelExportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onShowAuthSelection: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    
    Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentUser == null) {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.radialGradient(colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)))), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(110.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onShowAuthSelection, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp)) {
                    Text(stringResource(com.taytek.basehw.R.string.login_signup_btn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Text(text = stringResource(com.taytek.basehw.R.string.login_promo_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
            } else {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            if (currentUser.photoUrl != null) {
                                AsyncImage(model = currentUser.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(110.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp).size(28.dp).clip(CircleShape).background(Color(0xFF0033CC)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Verified, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val displayName = uiState.userData?.username ?: currentUser.email ?: "Collector"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (uiState.userData?.username != null) {
                            IconButton(onClick = { viewModel.setEditingUsername(true) }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.background(Color(0xFFE8EAF6), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stars, null, tint = Color(0xFF3F51B5), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = stringResource(com.taytek.basehw.R.string.premium_member), style = MaterialTheme.typography.labelSmall, color = Color(0xFF3F51B5), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(totalCars.toString(), stringResource(com.taytek.basehw.R.string.stat_cars), Modifier.weight(1f))
                StatCard("$currencySymbol${String.format(java.util.Locale.US, "%.0f", totalValue)}", stringResource(com.taytek.basehw.R.string.stat_value), Modifier.weight(1f))
                StatCard(boxedCount.toString(), stringResource(com.taytek.basehw.R.string.stat_boxed), Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            SectionHeader(stringResource(com.taytek.basehw.R.string.statistics_section))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (MaterialTheme.colorScheme.background == DarkNavy)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column {
                    ProfileListItem(Icons.Default.BarChart, stringResource(com.taytek.basehw.R.string.collection_statistics)) { onStatisticsClick() }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    CurrencySelector(currencyCode, viewModel)
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionHeader(stringResource(com.taytek.basehw.R.string.data_management))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (MaterialTheme.colorScheme.background == DarkNavy)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column {
                    if (currentUser != null) {
                        CloudBackupItem(uiState, viewModel)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                        RestoreDataItem(uiState, viewModel)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    }
                    ProfileListItem(Icons.Default.FileDownload, stringResource(com.taytek.basehw.R.string.excel_export_title)) { excelExportLauncher.launch("HotWheels_Collection.csv") }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    SyncGitHubItem(uiState, viewModel, workManager)
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionHeader(stringResource(com.taytek.basehw.R.string.settings_support_title))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (MaterialTheme.colorScheme.background == DarkNavy)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column {
                    ProfileListItem(Icons.Default.HelpOutline, stringResource(com.taytek.basehw.R.string.help_support), stringResource(com.taytek.basehw.R.string.contact_us_desc)) { onSupportClick() }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    ThemeSelector(themeState, viewModel)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    LanguageSelector(languageState, context, viewModel)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    ProfileListItem(Icons.Default.PrivacyTip, stringResource(com.taytek.basehw.R.string.privacy_policy)) { onPrivacyPolicyClick() }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    ProfileListItem(Icons.Default.Gavel, stringResource(com.taytek.basehw.R.string.terms_of_use)) { onTermsOfUseClick() }
                    
                    if (currentUser != null) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                        ProfileListItem(Icons.Default.Logout, stringResource(com.taytek.basehw.R.string.logout)) { 
                            coroutineScope.launch {
                                try {
                                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                } catch (e: Exception) { Log.e("Auth", "Clear state error: ${e.message}") }
                                viewModel.signOut()
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                        DeleteAccountItem(viewModel)
                    }
                }
            }
            ErrorMessages(uiState)
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (MaterialTheme.colorScheme.background == DarkNavy)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp).fillMaxWidth())
}

@Composable
fun ProfileListItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 18.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun CurrencySelector(currentCode: String, viewModel: ProfileViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().clickable { showMenu = true }.padding(vertical = 18.dp, horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CurrencyExchange, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(text = stringResource(com.taytek.basehw.R.string.currency_setting), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(text = currentCode, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            com.taytek.basehw.domain.model.AppCurrency.entries.forEach { currency ->
                DropdownMenuItem(text = { Text("${currency.code} (${currency.symbol})") }, onClick = { viewModel.setCurrency(currency.code); showMenu = false })
            }
        }
    }
}

@Composable
fun ThemeSelector(themeState: Int, viewModel: ProfileViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    val themeOptions = listOf(stringResource(com.taytek.basehw.R.string.theme_auto), stringResource(com.taytek.basehw.R.string.theme_light), stringResource(com.taytek.basehw.R.string.theme_dark))
    Box(modifier = Modifier.fillMaxWidth().clickable { showMenu = true }.padding(vertical = 18.dp, horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(text = stringResource(com.taytek.basehw.R.string.theme), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(text = themeOptions.getOrNull(themeState) ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            themeOptions.forEachIndexed { index, name -> DropdownMenuItem(text = { Text(name) }, onClick = { viewModel.setTheme(index); showMenu = false }) }
        }
    }
}

@Composable
fun LanguageSelector(languageState: String, context: android.content.Context, viewModel: ProfileViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    val langOptions = listOf("tr" to stringResource(com.taytek.basehw.R.string.language_tr), "en" to stringResource(com.taytek.basehw.R.string.language_en), "de" to stringResource(com.taytek.basehw.R.string.language_de))
    val currentLangName = langOptions.find { it.first == languageState }?.second ?: langOptions.first().second
    Box(modifier = Modifier.fillMaxWidth().clickable { showMenu = true }.padding(vertical = 18.dp, horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(text = stringResource(com.taytek.basehw.R.string.language), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(text = currentLangName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            langOptions.forEach { (code, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { viewModel.setLanguage(code); showMenu = false; (context as? Activity)?.recreate() }) }
        }
    }
}

@Composable
fun CloudBackupItem(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (MaterialTheme.colorScheme.background == DarkNavy)
                    MaterialTheme.colorScheme.surface
                else
                    AppPrimary.copy(alpha = 0.15f)
            )
            .clickable(enabled = !uiState.isLoading) { viewModel.backupToCloud() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CloudUpload,
                null,
                tint = if (MaterialTheme.colorScheme.background == DarkNavy) MaterialTheme.colorScheme.primary else AppPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(com.taytek.basehw.R.string.cloud_backup_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = if (uiState.isLoading && uiState.syncStatusMsg?.contains("Yedek") == true) uiState.syncStatusMsg else if (uiState.syncSuccess && uiState.syncStatusMsg?.contains("Yedek") == true) uiState.syncStatusMsg else if (uiState.error != null && uiState.error!!.contains("Yedek")) uiState.error else null
                if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = if (uiState.error != null && uiState.error!!.contains("Yedek")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun RestoreDataItem(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = !uiState.isLoading) { viewModel.restoreFromCloud() }.padding(vertical = 16.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.SettingsBackupRestore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(com.taytek.basehw.R.string.restore_data_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            val subtitle = if (uiState.isLoading && uiState.syncStatusMsg?.contains("Geri") == true) uiState.syncStatusMsg else if (uiState.syncSuccess && uiState.syncStatusMsg?.contains("Geri") == true) uiState.syncStatusMsg else if (uiState.error != null && uiState.error!!.contains("Geri")) uiState.error else null
            if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = if (uiState.error != null && uiState.error!!.contains("Geri")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun SyncGitHubItem(uiState: ProfileUiState, viewModel: ProfileViewModel, workManager: androidx.work.WorkManager) {
    Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.enqueueRemoteSync(workManager) }.padding(vertical = 18.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(com.taytek.basehw.R.string.pull_new_models), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            uiState.syncStatusResId?.let { Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }
        if (uiState.isSyncing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun DeleteAccountItem(viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(com.taytek.basehw.R.string.delete_account_dialog_title)) },
            text = { Text(stringResource(com.taytek.basehw.R.string.delete_account_dialog_text)) },
            confirmButton = { Button(onClick = { 
                showDialog = false
                coroutineScope.launch {
                    try {
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    } catch (e: Exception) { Log.e("Auth", "Clear state error: ${e.message}") }
                    viewModel.deleteAccount()
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(com.taytek.basehw.R.string.delete_account_confirm)) } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(com.taytek.basehw.R.string.delete_account_cancel)) } }
        )
    }
    Row(modifier = Modifier.fillMaxWidth().clickable { showDialog = true }.padding(vertical = 16.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DeleteForever, null, tint = Color.Red, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = stringResource(com.taytek.basehw.R.string.delete_account), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ErrorMessages(uiState: ProfileUiState) {
    val errorText = when (uiState.error) {
        "EMAIL_EMPTY_ERROR" -> stringResource(com.taytek.basehw.R.string.email_empty_error)
        else -> uiState.error
    }
    
    val statusText = when (uiState.syncStatusMsg) {
        "PASSWORD_RESET_SENT" -> stringResource(com.taytek.basehw.R.string.password_reset_sent)
        "SUCCESS_DELETE" -> stringResource(com.taytek.basehw.R.string.delete_account_success)
        "EXPORT_SUCCESS" -> null // Handled via snackbar usually, but we check here
        else -> uiState.syncStatusMsg
    }

    if (errorText != null && !errorText.contains("Yedek") && !errorText.contains("Geri")) {
        Text(
            text = errorText, 
            color = MaterialTheme.colorScheme.error, 
            style = MaterialTheme.typography.bodySmall, 
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
    }
    if (statusText != null && !statusText.contains("Yedek") && !statusText.contains("Geri") && statusText != "EXPORT_SUCCESS") {
        Text(
            text = statusText, 
            color = MaterialTheme.colorScheme.primary, 
            style = MaterialTheme.typography.bodySmall, 
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, 
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
    }
}

