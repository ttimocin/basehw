package com.taytek.basehw.ui.screens.addcar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.usecase.AddCarToCollectionUseCase
import com.taytek.basehw.domain.usecase.SearchMasterDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class AddCarUiState(
    val selectedBrand: Brand = Brand.HOT_WHEELS,
    val searchQuery: String = "",
    val selectedMasterData: MasterData? = null,
    val isOpened: Boolean = false,
    val purchaseDate: Date? = null,
    val personalNote: String = "",
    val storageLocation: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddCarViewModel @Inject constructor(
    private val searchMasterDataUseCase: SearchMasterDataUseCase,
    private val addCarToCollectionUseCase: AddCarToCollectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCarUiState())
    val uiState: StateFlow<AddCarUiState> = _uiState.asStateFlow()

    // Trigger for search: brand + query pair
    private val _searchTrigger = MutableStateFlow(Pair(Brand.HOT_WHEELS, ""))

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: Flow<PagingData<MasterData>> = _searchTrigger
        .debounce(300L)  // 300ms debounce as required
        .distinctUntilChanged()
        .flatMapLatest { (brand, query) ->
            searchMasterDataUseCase(brand, query)
        }
        .cachedIn(viewModelScope)

    fun onBrandSelected(brand: Brand) {
        _uiState.update { it.copy(selectedBrand = brand, selectedMasterData = null, searchQuery = "") }
        _searchTrigger.value = Pair(brand, "")
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, selectedMasterData = null) }
        _searchTrigger.value = Pair(_uiState.value.selectedBrand, query)
    }

    fun onMasterDataSelected(masterData: MasterData) {
        _uiState.update {
            it.copy(
                selectedMasterData = masterData,
                searchQuery = masterData.modelName
            )
        }
    }

    fun onIsOpenedChanged(isOpened: Boolean) {
        _uiState.update { it.copy(isOpened = isOpened) }
    }

    fun onPurchaseDateChanged(date: Date?) {
        _uiState.update { it.copy(purchaseDate = date) }
    }

    fun onPersonalNoteChanged(note: String) {
        _uiState.update { it.copy(personalNote = note) }
    }

    fun onStorageLocationChanged(location: String) {
        _uiState.update { it.copy(storageLocation = location) }
    }

    fun addCarToCollection() {
        saveCarToDatabase(isWishlist = false)
    }

    fun addCarToWishlist() {
        saveCarToDatabase(isWishlist = true)
    }

    private fun saveCarToDatabase(isWishlist: Boolean) {
        val state = _uiState.value
        val masterData = state.selectedMasterData ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                addCarToCollectionUseCase(
                    UserCar(
                        masterDataId = masterData.id,
                        isOpened = state.isOpened,
                        purchaseDate = state.purchaseDate,
                        personalNote = state.personalNote,
                        storageLocation = state.storageLocation,
                        isWishlist = isWishlist
                    )
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearSaveSuccess() { _uiState.update { it.copy(saveSuccess = false) } }
}
