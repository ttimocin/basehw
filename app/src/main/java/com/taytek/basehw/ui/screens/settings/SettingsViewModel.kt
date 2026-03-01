package com.taytek.basehw.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.taytek.basehw.data.remote.firebase.FirebaseAuthDataSource
import com.taytek.basehw.data.worker.FandomSyncWorker
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.repository.MasterDataRepository
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isSignedIn: Boolean = false,
    val userId: String? = null,
    val isSyncing: Boolean = false,
    val syncStatus: String = "",
    val masterDataCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val userCarRepository: UserCarRepository,
    private val masterDataRepository: MasterDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                isSignedIn = authDataSource.isSignedIn,
                userId = authDataSource.currentUser?.uid
            )
        }
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val count = masterDataRepository.getCount()
            _uiState.update { it.copy(masterDataCount = count) }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            val user = authDataSource.signInAnonymously()
            _uiState.update { it.copy(isSignedIn = user != null, userId = user?.uid) }
        }
    }

    fun signOut() {
        authDataSource.signOut()
        _uiState.update { it.copy(isSignedIn = false, userId = null) }
    }

    fun syncCollectionToFirestore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncStatus = "Koleksiyon yükleniyor…") }
            try {
                userCarRepository.syncToFirestore()
                _uiState.update { it.copy(isSyncing = false, syncStatus = "Koleksiyon başarıyla senkronize edildi ✓") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, syncStatus = "Hata: ${e.message}") }
            }
        }
    }

    fun enqueueFandomSync(workManager: WorkManager) {
        Brand.entries.forEach { brand ->
            val request = OneTimeWorkRequestBuilder<FandomSyncWorker>()
                .setInputData(
                    workDataOf(FandomSyncWorker.KEY_BRAND to brand.name)
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("fandom_sync_${brand.name}")
                .build()

            workManager.enqueueUniqueWork(
                "fandom_sync_${brand.name}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
        _uiState.update { it.copy(syncStatus = "Model verisi indirme başladı… (arka planda devam eder)") }
    }
}
