package com.taytek.basehw.ui.screens.collections.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taytek.basehw.domain.model.CustomCollection
import com.taytek.basehw.domain.repository.CustomCollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderDetailUiState(
    val collection: CustomCollection? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    private val repository: CustomCollectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderDetailUiState())
    val uiState: StateFlow<FolderDetailUiState> = _uiState.asStateFlow()

    fun loadFolder(id: Long) {
        viewModelScope.launch {
            repository.getCollectionById(id)
                .onEach { coll ->
                    _uiState.update { it.copy(collection = coll, isLoading = false) }
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect()
        }
    }

    fun removeCarFromFolder(folderId: Long, carId: Long) {
        viewModelScope.launch {
            repository.removeCarFromCollection(folderId, carId)
        }
    }
}
