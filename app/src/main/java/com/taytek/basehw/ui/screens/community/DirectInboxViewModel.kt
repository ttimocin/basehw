package com.taytek.basehw.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taytek.basehw.domain.model.DirectConversation
import com.taytek.basehw.domain.repository.SupabaseSyncRepository
import com.taytek.basehw.util.MessageCrypto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DirectInboxUiState(
    val conversations: List<DirectConversation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DirectInboxViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val supabaseSyncRepository: SupabaseSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectInboxUiState())
    val uiState: StateFlow<DirectInboxUiState> = _uiState.asStateFlow()

    fun loadInbox() {
        val myUid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            supabaseSyncRepository.getInboxConversations(myUid).onSuccess { list ->
                // Son mesajları şifre çöz
                val decryptedList = list.map { conv ->
                    val key = MessageCrypto.generateConversationKey(myUid, conv.peerUid)
                    val decryptedMsg = if (MessageCrypto.isEncrypted(conv.lastMessage)) {
                        try {
                            MessageCrypto.decrypt(conv.lastMessage, key)
                        } catch (e: Exception) {
                            conv.lastMessage
                        }
                    } else {
                        conv.lastMessage
                    }
                    conv.copy(lastMessage = decryptedMsg)
                }
                _uiState.value = _uiState.value.copy(
                    conversations = decryptedList,
                    isLoading = false,
                    error = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Mesaj kutusu yuklenemedi"
                )
            }
        }
    }
}
