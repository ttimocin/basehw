package com.taytek.basehw.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taytek.basehw.data.local.AppSettingsManager
import com.taytek.basehw.domain.model.DirectMessage
import com.taytek.basehw.domain.repository.SupabaseSyncRepository
import com.taytek.basehw.domain.repository.CommunityRepository
import com.taytek.basehw.util.MessageCrypto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DirectMessageUiState(
    val messages: List<DirectMessage> = emptyList(),
    val messageInput: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val dmCooldownRemaining: String? = null
)

@HiltViewModel
class DirectMessageViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val supabaseSyncRepository: SupabaseSyncRepository,
    private val appSettingsManager: AppSettingsManager,
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectMessageUiState())
    val uiState: StateFlow<DirectMessageUiState> = _uiState.asStateFlow()
    private var pollingJob: Job? = null

    fun loadConversation(peerUid: String, showLoading: Boolean = true) {
        val myUid = auth.currentUser?.uid ?: return
        if (showLoading) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        }
        viewModelScope.launch {
            supabaseSyncRepository.getConversationMessages(myUid, peerUid).onSuccess { messages ->
                // Mesajları çöz
                val key = MessageCrypto.generateConversationKey(myUid, peerUid)
                val decryptedMessages = messages.map { msg ->
                    if (MessageCrypto.isEncrypted(msg.body)) {
                        try {
                            val decryptedBody = MessageCrypto.decrypt(msg.body, key)
                            msg.copy(body = decryptedBody)
                        } catch (e: Exception) {
                            // Çözme başarısızsa orijinal metni koru
                            msg
                        }
                    } else {
                        msg
                    }
                }
                _uiState.value = _uiState.value.copy(
                    messages = decryptedMessages,
                    isLoading = false,
                    error = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Mesajlar yuklenemedi"
                )
            }
        }
    }

    fun startConversationPolling(peerUid: String) {
        stopConversationPolling()
        appSettingsManager.setCommunityInboxLastSeenAt(System.currentTimeMillis())
        loadConversation(peerUid, showLoading = true)
        val myUid = auth.currentUser?.uid ?: return
        val key = MessageCrypto.generateConversationKey(myUid, peerUid)
        pollingJob = viewModelScope.launch {
            supabaseSyncRepository.observeConversationMessages(myUid, peerUid).collect { incoming ->
                _uiState.update { state ->
                    if (state.messages.any { it.id == incoming.id }) {
                        state
                    } else {
                        // Gelen mesajı çöz
                        val decryptedMsg = if (MessageCrypto.isEncrypted(incoming.body)) {
                            try {
                                val decryptedBody = MessageCrypto.decrypt(incoming.body, key)
                                incoming.copy(body = decryptedBody)
                            } catch (e: Exception) {
                                incoming
                            }
                        } else {
                            incoming
                        }
                        state.copy(messages = (state.messages + decryptedMsg).sortedBy { it.createdAt })
                    }
                }
            }
        }
    }

    fun stopConversationPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun markInboxSeen() {
        appSettingsManager.setCommunityInboxLastSeenAt(System.currentTimeMillis())
    }

    private companion object {
        const val DM_LIMIT_COUNT = 10
        const val DM_LIMIT_WINDOW_MILLIS = 60 * 1000L // 1 dakika
    }

    fun onMessageInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(messageInput = value)
    }

    fun sendMessage(peerUid: String) {
        val myUid = auth.currentUser?.uid ?: return
        val text = _uiState.value.messageInput.trim()
        if (text.isBlank()) return

        // Check if either user has blocked the other
        viewModelScope.launch {
            val isBlocked = runCatching {
                communityRepository.getBlockedUsers().getOrNull()?.any { it.uid == peerUid } == true
            }.getOrDefault(false)
            
            val hasBlockedMe = runCatching {
                communityRepository.hasUserBlockedMe(peerUid).getOrDefault(false)
            }.getOrDefault(false)
            
            if (isBlocked || hasBlockedMe) {
                _uiState.value = _uiState.value.copy(
                    error = "Bu kullanıcıyla mesajlaşamazsınız.",
                    isSending = false
                )
                return@launch
            }
            
            proceedWithSend(myUid, peerUid, text)
        }
    }
    
    private fun proceedWithSend(myUid: String, peerUid: String, text: String) {
        // Mesajı şifrele
        val key = MessageCrypto.generateConversationKey(myUid, peerUid)
        val encryptedText = try {
            MessageCrypto.encrypt(text, key)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isSending = false,
                error = "Mesaj şifrelenemedi"
            )
            return
        }
        // DM rate limiting check
        val now = System.currentTimeMillis()
        val recentMessages = appSettingsManager
            .getDirectMessageTimestamps(myUid)
            .filter { now - it < DM_LIMIT_WINDOW_MILLIS }

        if (recentMessages.size >= DM_LIMIT_COUNT) {
            val remainingMillis = (recentMessages.minOrNull() ?: now) + DM_LIMIT_WINDOW_MILLIS - now
            _uiState.value = _uiState.value.copy(
                error = "Arka arkaya en fazla 10 mesaj gönderebilirsin. Yeni mesaj için ${formatCooldown(remainingMillis)} bekle",
                dmCooldownRemaining = formatCooldown(remainingMillis)
            )
            return
        }

        _uiState.value = _uiState.value.copy(isSending = true, error = null, dmCooldownRemaining = null)
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                supabaseSyncRepository.syncProfile(
                    firebaseUid = myUid,
                    displayName = null, // Do not send Google name to avoid overwriting chosen nickname
                    photoUrl = currentUser.photoUrl?.toString()
                )
            }

            val conversationId = buildConversationId(myUid, peerUid)
            supabaseSyncRepository.createMessage(
                firebaseUid = myUid,
                conversationId = conversationId,
                receiverUid = peerUid,
                messageBody = encryptedText
            ).onSuccess {
                _uiState.value = _uiState.value.copy(messageInput = "", isSending = false)
                loadConversation(peerUid, showLoading = false)

                // Save message timestamp for rate limiting
                val now2 = System.currentTimeMillis()
                val updated = appSettingsManager
                    .getDirectMessageTimestamps(myUid)
                    .filter { now2 - it < DM_LIMIT_WINDOW_MILLIS } + now2
                appSettingsManager.setDirectMessageTimestamps(myUid, updated)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message ?: "Mesaj gonderilemedi"
                )
            }
        }
    }

    private fun formatCooldown(remainingMillis: Long): String {
        val safe = remainingMillis.coerceAtLeast(0L)
        val totalSeconds = ((safe + 999L) / 1000L).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    private fun buildConversationId(uidA: String, uidB: String): String {
        return if (uidA <= uidB) "${uidA}__${uidB}" else "${uidB}__${uidA}"
    }

    override fun onCleared() {
        stopConversationPolling()
        super.onCleared()
    }
}
