package com.taytek.basehw.ui.screens.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taytek.basehw.domain.model.Feedback
import com.taytek.basehw.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportUiState(
    val subject: String = "",
    val message: String = "",
    val captchaQuestion: String = "",
    val captchaAnswer: Int = 0,
    val captchaInput: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val username: String = ""
)

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchUserInfo()
        generateCaptcha()
    }

    private fun fetchUserInfo() {
        viewModelScope.launch {
            val result = authRepository.getUserProfile()
            result.onSuccess { user ->
                _uiState.update { it.copy(username = user.username ?: "") }
            }
        }
    }

    fun generateCaptcha() {
        val num1 = (1..15).random()
        val num2 = (1..15).random()
        _uiState.update { 
            it.copy(
                captchaQuestion = "$num1 + $num2 = ?",
                captchaAnswer = num1 + num2,
                captchaInput = ""
            )
        }
    }

    fun updateSubject(value: String) { _uiState.update { it.copy(subject = value) } }
    fun updateMessage(value: String) { _uiState.update { it.copy(message = value) } }
    fun updateCaptchaInput(value: String) { _uiState.update { it.copy(captchaInput = value) } }

    fun submitFeedback(onSuccess: () -> Unit) {
        val state = _uiState.value
        
        if (state.captchaInput.toIntOrNull() != state.captchaAnswer) {
            _uiState.update { it.copy(error = "CAPTCHA_ERROR") }
            return
        }

        if (state.subject.isBlank() || state.message.isBlank()) {
            _uiState.update { it.copy(error = "FIELDS_EMPTY") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val feedback = Feedback(
                username = state.username,
                subject = state.subject,
                message = state.message
            )
            val result = authRepository.sendFeedback(feedback)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true, subject = "", message = "") }
                generateCaptcha()
                onSuccess()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun resetSuccess() { _uiState.update { it.copy(isSuccess = false) } }
}
