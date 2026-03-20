package com.taytek.basehw.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.UserCarRepository
import com.taytek.basehw.domain.usecase.AddCarToCollectionUseCase
import com.taytek.basehw.domain.usecase.GetMasterDataByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MasterDetailUiState(
    val masterData: MasterData? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSaving: Boolean = false,
    val savingAction: SavingAction = SavingAction.NONE,
    val navigateToWishlist: Boolean = false
)

enum class SavingAction {
    NONE,
    SINGLE,
    SERIES
}

@HiltViewModel
class MasterDetailViewModel @Inject constructor(
    private val getMasterDataByIdUseCase: GetMasterDataByIdUseCase,
    private val addCarToCollectionUseCase: AddCarToCollectionUseCase,
    private val userCarRepository: UserCarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MasterDetailUiState())
    val uiState: StateFlow<MasterDetailUiState> = _uiState.asStateFlow()

    fun loadMasterData(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val master = getMasterDataByIdUseCase(id)
                _uiState.update { it.copy(masterData = master, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun addToWishlist() {
        if (_uiState.value.isSaving) return
        val master = _uiState.value.masterData ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, savingAction = SavingAction.SINGLE, error = null) }
            try {
                addCarToCollectionUseCase(
                    UserCar(
                        masterDataId = master.id,
                        isWishlist = true
                    )
                )
                _uiState.update { it.copy(isSaving = false, savingAction = SavingAction.NONE, navigateToWishlist = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, savingAction = SavingAction.NONE, error = e.message) }
            }
        }
    }

    fun addSeriesToWishlist() {
        if (_uiState.value.isSaving) return
        val master = _uiState.value.masterData ?: return
        if (master.series.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, savingAction = SavingAction.SERIES, error = null) }
            try {
                userCarRepository.addSeriesToWishlist(master.brand, master.series, master.year)
                _uiState.update { it.copy(isSaving = false, savingAction = SavingAction.NONE, navigateToWishlist = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, savingAction = SavingAction.NONE, error = e.message) }
            }
        }
    }

    fun clearNavigationEvent() {
        _uiState.update { it.copy(navigateToWishlist = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
