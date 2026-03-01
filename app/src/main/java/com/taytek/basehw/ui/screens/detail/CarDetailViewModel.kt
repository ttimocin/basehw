package com.taytek.basehw.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.usecase.DeleteCarFromCollectionUseCase
import com.taytek.basehw.domain.usecase.GetCarByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val car: UserCar? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CarDetailViewModel @Inject constructor(
    private val getCarByIdUseCase: GetCarByIdUseCase,
    private val deleteCarFromCollectionUseCase: DeleteCarFromCollectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadCar(id: Long) {
        viewModelScope.launch {
            getCarByIdUseCase(id)
                .onEach { car ->
                    _uiState.update { it.copy(car = car, isLoading = false) }
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect()
        }
    }

    fun deleteCar() {
        val carId = _uiState.value.car?.id ?: return
        viewModelScope.launch {
            try {
                deleteCarFromCollectionUseCase(carId)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
