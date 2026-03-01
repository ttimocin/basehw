package com.taytek.basehw.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.taytek.basehw.domain.repository.AuthRepository
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncSuccess: Boolean = false,
    val syncStatusMsg: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userCarRepository: UserCarRepository
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentUser
        )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isFailure) {
                _uiState.update { 
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Giriş başarısız.") 
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
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

    fun clearMessages() {
        _uiState.update { it.copy(error = null, syncSuccess = false, syncStatusMsg = null) }
    }
}
