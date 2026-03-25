package com.taytek.basehw.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.filterNotNull
import com.google.firebase.auth.FirebaseUser
import com.taytek.basehw.domain.repository.AuthRepository
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
import java.io.OutputStream
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncSuccess: Boolean = false,
    val syncStatusMsg: String? = null,
    val userData: com.taytek.basehw.domain.model.User? = null,
    val isUsernameAvailable: Boolean? = null,
    val showUsernamePrompt: Boolean = false,
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
    val consentGranted: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userCarRepository: UserCarRepository,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager,
    private val currencyRepository: com.taytek.basehw.domain.repository.CurrencyRepository
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
        .map { list -> list.find { !it.isOpened }?.count ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalOpened: StateFlow<Int> = userCarRepository.getBoxStatusCounts()
        .map { list -> list.find { it.isOpened }?.count ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setCurrency(code: String) {
        appSettingsManager.setCurrency(code)
    }

    val themeFlow = appSettingsManager.themeFlow
    val languageFlow = appSettingsManager.languageFlow

    fun setTheme(theme: Int) {
        appSettingsManager.setTheme(theme)
    }

    fun setLanguage(languageCode: String) {
        appSettingsManager.setLanguage(languageCode)
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

        _uiState.update { it.copy(syncStatusResId = com.taytek.basehw.R.string.sync_in_progress, isSyncing = true) }

        viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(request.id).asFlow().filterNotNull().collect { workInfo ->
                when (workInfo.state) {
                    androidx.work.WorkInfo.State.SUCCEEDED -> {
                        _uiState.update { it.copy(
                            syncStatusResId = com.taytek.basehw.R.string.sync_success,
                            isSyncing = false
                        )}
                    }
                    androidx.work.WorkInfo.State.FAILED -> {
                        _uiState.update { it.copy(
                            syncStatusResId = com.taytek.basehw.R.string.sync_failed,
                            isSyncing = false
                        )}
                    }
                    androidx.work.WorkInfo.State.CANCELLED -> {
                        _uiState.update { it.copy(
                            syncStatusResId = com.taytek.basehw.R.string.sync_cancelled,
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
                        _uiState.update { it.copy(userData = profileResult.getOrNull()) }
                    } else {
                        // Fallback to minimal info if firestore fetch fails
                        _uiState.update { 
                            it.copy(
                                userData = com.taytek.basehw.domain.model.User(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    photoUrl = firebaseUser.photoUrl?.toString()
                                )
                            ) 
                        }
                    }
                } else {
                    _uiState.update { it.copy(userData = null) }
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        userData = user,
                        showUsernamePrompt = user.username == null // Prompt if no username
                    ) 
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(isLoading = false, error = error.message ?: "Giriş başarısız.") 
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signInWithEmail(email, password)
            result.onSuccess { user ->
                _uiState.update { it.copy(isLoading = false, userData = user) }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(isLoading = false, error = error.message ?: "Giriş başarısız.") 
                }
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, username: String, consentGranted: Boolean) {
        if (!consentGranted) {
            _uiState.update { it.copy(error = "Lütfen veri kullanım onayını işaretleyin.") }
            return
        }

        if (!com.taytek.basehw.domain.util.AuthValidator.isPasswordValid(password)) {
            _uiState.update { it.copy(error = "Şifre en az 8 karakter olmalı; büyük harf, küçük harf ve rakam içermelidir.") }
            return
        }

        if (username.isNotBlank()) {
            if (!com.taytek.basehw.domain.util.AuthValidator.isUsernameFormatValid(username)) {
                _uiState.update { it.copy(error = "Kullanıcı adı 3-20 karakter olmalı ve sadece harf, rakam ve alt çizgi içermelidir.") }
                return
            }

            if (!com.taytek.basehw.domain.util.AuthValidator.isUsernameClean(username)) {
                _uiState.update { it.copy(error = "Uygunsuz kullanıcı adı.") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            var finalUsername = username.trim()
            if (finalUsername.isEmpty()) {
                var uniqueUsernameFound = false
                var attempt = 0
                while (!uniqueUsernameFound && attempt < 5) {
                    val randomSuffix = (10000..99999).random()
                    finalUsername = "user_$randomSuffix"
                    val available = authRepository.checkUsernameAvailable(finalUsername).getOrDefault(false)
                    if (available) {
                        uniqueUsernameFound = true
                    }
                    attempt++
                }
                if (!uniqueUsernameFound) {
                    _uiState.update { it.copy(isLoading = false, error = "Kullanıcı adı oluşturulamadı. Lütfen manuel girin.") }
                    return@launch
                }
            } else {
                // Re-check username availability if provided manually
                val availableResult = authRepository.checkUsernameAvailable(finalUsername)
                if (!availableResult.getOrDefault(false)) {
                    _uiState.update { it.copy(isLoading = false, error = "Bu kullanıcı adı zaten alınmış.") }
                    return@launch
                }
            }

            val result = authRepository.signUpWithEmail(email, password, finalUsername)
            result.onSuccess { user ->
                _uiState.update { it.copy(isLoading = false, userData = user) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message ?: "Kayıt başarısız.") }
            }
        }
    }

    fun checkUsernameAvailability(username: String) {
        if (username.length < 3) {
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
                        error = "Kullanıcı adı kontrol edilemedi, lütfen Firebase kurallarınızı kontrol edin." 
                    ) 
                }
            }
        }
    }

    fun assignUsername(username: String) {
        if (!com.taytek.basehw.domain.util.AuthValidator.isUsernameFormatValid(username) || 
            !com.taytek.basehw.domain.util.AuthValidator.isUsernameClean(username)) {
            _uiState.update { it.copy(error = "Geçersiz veya uygunsuz kullanıcı adı.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val available = authRepository.checkUsernameAvailable(username).getOrDefault(false)
            if (available) {
                val result = authRepository.updateUsername(username)
                result.onSuccess {
                    _uiState.update { it.copy(isLoading = false, showUsernamePrompt = false, userData = it.userData?.copy(username = username)) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Bu kullanıcı adı zaten alınmış.") }
            }
        }
    }

    fun skipUsernameSelection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            var uniqueUsernameFound = false
            var attempt = 0
            var generatedUsername = ""
            
            while (!uniqueUsernameFound && attempt < 5) {
                val randomSuffix = (10000..99999).random()
                generatedUsername = "user_$randomSuffix"
                val available = authRepository.checkUsernameAvailable(generatedUsername).getOrDefault(false)
                if (available) {
                    uniqueUsernameFound = true
                }
                attempt++
            }
            
            if (uniqueUsernameFound) {
                val result = authRepository.updateUsername(generatedUsername)
                result.onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            showUsernamePrompt = false, 
                            userData = it.userData?.copy(username = generatedUsername) 
                        ) 
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Kullanıcı adı atanamadı, lütfen tekrar deneyin.") }
            }
        }
    }

    fun setEditingUsername(isEditing: Boolean) {
        _uiState.update { it.copy(isEditingUsername = isEditing, error = null, isUsernameAvailable = null) }
    }

    fun saveEditedUsername(newUsername: String) {
        if (!com.taytek.basehw.domain.util.AuthValidator.isUsernameFormatValid(newUsername) || 
            !com.taytek.basehw.domain.util.AuthValidator.isUsernameClean(newUsername)) {
            _uiState.update { it.copy(error = "Geçersiz veya uygunsuz kullanıcı adı.") }
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
                _uiState.update { it.copy(isLoading = false, error = "Bu kullanıcı adı zaten alınmış.") }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Bağlantı hatası, kontrol edilemedi.") }
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
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Sıfırlama e-postası gönderilemedi.") 
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
                        error = null
                    ) 
                }
            }
        }
    }

    fun backupToCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, syncSuccess = false, syncStatusMsg = "Yedekleniyor...") }
            try {
                userCarRepository.syncToFirestore()
                _uiState.update { it.copy(isLoading = false, syncSuccess = true, syncStatusMsg = "Yedekleme başarılı!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Yedekleme hatası.") }
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, syncSuccess = false, syncStatusMsg = "Geri yükleniyor...") }
            try {
                userCarRepository.syncFromFirestore()
                _uiState.update { it.copy(isLoading = false, syncSuccess = true, syncStatusMsg = "Geri yükleme başarılı!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Geri yükleme hatası.") }
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, syncStatusMsg = "Veriler temizleniyor...") }
            
            // 1. Clear Firestore Data
            val dataResult = userCarRepository.deleteCloudData()
            if (dataResult.isFailure) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "Veri temizleme hatası: ${dataResult.exceptionOrNull()?.message}") 
                }
                return@launch
            }

            // 2. Delete Auth Account
            _uiState.update { it.copy(syncStatusMsg = "Hesap siliniyor...") }
            val authResult = authRepository.deleteAccount()
            
            if (authResult.isFailure) {
                val errorMsg = authResult.exceptionOrNull()?.message
                if (errorMsg == "REAUTH_REQUIRED") {
                    // Force sign out so they can sign in fresh and try again
                    signOut() // Clear state normally
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = "REAUTH_REQUIRED"
                        ) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(isLoading = false, error = errorMsg ?: "AUTH_DELETE_ERROR") 
                    }
                }
            } else {
                // 3. Clear Local Data
                userCarRepository.clearLocalData()
                resetAuthState() // FULL RESET after deletion
                _uiState.update { it.copy(isLoading = false, syncStatusMsg = "SUCCESS_DELETE") }
            }
        }
    }

    fun exportToExcel(outputStream: OutputStream) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, syncStatusMsg = null) }
            try {
                val cars = userCarRepository.getAllCarsWithMasterList()
                
                withContext(Dispatchers.IO) {
                    // Write UTF-8 BOM for Excel compatibility
                    outputStream.write(0xEF)
                    outputStream.write(0xBB)
                    outputStream.write(0xBF)
                    
                    val writer = outputStream.bufferedWriter(Charsets.UTF_8)
                    
                    // Header
                    val columns = listOf("Brand", "Model", "Year", "Series", "Status", "Purchase Date", "Price", "Value", "Note", "Location")
                    writer.write(columns.joinToString(";"))
                    writer.newLine()
                    
                    // Data
                    cars.forEach { car ->
                        val row = listOf(
                            car.masterData?.brand?.name ?: "",
                            car.masterData?.modelName?.replace(";", ",") ?: "",
                            car.masterData?.year?.toString() ?: "",
                            car.masterData?.series?.replace(";", ",") ?: "",
                            if (car.isOpened) "Opened" else "Mint",
                            car.purchaseDate?.toString()?.replace(";", ",") ?: "",
                            car.purchasePrice?.toString() ?: "0.0",
                            car.estimatedValue?.toString() ?: "0.0",
                            car.personalNote.replace("\n", " ").replace(";", ","),
                            car.storageLocation.replace(";", ",")
                        )
                        writer.write(row.joinToString(";"))
                        writer.newLine()
                    }
                    
                    writer.flush()
                    writer.close()
                }
                
                _uiState.update { it.copy(isLoading = false, syncStatusMsg = "EXPORT_SUCCESS") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Export failed: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, syncSuccess = false, syncStatusMsg = null) }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { it.copy(error = message) }
    }

    // Auth State Management
    fun updateEmail(value: String) { _uiState.update { it.copy(email = value) } }
    fun updatePassword(value: String) { _uiState.update { it.copy(password = value) } }
    fun setRegisterMode(isRegister: Boolean) { _uiState.update { it.copy(isRegisterMode = isRegister, error = null) } }
    fun toggleRegisterMode() { _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, error = null) } }
    fun togglePasswordVisibility() { _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) } }
    fun toggleRememberMe() { _uiState.update { it.copy(rememberMe = !it.rememberMe) } }
    fun updateAuthUsername(value: String) { 
        _uiState.update { it.copy(username = value) } 
        checkUsernameAvailability(value)
    }
    fun setShowAuthOptionSelection(show: Boolean) { _uiState.update { it.copy(showAuthOptionSelection = show) } }
    fun setShowEmailAuthFields(show: Boolean) { _uiState.update { it.copy(showEmailAuthFields = show) } }
    fun toggleConsent() { _uiState.update { it.copy(consentGranted = !it.consentGranted) } }
    
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
                error = null
            )
        }
    }
}
