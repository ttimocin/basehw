package com.taytek.basehw.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.asFlow
import com.taytek.basehw.data.worker.RemoteYearSyncWorker
import com.taytek.basehw.domain.repository.MasterDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull

data class SettingsUiState(
    val syncStatus: String = "",
    val isSyncing: Boolean = false,
    val masterDataCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val masterDataRepository: MasterDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val count = masterDataRepository.getCount()
            _uiState.update { it.copy(masterDataCount = count) }
        }
    }

    fun enqueueRemoteSync(workManager: WorkManager) {
        val request = OneTimeWorkRequestBuilder<RemoteYearSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("github_remote_sync")
            .build()

        workManager.enqueueUniqueWork(
            "github_remote_sync",
            ExistingWorkPolicy.REPLACE,
            request
        )

        _uiState.update { it.copy(syncStatus = "GitHub'dan yeni araçlar indiriliyor...", isSyncing = true) }

        viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(request.id).asFlow().filterNotNull().collect { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _uiState.update { it.copy(
                            syncStatus = "Başarılı! Yeni araçlar kaydedildi. ✅",
                            isSyncing = false
                        )}
                        loadStats() // Refresh the car count on the UI
                    }
                    WorkInfo.State.FAILED -> {
                        _uiState.update { it.copy(
                            syncStatus = "Güncelleme başarısız oldu. Lütfen interneti kontrol edin. ❌",
                            isSyncing = false
                        )}
                    }
                    WorkInfo.State.CANCELLED -> {
                        _uiState.update { it.copy(
                            syncStatus = "İptal edildi. 🛑",
                            isSyncing = false
                        )}
                    }
                    else -> {}
                }
            }
        }
    }
}
