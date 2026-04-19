package com.taytek.basehw.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import android.util.Log
import kotlinx.coroutines.flow.filterNotNull
import com.google.firebase.auth.FirebaseUser
import com.taytek.basehw.data.remote.network.SupabaseStorageDataSource
import com.taytek.basehw.domain.model.BadgeType
import com.taytek.basehw.domain.model.CollectionRankCalculator
import com.taytek.basehw.domain.model.fromInputs
import com.taytek.basehw.domain.repository.AuthRepository
import com.taytek.basehw.domain.repository.CommunityRepository
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import android.net.Uri
import com.taytek.basehw.domain.model.CollectionImportMode
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.taytek.basehw.domain.model.VehicleCondition
import com.taytek.basehw.R
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class ProfileUiState(
    val isLoading: Boolean = false,
    /** True while buluta yedekle / buluttan yükle runs (no full-screen [isLoading] overlay). */
    val isCloudDataOpRunning: Boolean = false,
    val error: String? = null,
    val syncSuccess: Boolean = false,
    val syncStatusMsg: String? = null,
    val userData: com.taytek.basehw.domain.model.User? = null,
    val isUsernameAvailable: Boolean? = null,
    val showUsernamePrompt: Boolean = false,
    val pendingUsernamePromptAfterConsent: Boolean = false,
    val isEditingUsername: Boolean = false,
    val isSyncing: Boolean = false,
    val syncStatusResId: Int? = null,
    // Auth Flow Refinements
    val email: String = "",
    val password: String = "",
    val isRegisterMode: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val showAuthOptionSelection: Boolean = false,
    val showEmailAuthFields: Boolean = false,
    val username: String = "",
    val consentGranted: Boolean = false,
    val isLoadingVerification: Boolean = false,
    val verificationEmailSent: Boolean = false,
    val showExportDialog: Boolean = false,
    val showRestorePrompt: Boolean = false,
    val isCloudCheckInProgress: Boolean = false,
    val showMandatoryConsentDialog: Boolean = false,
    val isEmailVerified: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val userCarRepository: UserCarRepository,
    private val communityRepository: CommunityRepository,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager,
    private val currencyRepository: com.taytek.basehw.domain.repository.CurrencyRepository,
    private val storageDataSource: SupabaseStorageDataSource
) : ViewModel() {

    val currencyCode: StateFlow<String> = appSettingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EUR")

    val currencySymbol: StateFlow<String> = currencyCode
        .map { com.taytek.basehw.domain.model.AppCurrency.fromCode(it).symbol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "€")

    private val conversionRate: StateFlow<Double> = combine(currencyRepository.getRates(), currencyCode) { rates: com.taytek.basehw.domain.model.CurrencyRates?, code: String ->
        val effectiveCode = if (code.isBlank()) "EUR" else code
        if (effectiveCode == "EUR") 1.0
        else rates?.rates?.get(effectiveCode) ?: 1.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    val totalValue: StateFlow<Double> = combine(userCarRepository.getTotalEstimatedValue(), conversionRate) { value: Double, rate: Double ->
        value * rate
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCars: StateFlow<Int> = userCarRepository.getTotalCarsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalBoxed: StateFlow<Int> = userCarRepository.getBoxStatusCounts()
        .map { list -> list.filter { it.condition != "LOOSE" }.sumOf { it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalOpened: StateFlow<Int> = userCarRepository.getBoxStatusCounts()
        .map { list -> list.find { it.condition == "LOOSE" }?.count ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeBadge: StateFlow<BadgeType> = userCarRepository.getRankCars()
        .map { cars ->
            val inputs = CollectionRankCalculator.calculate(cars)
            BadgeType.fromInputs(inputs)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BadgeType.ROOKIE)

    fun setCurrency(code: String) {
        appSettingsManager.setCurrency(code)
    }

    val themeFlow = appSettingsManager.themeFlow
    val languageFlow = appSettingsManager.languageFlow
    val fontFlow = appSettingsManager.fontFlow

    fun setTheme(theme: Int) {
        appSettingsManager.setTheme(theme)
    }

    fun setLanguage(languageCode: String) {
        appSettingsManager.setLanguage(languageCode)
    }

    fun setFontFamily(fontFamily: Int) {
        appSettingsManager.setFontFamily(fontFamily)
    }

    fun enqueueRemoteSync(workManager: androidx.work.WorkManager) {
        val request = androidx.work.OneTimeWorkRequestBuilder<com.taytek.basehw.data.worker.RemoteYearSyncWorker>()
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .addTag("remote_catalog_sync")
            .build()

        workManager.enqueueUniqueWork(
            "remote_catalog_sync",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )

        _uiState.update { it.copy(syncStatusResId = R.string.sync_in_progress, isSyncing = true) }

        viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(request.id).asFlow().filterNotNull().collect { workInfo ->
                when (workInfo.state) {
                    androidx.work.WorkInfo.State.SUCCEEDED -> {
                        _uiState.update { it.copy(
                            syncStatusResId = R.string.sync_success,
                            isSyncing = false
                        )}
                    }
                    androidx.work.WorkInfo.State.FAILED -> {
                        _uiState.update { it.copy(
                            syncStatusResId = R.string.sync_failed,
                            isSyncing = false
                        )}
                    }
                    androidx.work.WorkInfo.State.CANCELLED -> {
                        _uiState.update { it.copy(
                            syncStatusResId = R.string.sync_cancelled,
                            isSyncing = false
                        )}
                    }
                    else -> {}
                }
            }
        }
    }

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentUser
        )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            currencyRepository.refreshRates()
        }
        viewModelScope.launch {
            authRepository.currentUserFlow.collect { firebaseUser ->
                if (firebaseUser != null) {
                    val profileResult = authRepository.getUserProfile()
                    if (profileResult.isSuccess) {
                        val userProfile = profileResult.getOrNull()
                        val shouldPromptForUsername =
                            userProfile?.googleUsernameOnboardingRequired == true &&
                                userProfile.googleUsernameOnboardingCompleted != true
                        if (userProfile?.privacyAccepted == true) {
                            appSettingsManager.setAcceptedPrivacyTerms(true)
                        }
                        val needsConsent = userProfile?.privacyAccepted != true && !appSettingsManager.hasAcceptedPrivacyTerms()
                        _uiState.update {
                            it.copy(
                                userData = userProfile,
                                showMandatoryConsentDialog = needsConsent,
                                showUsernamePrompt = shouldPromptForUsername && !needsConsent,
                                pendingUsernamePromptAfterConsent = shouldPromptForUsername && needsConsent,
                                isEmailVerified = firebaseUser.isEmailVerified
                            )
                        }
                        viewModelScope.launch {
                            val localCarCount = runCatching { userCarRepository.getTotalCarsCount().first() }.getOrDefault(0)
                            if (localCarCount > 0) {
                                runCatching { userCarRepository.syncToSupabase() }
                                    .onFailure { e ->
                                        Log.w("ProfileViewModel", "Profile sync skipped: ${e.message}", e)
                                    }
                            }
                        }
                    } else {
                        // Fallback to minimal info if profile fetch fails
                        _uiState.update { 
                            it.copy(
                                userData = com.taytek.basehw.domain.model.User(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    photoUrl = firebaseUser.photoUrl?.toString()
                                ),
                                showMandatoryConsentDialog = !appSettingsManager.hasAcceptedPrivacyTerms(),
                                showUsernamePrompt = false,
                                pendingUsernamePromptAfterConsent = false
                            ) 
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            userData = null,
                            showMandatoryConsentDialog = false,
                            showUsernamePrompt = false,
                            pendingUsernamePromptAfterConsent = false
                        )
                    }
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                val shouldPromptForUsername =
                    user.googleUsernameOnboardingRequired && !user.googleUsernameOnboardingCompleted
                // Check if user has accepted privacy terms - show consent dialog if not
                val needsConsent = user.privacyAccepted != true
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userData = user,
                        showUsernamePrompt = shouldPromptForUsername && !needsConsent,
                        pendingUsernamePromptAfterConsent = shouldPromptForUsername && needsConsent,
                        showMandatoryConsentDialog = needsConsent
                    )
                }
                checkForBackupAfterLogin()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(isLoading = false, error = error.message ?: getString(R.string.vm_login_failed)) 
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signInWithEmail(email, password)
            result.onSuccess { user ->
                // Check if user has accepted privacy terms - show consent dialog if not
                val needsConsent = user.privacyAccepted != true
                _uiState.update { it.copy(isLoading = false, userData = user, showMandatoryConsentDialog = needsConsent) }
                checkForBackupAfterLogin()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(isLoading = false, error = error.message ?: getString(R.string.vm_login_failed)) 
                }
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, username: String) {
        if (!com.taytek.basehw.domain.util.AuthValidator.isPasswordValid(password)) {
            _uiState.update { it.copy(error = getString(R.string.vm_password_criteria)) }
            return
        }

        if (username.isNotBlank()) {
            if (!com.taytek.basehw.domain.util.AuthValidator.isUsernameFormatValid(username)) {
                _uiState.update { it.copy(error = getString(R.string.vm_username_format)) }
                return
            }

            if (!com.taytek.basehw.domain.util.AuthValidator.isUsernameClean(username)) {
                _uiState.update { it.copy(error = getString(R.string.vm_username_inappropriate)) }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            var finalUsername = username.trim()
            if (finalUsername.isEmpty()) {
                val generated = generateFallbackUsername(email)
                if (generated == null) {
                    _uiState.update { it.copy(isLoading = false, error = getString(R.string.vm_username_generation_failed)) }
                    return@launch
                }
                finalUsername = generated
            } else {
                // Re-check username availability if provided manually
                val availableResult = authRepository.checkUsernameAvailable(finalUsername)
                if (!availableResult.getOrDefault(false)) {
                    _uiState.update { it.copy(isLoading = false, error = getString(R.string.vm_username_taken)) }
                    return@launch
                }
            }

            val result = authRepository.signUpWithEmail(email, password, finalUsername)
            result.onSuccess { user ->
                // Auto send verification email on sign up
                authRepository.sendEmailVerification()
                // New users haven't accepted rules yet - show consent dialog
                _uiState.update { it.copy(isLoading = false, userData = user, showMandatoryConsentDialog = true) }
                checkForBackupAfterLogin()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message ?: getString(R.string.vm_register_failed)) }
            }
        }
    }

    fun sendEmailVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVerification = true, error = null) }
            val result = authRepository.sendEmailVerification()
            result.onSuccess {
                _uiState.update { it.copy(isLoadingVerification = false, verificationEmailSent = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoadingVerification = false, error = error.message) }
            }
        }
    }

    fun reloadUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVerification = true, error = null) }
            val result = authRepository.reloadUser()
            result.onSuccess { user ->
                _uiState.update { 
                    it.copy(
                        isLoadingVerification = false,
                        isEmailVerified = user?.isEmailVerified ?: false
                    ) 
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoadingVerification = false, error = error.message) }
            }
        }
    }

    fun checkUsernameAvailability(username: String) {
        if (username.length < 3 || username.length > 8) {
            _uiState.update { it.copy(isUsernameAvailable = null) }
            return
        }
        viewModelScope.launch {
            val result = authRepository.checkUsernameAvailable(username)
            if (result.isSuccess) {
                _uiState.update { it.copy(isUsernameAvailable = result.getOrNull()) }
            } else {
                _uiState.update { 
                    it.copy(
                        isUsernameAvailable = null, 
                        error = getString(R.string.vm_username_check_failed) 
                    ) 
                }
            }
        }
    }

    fun assignUsername(username: String) {
        val trimmedUsername = username.trim()
        if (!com.taytek.basehw.domain.util.AuthValidator.isUsernameFormatValid(trimmedUsername) ||
            !com.taytek.basehw.domain.util.AuthValidator.isUsernameClean(trimmedUsername)) {
            _uiState.update { it.copy(error = getString(R.string.vm_username_invalid)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val available = authRepository.checkUsernameAvailable(trimmedUsername).getOrDefault(false)
            if (available) {
                val result = authRepository.updateUsername(trimmedUsername)
                result.onSuccess {
                    authRepository.completeGoogleUsernameOnboarding()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showUsernamePrompt = false,
                            pendingUsernamePromptAfterConsent = false,
                            userData = it.userData?.copy(
                                username = trimmedUsername,
                                googleUsernameOnboardingRequired = false,
                                googleUsernameOnboardingCompleted = true
                            )
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = getString(R.string.vm_username_taken)) }
            }
        }
    }

    fun skipUsernameSelection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val generatedUsername = generateFallbackUsername(_uiState.value.userData?.email)
            if (generatedUsername != null) {
                val result = authRepository.updateUsername(generatedUsername)
                result.onSuccess {
                    authRepository.completeGoogleUsernameOnboarding()
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            showUsernamePrompt = false, 
                            pendingUsernamePromptAfterConsent = false,
                            userData = it.userData?.copy(
                                username = generatedUsername,
                                googleUsernameOnboardingRequired = false,
                                googleUsernameOnboardingCompleted = true
                            ) 
                        ) 
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = getString(R.string.vm_username_assign_failed)) }
            }
        }
    }

    private suspend fun generateFallbackUsername(email: String?): String? {
        val rawLocal = email?.substringBefore("@").orEmpty()
        val cleaned = rawLocal.lowercase().filter { it.isLetterOrDigit() || it == '_' }
        var base = cleaned.ifBlank { "user" }
        if (base.length < 3) {
            base = (base + "usr").take(3)
        }
        base = base.take(8)

        if (authRepository.checkUsernameAvailable(base).getOrDefault(false)) {
            return base
        }

        for (suffix in 1..99) {
            val suffixText = suffix.toString()
            val candidateBaseLength = 8 - suffixText.length
            if (candidateBaseLength < 3) continue
            val candidate = base.take(candidateBaseLength) + suffixText
            if (authRepository.checkUsernameAvailable(candidate).getOrDefault(false)) {
                return candidate
            }
        }

        return null
    }

    fun setEditingUsername(isEditing: Boolean) {
        _uiState.update { it.copy(isEditingUsername = isEditing, error = null, isUsernameAvailable = null) }
    }

    fun saveEditedUsername(newUsername: String) {
        if (!com.taytek.basehw.domain.util.AuthValidator.isUsernameFormatValid(newUsername) || 
            !com.taytek.basehw.domain.util.AuthValidator.isUsernameClean(newUsername)) {
            _uiState.update { it.copy(error = getString(R.string.vm_username_invalid)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val availableResult = authRepository.checkUsernameAvailable(newUsername)
            
            if (availableResult.isSuccess && availableResult.getOrDefault(false)) {
                val updateResult = authRepository.updateUsername(newUsername)
                updateResult.onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isEditingUsername = false, 
                            userData = it.userData?.copy(username = newUsername) 
                        ) 
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            } else if (availableResult.isSuccess) {
                _uiState.update { it.copy(isLoading = false, error = getString(R.string.vm_username_taken)) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = getString(R.string.vm_username_check_failed)) }
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "EMAIL_EMPTY_ERROR") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isFailure) {
                _uiState.update { 
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: getString(R.string.vm_reset_failed)) 
                }
            } else {
                _uiState.update { it.copy(isLoading = false, syncStatusMsg = "PASSWORD_RESET_SENT") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            if (!_uiState.value.rememberMe) {
                resetAuthState()
            } else {
                // Keep email, but clear password for security and reset mode to Login
                _uiState.update { 
                    it.copy(
                        password = "", 
                        isPasswordVisible = false, 
                        isRegisterMode = false,
                        showUsernamePrompt = false,
                        pendingUsernamePromptAfterConsent = false,
                        showMandatoryConsentDialog = false,
                        error = null
                    ) 
                }
            }
        }
    }

    fun backupToCloud() {
        val user = authRepository.currentUser
        if (user == null || user.isAnonymous) {
            _uiState.update { it.copy(error = getString(R.string.login_required_backup)) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCloudDataOpRunning = true,
                    error = null,
                    syncSuccess = false,
                    syncStatusMsg = getString(R.string.vm_backing_up)
                )
            }
            try {
                userCarRepository.syncToSupabase()
                _uiState.update {
                    it.copy(
                        isCloudDataOpRunning = false,
                        syncSuccess = true,
                        syncStatusMsg = getString(R.string.vm_backup_success)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCloudDataOpRunning = false,
                        error = e.localizedMessage ?: getString(R.string.vm_backup_error)
                    )
                }
            }
        }
    }

    fun restoreFromCloud() {
        val user = authRepository.currentUser
        if (user == null || user.isAnonymous) {
            _uiState.update { it.copy(error = getString(R.string.login_required_backup)) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCloudDataOpRunning = true,
                    error = null,
                    syncSuccess = false,
                    syncStatusMsg = getString(R.string.vm_restoring),
                    showRestorePrompt = false
                )
            }
            try {
                userCarRepository.syncFromSupabase()
                _uiState.update {
                    it.copy(
                        isCloudDataOpRunning = false,
                        syncSuccess = true,
                        syncStatusMsg = getString(R.string.vm_restore_success)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCloudDataOpRunning = false,
                        error = e.localizedMessage ?: getString(R.string.vm_restore_error)
                    )
                }
            }
        }
    }

    private fun checkForBackupAfterLogin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudCheckInProgress = true) }
            // Wait for auth to settle
            kotlinx.coroutines.delay(2000)
            
            val totalCars = userCarRepository.getTotalCarsCount().first()
            if (totalCars == 0) {
                if (userCarRepository.hasCloudData()) {
                    _uiState.update { it.copy(showRestorePrompt = true) }
                }
            }
            _uiState.update { it.copy(isCloudCheckInProgress = false) }
        }
    }

    fun dismissRestorePrompt() {
        _uiState.update { it.copy(showRestorePrompt = false) }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, syncStatusMsg = getString(R.string.vm_cleaning_data)) }

            // 1) Delete remote account data + Firebase user
            _uiState.update { it.copy(syncStatusMsg = getString(R.string.vm_deleting_account)) }
            val authResult = authRepository.deleteAccount()

            if (authResult.isFailure) {
                val errorMsg = authResult.exceptionOrNull()?.message
                if (errorMsg == "REAUTH_REQUIRED") {
                    // Force sign out so they can sign in fresh and try again
                    signOut() // Clear state normally
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = getString(R.string.vm_delete_account_reauth_required)
                        ) 
                    }
                } else if (errorMsg?.startsWith("REMOTE_CLEANUP_FAILED:") == true) {
                    val reason = errorMsg.removePrefix("REMOTE_CLEANUP_FAILED:").ifBlank { "unknown_error" }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = getString(R.string.vm_remote_cleanup_failed_with_link).format(reason)
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(isLoading = false, error = errorMsg ?: "AUTH_DELETE_ERROR") 
                    }
                }
            } else {
                // 2) Clear local cache/state
                userCarRepository.clearLocalData()
                resetAuthState() // FULL RESET after deletion
                _uiState.update { it.copy(isLoading = false, syncStatusMsg = "SUCCESS_DELETE") }
            }
        }
    }

    suspend fun exportToExcel(outputStream: OutputStream) {
        _uiState.update { it.copy(isLoading = true, error = null, syncStatusMsg = null, showExportDialog = false) }
        try {
                val cars = userCarRepository.getAllCarsWithMasterList()
                val rate = conversionRate.value
                val symbol = currencySymbol.value
                val lang = languageFlow.value.ifBlank { "tr" }
                
                withContext(Dispatchers.IO) {
                    val writer = outputStream.bufferedWriter(java.nio.charset.Charset.forName("ISO-8859-9"))
                    val sep = ";"
                    
                    // Localized Headers (ASCII-safe for TR)
                    val columns = when(lang) {
                        "en" -> listOf("Brand", "Model", "Year", "Series", "Status", "Toy Num", "Col Num", "Case", "Feature", "Category", "Purchase Date", "Price ($symbol)", "Value ($symbol)", "Note", "Location")
                        "de" -> listOf("Marke", "Modell", "Jahr", "Serie", "Status", "Spielzeug Nr", "Kollektions-Nr", "Case", "Merkmal", "Kategorie", "Kaufdatum", "Preis ($symbol)", "Wert ($symbol)", "Notiz", "Standort")
                        "es" -> listOf("Marca", "Modelo", "Año", "Serie", "Estado", "Nº Juguete", "Nº Colección", "Caja", "Característica", "Categoría", "Fecha de Compra", "Precio ($symbol)", "Valor ($symbol)", "Nota", "Ubicación")
                        else -> listOf("Marka", "Model", "Yil", "Seri", "Durum", "Toy Num", "Col Num", "Case", "Ozellik", "Kategori", "Satin Alim Tarihi", "Fiyat ($symbol)", "Deger ($symbol)", "Not", "Konum")
                    }
                    
                    writer.write(columns.joinToString(sep))
                    writer.newLine()
                    
                    // Data
                    cars.forEach { car ->
                        val priceStr = car.purchasePrice?.let { 
                            val converted = it * rate
                            "$symbol${String.format(java.util.Locale.US, "%.2f", converted)}"
                        } ?: "0.00"
                        
                        val valueStr = car.estimatedValue?.let { 
                            val converted = it * rate
                            "$symbol${String.format(java.util.Locale.US, "%.2f", converted)}"
                        } ?: "0.00"

                        val cond = car.condition
                        val statusStr = getStatusString(lang, cond)

                        val row = listOf(
                            car.masterData?.brand?.name ?: car.manualBrand?.name ?: "",
                            car.masterData?.modelName?.replace(sep, ",") ?: car.manualModelName?.replace(sep, ",") ?: "",
                            (car.masterData?.year ?: car.manualYear)?.toString() ?: "",
                            car.masterData?.series?.replace(sep, ",") ?: car.manualSeries?.replace(sep, ",") ?: "",
                            statusStr,
                            car.masterData?.toyNum ?: "",
                            car.masterData?.colNum ?: "",
                            car.masterData?.caseNum ?: "",
                            car.masterData?.feature ?: "",
                            car.masterData?.category ?: "",
                            car.purchaseDate?.toString()?.replace(sep, ",") ?: "",
                            priceStr,
                            valueStr,
                            car.personalNote.replace("\n", " ").replace(sep, ","),
                            car.storageLocation.replace(sep, ",")
                        )
                        writer.write(row.joinToString(sep))
                        writer.newLine()
                    }
                    
                    writer.flush()
                    writer.close()
                }
                
                _uiState.update { it.copy(isLoading = false, syncStatusMsg = "EXPORT_SUCCESS") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = getString(R.string.vm_export_failed).format(e.message)) }
            }
    }

    suspend fun exportToJson(outputStream: OutputStream) {
        _uiState.update { it.copy(isLoading = true, error = null, showExportDialog = false) }
        try {
            val cars = userCarRepository.getAllCarsWithMasterList()
            val rate = conversionRate.value
            val symbol = currencySymbol.value
            
            val cleanData = cars.map { car ->
                mutableMapOf<String, Any?>().apply {
                    val brandCode = car.masterData?.brand?.name ?: car.manualBrand?.name
                    val brand = car.masterData?.brand?.displayName ?: car.manualBrand?.displayName
                    val model = car.masterData?.modelName ?: car.manualModelName
                    
                    if (!brand.isNullOrBlank()) put("marka", brand)
                    if (!brandCode.isNullOrBlank()) put("marka_kod", brandCode)
                    if (!model.isNullOrBlank()) put("model", model)
                    
                    // Master Data Details (Only if not blank)
                    car.masterData?.let { md ->
                        if (md.series.isNotBlank()) put("seri", md.series)
                        if (md.seriesNum.isNotBlank()) put("seri_no", md.seriesNum)
                        md.year?.let { put("yil", it) }
                        if (md.color.isNotBlank()) put("renk", md.color)
                        if (md.toyNum.isNotBlank()) put("toy_no", md.toyNum)
                        if (md.colNum.isNotBlank()) put("col_no", md.colNum)
                        if (md.caseNum.isNotBlank()) put("case", md.caseNum)
                        md.feature?.let { if (it.isNotBlank()) put("ozellik", it) }
                        md.category?.let { if (it.isNotBlank()) put("kategori", it) }
                    } ?: run {
                        // Manual Data fallback
                        car.manualYear?.let { put("yil", it) }
                        car.manualSeries?.let { if (it.isNotBlank()) put("seri", it) }
                    }
                    
                    // User Collection Data
                    val condObj = car.condition
                    put("durum", condObj.name)
                    if (car.personalNote.isNotBlank()) put("not", car.personalNote)
                    if (car.storageLocation.isNotBlank()) put("konum", car.storageLocation)
                    
                    car.purchasePrice?.let { if (it > 0) {
                        val converted = it * rate
                        put("fiyat", "$symbol${String.format(java.util.Locale.US, "%.2f", converted)}") 
                    } }
                    car.estimatedValue?.let { if (it > 0) {
                        val converted = it * rate
                        put("deger", "$symbol${String.format(java.util.Locale.US, "%.2f", converted)}") 
                    } }
                    
                    if (car.quantity > 1) put("adet", car.quantity)
                    if (car.isFavorite) put("favori", true)
                    if (car.isCustom) put("custom", true)
                    if (car.isWishlist) put("wishlist", true)
                }
            }

            val gson = com.google.gson.GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
            val root = mapOf(
                "exportVersion" to 2,
                "currency" to currencyCode.value.ifBlank { "EUR" },
                "cars" to cleanData
            )
            val jsonString = gson.toJson(root)
            
            withContext(Dispatchers.IO) {
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(jsonString) }
            }
            _uiState.update { it.copy(isLoading = false, syncStatusMsg = "EXPORT_SUCCESS") }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = getString(R.string.vm_export_failed).format(e.message)) }
        }
    }

    suspend fun exportToPdf(outputStream: OutputStream) {
        _uiState.update { it.copy(isLoading = true, error = null, showExportDialog = false) }
        try {
            val cars = userCarRepository.getAllCarsWithMasterList()
            val rate = conversionRate.value
            val symbol = currencySymbol.value
            
            withContext(Dispatchers.IO) {
                // ... (existing PDF code same up to car values) ...
                val pdfDocument = PdfDocument()
                val pageWidth = 595 // A4 width in points
                val pageHeight = 842 // A4 height in points
                val margin = 40f
                
                val titlePaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 20f
                    color = android.graphics.Color.BLACK
                }
                
                val headerPaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 10f
                    color = android.graphics.Color.WHITE
                }
                
                val dataPaint = Paint().apply {
                    textSize = 9f
                    color = android.graphics.Color.BLACK
                }
                
                val footerPaint = Paint().apply {
                    textSize = 8f
                    color = android.graphics.Color.GRAY
                }

                val headerBgPaint = Paint().apply {
                    color = android.graphics.Color.rgb(180, 0, 0) // HotWheels Red-ish
                }
                
                val rowBgPaint = Paint().apply {
                    color = android.graphics.Color.rgb(245, 245, 245)
                }

                val lang = languageFlow.value.ifBlank { "tr" }
                val cols = when(lang) {
                    "en" -> listOf("Brand", "Model", "Series", "Year", "Status", "Price", "Value")
                    "de" -> listOf("Marke", "Modell", "Serie", "Jahr", "Status", "Preis", "Wert")
                    "es" -> listOf("Marca", "Modelo", "Serie", "Año", "Estado", "Precio", "Valor")
                    else -> listOf("Marka", "Model", "Seri", "Yıl", "Durum", "Fiyat", "Değer")
                }
                val colWidths = listOf(75f, 135f, 100f, 35f, 55f, 55f, 60f)
                val rowHeight = 22f
                
                var pageNumber = 1
                var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas
                
                canvas.drawText("HotWheels Koleksiyon Raporu", margin, 60f, titlePaint)
                val dateStr = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                canvas.drawText("Tarih: $dateStr", margin, 80f, dataPaint)
                
                var currentY = 110f
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 25f, headerBgPaint)
                var currentX = margin + 5f
                cols.forEachIndexed { index, title ->
                    canvas.drawText(title, currentX, currentY + 17f, headerPaint)
                    currentX += colWidths[index]
                }
                currentY += 25f

                cars.forEachIndexed { carIndex, car ->
                    if (currentY + rowHeight > pageHeight - margin - 30f) {
                        canvas.drawText("Sayfa $pageNumber", pageWidth / 2f - 20f, pageHeight - 20f, footerPaint)
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        
                        currentY = margin
                        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 25f, headerBgPaint)
                        currentX = margin + 5f
                        cols.forEachIndexed { index, title ->
                            canvas.drawText(title, currentX, currentY + 17f, headerPaint)
                            currentX += colWidths[index]
                        }
                        currentY += 25f
                    }
                    
                    if (carIndex % 2 == 1) {
                        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, rowBgPaint)
                    }
                    
                    val pCond = car.condition
                    
                    currentX = margin + 5f
                    val carStatus = getStatusString(lang, pCond)

                    val carValues = listOf(
                        car.masterData?.brand?.displayName ?: car.manualBrand?.displayName ?: "",
                        car.masterData?.modelName ?: car.manualModelName ?: "",
                        car.masterData?.series ?: car.manualSeries ?: "",
                        (car.masterData?.year ?: car.manualYear)?.toString() ?: "-",
                        carStatus,
                        car.purchasePrice?.let { 
                            val converted = it * rate
                            "$symbol${String.format(java.util.Locale.US, "%.2f", converted)}"
                        } ?: "0.00",
                        car.estimatedValue?.let { 
                            val converted = it * rate
                            "$symbol${String.format(java.util.Locale.US, "%.2f", converted)}"
                        } ?: "0.00"
                    )
                    
                    carValues.forEachIndexed { index, value ->
                        canvas.drawText(value, currentX, currentY + 15f, dataPaint)
                        currentX += colWidths[index]
                    }
                    
                    currentY += rowHeight
                }
                
                canvas.drawText("Sayfa $pageNumber", pageWidth / 2f - 20f, pageHeight - 20f, footerPaint)
                pdfDocument.finishPage(page)
                pdfDocument.writeTo(outputStream)
                pdfDocument.close()
            }
            _uiState.update { it.copy(isLoading = false, syncStatusMsg = "EXPORT_SUCCESS") }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = getString(R.string.vm_export_failed).format(e.message)) }
        }
    }

    fun importCollectionFromUri(uri: Uri, mimeType: String?, mode: CollectionImportMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, syncStatusMsg = null) }
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val result = userCarRepository.importCollection(
                        stream,
                        mimeType,
                        mode,
                        conversionRate.value
                    )
                    result.fold(
                        onSuccess = { stats ->
                            userCarRepository.triggerSync()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    syncSuccess = true,
                                    syncStatusMsg = context.getString(
                                        R.string.import_collection_result,
                                        stats.added,
                                        stats.skippedDuplicates,
                                        stats.skippedIncomplete,
                                        stats.parseFailures
                                    )
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = e.message ?: context.getString(R.string.import_collection_failed)
                                )
                            }
                        }
                    )
                } ?: _uiState.update {
                    it.copy(isLoading = false, error = context.getString(R.string.import_collection_failed))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.import_collection_failed)
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                error = null,
                syncSuccess = false,
                syncStatusMsg = null,
                isCloudDataOpRunning = false
            )
        }
    }

    fun onExportClick() {
        _uiState.update { it.copy(showExportDialog = true) }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { it.copy(error = message) }
    }

    fun updateCollectionVisibility(isPublic: Boolean) {
        val userData = _uiState.value.userData ?: return
        val newCollection = isPublic
        val currentWishlist = userData.isWishlistPublic
        // Optimistic UI update - toggle immediately
        _uiState.update {
            it.copy(userData = it.userData?.copy(isCollectionPublic = newCollection))
        }
        viewModelScope.launch {
            authRepository.updateVisibilitySettings(newCollection, currentWishlist)
                .onSuccess {
                    viewModelScope.launch {
                        runCatching { userCarRepository.syncToSupabase() }
                            .onFailure { e ->
                                Log.w("ProfileViewModel", "Visibility sync skipped: ${e.message}", e)
                            }
                    }
                }
                .onFailure { e ->
                    // Rollback on failure
                    _uiState.update {
                        it.copy(
                            userData = it.userData?.copy(isCollectionPublic = !newCollection),
                            error = e.message ?: getString(R.string.vm_visibility_error)
                        )
                    }
                }
        }
    }

    fun updateWishlistVisibility(isPublic: Boolean) {
        val userData = _uiState.value.userData ?: return
        val currentCollection = userData.isCollectionPublic
        val newWishlist = isPublic
        // Optimistic UI update - toggle immediately
        _uiState.update {
            it.copy(userData = it.userData?.copy(isWishlistPublic = newWishlist))
        }
        viewModelScope.launch {
            authRepository.updateVisibilitySettings(currentCollection, newWishlist)
                .onSuccess {
                    viewModelScope.launch {
                        runCatching { userCarRepository.syncToSupabase() }
                            .onFailure { e ->
                                Log.w("ProfileViewModel", "Visibility sync skipped: ${e.message}", e)
                            }
                    }
                }
                .onFailure { e ->
                    // Rollback on failure
                    _uiState.update {
                        it.copy(
                            userData = it.userData?.copy(isWishlistPublic = !newWishlist),
                            error = e.message ?: getString(R.string.vm_visibility_error)
                        )
                    }
                }
        }
    }

    // Avatar Management
    fun selectDefaultAvatar(avatarId: Int) {
        val userData = _uiState.value.userData

        if (userData == null) {
            android.util.Log.e("ProfileViewModel", "userData is null, cannot select avatar")
            return
        }
        viewModelScope.launch {

            authRepository.updateProfileAvatar(avatarId, null) // null = varsayılan avatar, custom URL temizle
                .onSuccess {

                    _uiState.update {
                        it.copy(userData = it.userData?.copy(selectedAvatarId = avatarId, customAvatarUrl = null))
                    }
                }
                .onFailure { e ->
                    android.util.Log.e("ProfileViewModel", "Avatar update FAILED: ${e.message}", e)
                    _uiState.update { it.copy(error = e.message ?: getString(R.string.vm_avatar_select_error)) }
                }
        }
    }

    fun uploadCustomAvatar(uri: android.net.Uri) {
        val userData = _uiState.value.userData

        if (userData == null) {
            android.util.Log.e("ProfileViewModel", "userData is null, cannot upload avatar")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val previousAvatarUrl = authRepository.getCurrentCustomAvatarUrl().getOrNull()

                val avatarUrl = storageDataSource.uploadUserProfileAvatar(userData.uid, uri.toString())

                if (avatarUrl != null) {
                    if (!previousAvatarUrl.isNullOrBlank() && previousAvatarUrl != avatarUrl) {
                        val deleted = storageDataSource.deleteByPublicUrl(previousAvatarUrl)

                    }

                    authRepository.updateProfileAvatar(0, avatarUrl) // 0 = custom upload, URL'yi kaydet
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userData = it.userData?.copy(selectedAvatarId = 0, customAvatarUrl = avatarUrl)
                        )
                    }

                } else {
                    android.util.Log.e("ProfileViewModel", "Avatar URL is null after upload")
                    _uiState.update { it.copy(isLoading = false, error = getString(R.string.vm_avatar_upload_error)) }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Avatar upload exception: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: getString(R.string.vm_avatar_upload_exception)) }
            }
        }
    }

    // Auth State Management
    fun updateEmail(value: String) { _uiState.update { it.copy(email = value) } }
    fun updatePassword(value: String) { _uiState.update { it.copy(password = value) } }
    fun setRegisterMode(isRegister: Boolean) { _uiState.update { it.copy(isRegisterMode = isRegister, error = null) } }
    fun toggleRegisterMode() { _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, error = null) } }
    fun togglePasswordVisibility() { _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) } }
    fun toggleRememberMe() { _uiState.update { it.copy(rememberMe = !it.rememberMe) } }
    fun updateAuthUsername(value: String) { 
        val sanitized = value.take(8)
        _uiState.update { it.copy(username = sanitized) } 
        checkUsernameAvailability(sanitized)
    }
    fun setShowAuthOptionSelection(show: Boolean) { _uiState.update { it.copy(showAuthOptionSelection = show) } }
    fun setShowEmailAuthFields(show: Boolean) { _uiState.update { it.copy(showEmailAuthFields = show) } }
    fun toggleConsent() { _uiState.update { it.copy(consentGranted = !it.consentGranted) } }

    fun acceptMandatoryConsent() {
        appSettingsManager.setAcceptedPrivacyTerms(true)
        viewModelScope.launch {
            authRepository.acceptPrivacyTerms()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showMandatoryConsentDialog = false,
                            showUsernamePrompt = it.pendingUsernamePromptAfterConsent,
                            pendingUsernamePromptAfterConsent = false,
                            userData = it.userData?.copy(privacyAccepted = true),
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: getString(R.string.accept_terms_error)) }
                }
        }
    }

    fun declineMandatoryConsent() {
        signOut()
    }
    
    private fun getStatusString(lang: String, cond: VehicleCondition): String {
        return when (lang) {
            "en" -> when (cond) {
                VehicleCondition.MINT -> "Mint"
                VehicleCondition.NEAR_MINT -> "Near Mint"
                VehicleCondition.DAMAGED -> "Damaged"
                VehicleCondition.LOOSE -> "Loose"
            }
            "de" -> when (cond) {
                VehicleCondition.MINT -> "OVP (Mint)"
                VehicleCondition.NEAR_MINT -> "OVP (Near Mint)"
                VehicleCondition.DAMAGED -> "Beschadigt"
                VehicleCondition.LOOSE -> "Lose"
            }
            "es" -> when (cond) {
                VehicleCondition.MINT -> "En caja (Mint)"
                VehicleCondition.NEAR_MINT -> "En caja (Near Mint)"
                VehicleCondition.DAMAGED -> "Danado"
                VehicleCondition.LOOSE -> "Abierto"
            }
            else -> when (cond) {
                VehicleCondition.MINT -> "Kapali (Mint)"
                VehicleCondition.NEAR_MINT -> "Kapali (Near Mint)"
                VehicleCondition.DAMAGED -> "Hasarli"
                VehicleCondition.LOOSE -> "Acilmis"
            }
        }
    }

    fun resetAuthState() {
        _uiState.update { 
            it.copy(
                email = "",
                password = "",
                username = "",
                isRegisterMode = false,
                isPasswordVisible = false,
                showAuthOptionSelection = false,
                showEmailAuthFields = false,
                consentGranted = false,
                showUsernamePrompt = false,
                pendingUsernamePromptAfterConsent = false,
                showMandatoryConsentDialog = false,
                error = null
            )
        }
    }

    private fun getString(resId: Int): String {
        return context.getString(resId)
    }
}
