package com.taytek.basehw.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taytek.basehw.domain.model.CommunityNotification
import com.taytek.basehw.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<CommunityNotification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun loadNotifications() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.getNotifications()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(notifications = list, isLoading = false)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(error = err.localizedMessage, isLoading = false)
                }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId).onSuccess {
                // Update local list instantly
                val current = _uiState.value.notifications
                val updated = current.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                _uiState.value = _uiState.value.copy(notifications = updated)
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId).onSuccess {
                // Update local list instantly
                val current = _uiState.value.notifications
                val updated = current.filter { it.id != notificationId }
                _uiState.value = _uiState.value.copy(notifications = updated)
            }
        }
    }
}
