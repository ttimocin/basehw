package com.taytek.basehw.ui.screens.addcar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.model.CurrencyRates
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.model.VehicleCondition
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

@HiltViewModel
class AddWantedCarViewModel @Inject constructor(
    private val searchMasterDataUseCase: SearchMasterDataUseCase,
    private val addCarToCollectionUseCase: AddCarToCollectionUseCase,
    private val userCarRepository: com.taytek.basehw.domain.repository.UserCarRepository,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager,
    private val currencyRepository: com.taytek.basehw.domain.repository.CurrencyRepository
) : ViewModel() {

    private val currencyCode: StateFlow<String> = appSettingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EUR")

    val conversionRate: StateFlow<Double> = combine(currencyRepository.getRates(), currencyCode) { rates, code ->
        val effectiveCode = if (code.isBlank()) "EUR" else code
        if (effectiveCode == "EUR") 1.0
        else rates?.rates?.get(effectiveCode) ?: 1.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    val currencySymbol: StateFlow<String> = currencyCode
        .map { com.taytek.basehw.domain.model.AppCurrency.fromCode(it).symbol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "€")

    private val _uiState = MutableStateFlow(AddCarUiState())
    val uiState: StateFlow<AddCarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            currencyRepository.refreshRates()
        }
        
        viewModelScope.launch {
            appSettingsManager.languageFlow.take(1).collect { code ->
                val defaultCurrency = if (code == "tr") "TRY" else "USD"
                _uiState.update { it.copy(selectedCurrency = com.taytek.basehw.domain.model.AppCurrency.fromCode(defaultCurrency)) }
            }
        }
    }

    private val _searchTrigger = MutableStateFlow(Pair(Brand.HOT_WHEELS, ""))

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: Flow<PagingData<MasterData>> = _searchTrigger
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { (brand, query) ->
            searchMasterDataUseCase(brand, query)
        }
        .cachedIn(viewModelScope)

    fun onBrandSelected(brand: Brand) {
        _uiState.update { it.copy(selectedBrand = brand, selectedMasterData = null, searchQuery = "") }
        _searchTrigger.value = Pair(brand, "")
    }

    fun onCurrencySelected(currency: com.taytek.basehw.domain.model.AppCurrency) {
        _uiState.update { it.copy(selectedCurrency = currency) }
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

    fun onConditionChanged(condition: VehicleCondition) {
        _uiState.update { it.copy(condition = condition) }
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

    fun onUserPhotoUrlChanged(url: String?) {
        _uiState.update { it.copy(userPhotoUrl = url, isPhotoOptionMenuVisible = false, isUrlInputDialogVisible = false) }
    }

    fun togglePhotoOptionMenu(show: Boolean) {
        _uiState.update { it.copy(isPhotoOptionMenuVisible = show) }
    }

    fun toggleUrlInputDialog(show: Boolean) {
        _uiState.update { it.copy(isUrlInputDialogVisible = show, isPhotoOptionMenuVisible = false) }
    }

    fun onPurchasePriceChanged(price: String) {
        _uiState.update { it.copy(purchasePrice = price) }
    }

    fun onEstimatedValueChanged(value: String) {
        _uiState.update { it.copy(estimatedValue = value) }
    }

    fun toggleManualMode() {
        _uiState.update { it.copy(isManualMode = !it.isManualMode, selectedMasterData = null) }
    }

    fun onManualModelNameChanged(name: String) {
        _uiState.update { it.copy(manualModelName = name) }
    }

    fun onManualBrandChanged(brand: Brand) {
        _uiState.update { it.copy(manualBrand = brand) }
    }

    fun onManualYearChanged(year: String) {
        _uiState.update { it.copy(manualYear = year) }
    }

    fun onManualSeriesChanged(series: String) {
        _uiState.update { it.copy(manualSeries = series) }
    }

    fun onManualSeriesNumChanged(num: String) {
        _uiState.update { it.copy(manualSeriesNum = num) }
    }

    fun onManualScaleChanged(scale: String) {
        _uiState.update { it.copy(manualScale = scale) }
    }

    fun onManualIsPremiumChanged(isPremium: Boolean) {
        _uiState.update { it.copy(manualIsPremium = isPremium) }
    }

    fun addCarToWishlist() {
        saveCarToDatabase(isWishlist = true)
    }

    fun addSeriesToWishlist() {
        val state = _uiState.value
        val master = state.selectedMasterData ?: return
        if (master.series.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                userCarRepository.addSeriesToWishlist(master.brand, master.series, master.year)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
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

    private fun saveCarToDatabase(isWishlist: Boolean) {
        val state = _uiState.value
        if (!state.isManualMode && state.selectedMasterData == null) return
        if (state.isManualMode && state.manualModelName.isBlank()) {
            _uiState.update { it.copy(error = "Model ismi boş bırakılamaz.") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                // Fetch live rates safely before computing saving multiplier. 
                // Repository is initialized from persistent cache, so this is usually instant.
                val rates = currencyRepository.getRates().filterNotNull().first()

                val selectedCode = state.selectedCurrency?.code ?: "EUR"
                val rate = if (selectedCode == "EUR") 1.0 else rates.rates[selectedCode] ?: 1.0
                
                val storedPurchasePrice = parsePrice(state.purchasePrice)?.let { it / rate }
                val storedEstimatedValue = parsePrice(state.estimatedValue)?.let { it / rate }

                val carToAdd = if (state.isManualMode) {
                    UserCar(
                        masterDataId = null,
                        manualModelName = state.manualModelName,
                        manualBrand = state.manualBrand,
                        manualYear = state.manualYear.toIntOrNull(),
                        manualSeries = state.manualSeries,
                        manualSeriesNum = state.manualSeriesNum,
                        manualScale = state.manualScale,
                        manualIsPremium = state.manualIsPremium,
                        condition = state.condition,
                        purchaseDate = state.purchaseDate,
                        personalNote = state.personalNote,
                        storageLocation = state.storageLocation,
                        isWishlist = isWishlist,
                        userPhotoUrl = state.userPhotoUrl,
                        purchasePrice = storedPurchasePrice,
                        estimatedValue = storedEstimatedValue
                    )
                } else {
                    val selectedMaster = state.selectedMasterData!!
                    UserCar(
                        masterDataId = selectedMaster.id,
                        manualModelName = selectedMaster.modelName,
                        manualBrand = selectedMaster.brand,
                        manualYear = selectedMaster.year,
                        manualSeries = selectedMaster.series.takeIf { it.isNotBlank() },
                        manualSeriesNum = selectedMaster.seriesNum.takeIf { it.isNotBlank() },
                        manualScale = selectedMaster.scale,
                        manualIsPremium = selectedMaster.isPremium,
                        condition = state.condition,
                        purchaseDate = state.purchaseDate,
                        personalNote = state.personalNote,
                        storageLocation = state.storageLocation,
                        isWishlist = isWishlist,
                        userPhotoUrl = state.userPhotoUrl,
                        purchasePrice = storedPurchasePrice,
                        estimatedValue = storedEstimatedValue
                    )
                }

                addCarToCollectionUseCase(carToAdd)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearSaveSuccess() { _uiState.update { it.copy(saveSuccess = false) } }
}
