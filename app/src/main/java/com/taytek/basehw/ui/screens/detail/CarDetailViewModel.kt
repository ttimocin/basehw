package com.taytek.basehw.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.usecase.DeleteCarFromCollectionUseCase
import com.taytek.basehw.domain.usecase.GetCarByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.delay

data class DetailUiState(
    val car: UserCar? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val isPhotoOptionMenuVisible: Boolean = false,
    val isUrlInputDialogVisible: Boolean = false,
    val isSavingSeries: Boolean = false,
    val isSeriesInWishlist: Boolean = false,
    val seriesJustAdded: Boolean = false
)

@HiltViewModel
class CarDetailViewModel @Inject constructor(
    private val getCarByIdUseCase: GetCarByIdUseCase,
    private val deleteCarFromCollectionUseCase: DeleteCarFromCollectionUseCase,
    private val customCollectionRepository: com.taytek.basehw.domain.repository.CustomCollectionRepository,
    private val userCarRepository: com.taytek.basehw.domain.repository.UserCarRepository,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager,
    private val currencyRepository: com.taytek.basehw.domain.repository.CurrencyRepository
) : ViewModel() {

    val currencyCode: Flow<String> = appSettingsManager.currencyFlow

    val currencySymbol: StateFlow<String> = currencyCode
        .map { com.taytek.basehw.domain.model.AppCurrency.fromCode(it).symbol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "€")

    val conversionRate: StateFlow<Double> = combine(currencyRepository.getRates(), currencyCode) { rates, code ->
        val effectiveCode = if (code.isBlank()) "EUR" else code
        if (effectiveCode == "EUR") 1.0
        else rates?.rates?.get(effectiveCode) ?: 1.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    val allCollections = customCollectionRepository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            currencyRepository.refreshRates()
        }
    }

    fun loadCar(id: Long) {
        viewModelScope.launch {
            getCarByIdUseCase(id)
                .onEach { car ->
                    _uiState.update { it.copy(car = car, isLoading = false) }
                    val brand = car?.masterData?.brand ?: car?.manualBrand
                    val series = car?.masterData?.series?.takeIf { it.isNotBlank() } ?: car?.manualSeries
                    if (brand != null && !series.isNullOrBlank()) {
                        val inWishlist = userCarRepository.isSeriesInWishlist(brand, series)
                        _uiState.update { it.copy(isSeriesInWishlist = inWishlist) }
                    }
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

    fun addToCollection(collectionId: Long) {
        val carId = _uiState.value.car?.id ?: return
        viewModelScope.launch {
            customCollectionRepository.addCarToCollection(collectionId, carId)
        }
    }

    fun removeFromCollection(collectionId: Long) {
        val carId = _uiState.value.car?.id ?: return
        viewModelScope.launch {
            customCollectionRepository.removeCarFromCollection(collectionId, carId)
        }
    }

    fun toggleFavorite() {
        val car = _uiState.value.car ?: return
        viewModelScope.launch {
            try {
                userCarRepository.updateCar(car.copy(isFavorite = !car.isFavorite))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateStorageLocation(location: String) {
        val car = _uiState.value.car ?: return
        viewModelScope.launch {
            try {
                userCarRepository.updateCar(car.copy(storageLocation = location))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun parsePrice(price: String): Double? {
        if (price.isBlank()) return null
        return try {
            price.replace(",", ".").toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun updatePurchasePrice(priceStr: String) {
        val car = _uiState.value.car ?: return
        val price = parsePrice(priceStr)
        val currentRate = conversionRate.value
        val storedPrice = price?.let { it / currentRate }
        viewModelScope.launch {
            try {
                userCarRepository.updateCar(car.copy(purchasePrice = storedPrice))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateEstimatedValue(valueStr: String) {
        val car = _uiState.value.car ?: return
        val value = parsePrice(valueStr)
        val currentRate = conversionRate.value
        val storedValue = value?.let { it / currentRate }
        viewModelScope.launch {
            try {
                userCarRepository.updateCar(car.copy(estimatedValue = storedValue))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updatePurchaseDate(date: Date?) {
        val car = _uiState.value.car ?: return
        viewModelScope.launch {
            try {
                userCarRepository.updateCar(car.copy(purchaseDate = date))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updatePersonalNote(note: String) {
        val car = _uiState.value.car ?: return
        viewModelScope.launch {
            try {
                userCarRepository.updateCar(car.copy(personalNote = note))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun togglePhotoOptionMenu(visible: Boolean) {
        _uiState.update { it.copy(isPhotoOptionMenuVisible = visible) }
    }

    fun toggleUrlInputDialog(visible: Boolean) {
        _uiState.update { it.copy(isUrlInputDialogVisible = visible, isPhotoOptionMenuVisible = false) }
    }

    fun addSeriesToWishlist() {
        val car = _uiState.value.car ?: return
        val brand = car.masterData?.brand ?: car.manualBrand ?: return
        val series = car.masterData?.series?.takeIf { it.isNotBlank() } ?: car.manualSeries ?: return
        val year = car.masterData?.year ?: car.manualYear
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingSeries = true) }
            try {
                userCarRepository.addSeriesToWishlist(brand, series, year)
                _uiState.update { it.copy(isSavingSeries = false, isSeriesInWishlist = true, seriesJustAdded = true) }
                delay(1500)
                _uiState.update { it.copy(seriesJustAdded = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingSeries = false, error = e.message) }
            }
        }
    }

    fun onUserPhotoUrlChanged(url: String?) {
        val car = _uiState.value.car ?: return
        viewModelScope.launch {
            try {
                userCarRepository.updateCar(car.copy(userPhotoUrl = url))
                _uiState.update { it.copy(isUrlInputDialogVisible = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
