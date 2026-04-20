package com.taytek.basehw.ui.screens.addcar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.HwCardType
import com.taytek.basehw.domain.model.HwCardTypeRules
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.model.CurrencyRates
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.model.VehicleCondition
import com.taytek.basehw.domain.usecase.AddCarToCollectionUseCase
import com.taytek.basehw.domain.usecase.GetMasterDataByIdUseCase
import com.taytek.basehw.domain.usecase.SearchMasterDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

data class AddCarUiState(
    val selectedBrand: Brand = Brand.HOT_WHEELS,
    val searchQuery: String = "",
    val selectedMasterData: MasterData? = null,
    val isManualMode: Boolean = false,
    val manualModelName: String = "",
    val manualBrand: Brand = Brand.HOT_WHEELS,
    val manualYear: String = "",
    val manualSeries: String = "",
    val manualSeriesNum: String = "",
    val manualScale: String = "1:64",
    val manualIsPremium: Boolean = false,
    val condition: VehicleCondition = VehicleCondition.MINT,
    val purchaseDate: Date? = Date(),
    val personalNote: String = "",
    val storageLocation: String = "",
    val userPhotoUrl: String? = null,
    val purchasePrice: String = "",
    val estimatedValue: String = "",
    val selectedCurrency: com.taytek.basehw.domain.model.AppCurrency? = null,
    val isCustom: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isPhotoOptionMenuVisible: Boolean = false,
    val isUrlInputDialogVisible: Boolean = false,
    val ocrHintMessage: String? = null,
    val deleteId: Long? = null,
    val isAiAnalysing: Boolean = false,
    val isAiBackgroundAnalysing: Boolean = false,
    val aiAnalysisResult: com.taytek.basehw.domain.util.VisionAnalysisResult? = null,
    val error: String? = null,
    val hwCardType: HwCardType = HwCardType.SHORT
)

@HiltViewModel
class AddCarViewModel @Inject constructor(
    private val searchMasterDataUseCase: SearchMasterDataUseCase,
    private val getMasterDataByIdUseCase: GetMasterDataByIdUseCase,
    private val addCarToCollectionUseCase: AddCarToCollectionUseCase,
    private val userCarRepository: com.taytek.basehw.domain.repository.UserCarRepository,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager,
    private val currencyRepository: com.taytek.basehw.domain.repository.CurrencyRepository,
    private val openAiVisionHelper: com.taytek.basehw.domain.util.OpenAiVisionHelper
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
            val code = appSettingsManager.currencyFlow.first()
            _uiState.update {
                it.copy(selectedCurrency = com.taytek.basehw.domain.model.AppCurrency.fromCode(code))
            }
        }
    }

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
        _uiState.update { it.copy(selectedBrand = brand, selectedMasterData = null, searchQuery = "", ocrHintMessage = null) }
        _searchTrigger.value = Pair(brand, "")
    }

    fun onCurrencySelected(currency: com.taytek.basehw.domain.model.AppCurrency) {
        viewModelScope.launch {
            val state = _uiState.value
            val oldCurrency = state.selectedCurrency
                ?: com.taytek.basehw.domain.model.AppCurrency.fromCode(appSettingsManager.currencyFlow.value)
            if (oldCurrency == currency) return@launch

            val rates = currencyRepository.getRates().filterNotNull().first()
            fun rateToEur(code: String): Double =
                if (code == "EUR") 1.0 else rates.rates[code] ?: 1.0

            fun convertAmount(amountStr: String, from: com.taytek.basehw.domain.model.AppCurrency, to: com.taytek.basehw.domain.model.AppCurrency): String {
                val amount = parsePrice(amountStr) ?: return amountStr
                val amountEur = amount / rateToEur(from.code)
                val converted = amountEur * rateToEur(to.code)
                return formatPriceForInput(converted)
            }

            val newPurchase =
                if (state.purchasePrice.isBlank()) "" else convertAmount(state.purchasePrice, oldCurrency, currency)
            val newEstimated =
                if (state.estimatedValue.isBlank()) "" else convertAmount(state.estimatedValue, oldCurrency, currency)

            _uiState.update {
                it.copy(
                    selectedCurrency = currency,
                    purchasePrice = newPurchase,
                    estimatedValue = newEstimated
                )
            }
        }
    }

    private fun formatPriceForInput(value: Double): String {
        val rounded = kotlin.math.round(value * 100.0) / 100.0
        return if (abs(rounded - rounded.toLong()) < 1e-9) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, selectedMasterData = null, ocrHintMessage = null) }
        _searchTrigger.value = Pair(_uiState.value.selectedBrand, query)
    }

    fun applyDetectedModelQuery(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        _uiState.update {
            it.copy(
                isManualMode = false,
                selectedMasterData = null,
                searchQuery = normalized,
                ocrHintMessage = null
            )
        }
        _searchTrigger.value = Pair(_uiState.value.selectedBrand, normalized)
    }

    fun applyDetectedCameraRecognition(query: String?, detectedBrand: Brand?) {
        val normalized = query?.trim().orEmpty()
        if (normalized.isBlank() && detectedBrand == null) {
            _uiState.update {
                it.copy(
                    ocrHintMessage = "Kutusuz arac veya okunabilir model metni bulunamadi. Lutfen modeli listeden secin ya da manuel girin."
                )
            }
            return
        }

        val brandToUse = detectedBrand ?: _uiState.value.selectedBrand
        _uiState.update {
            it.copy(
                selectedBrand = brandToUse,
                isManualMode = false,
                selectedMasterData = null,
                searchQuery = normalized,
                ocrHintMessage = null
            )
        }
        _searchTrigger.value = Pair(brandToUse, normalized)
    }

    fun clearOcrHintMessage() {
        _uiState.update { it.copy(ocrHintMessage = null) }
    }

    fun loadMasterDataById(id: Long) {
        viewModelScope.launch {
            getMasterDataByIdUseCase(id)?.let { master ->
                _uiState.update {
                    it.copy(
                        selectedMasterData = master,
                        searchQuery = master.modelName,
                        selectedBrand = master.brand
                    )
                }
            }
        }
    }

    fun setDeleteId(id: Long) {
        _uiState.update { it.copy(deleteId = id) }
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

    fun onHwCardTypeChanged(type: HwCardType) {
        _uiState.update { it.copy(hwCardType = type) }
    }

    private fun shouldPersistHwCardType(state: AddCarUiState): Boolean =
        HwCardTypeRules.showForManual(state.isManualMode, state.manualBrand) ||
            (!state.isManualMode && state.selectedMasterData?.let { HwCardTypeRules.showForMaster(it) } == true)

    fun onIsCustomChanged(isCustom: Boolean) {
        _uiState.update { it.copy(isCustom = isCustom) }
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

    fun onAnalyzeImage(base64Image: String, isBackground: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { 
                if (isBackground) it.copy(isAiBackgroundAnalysing = true, error = null)
                else it.copy(isAiAnalysing = true, error = null) 
            }
            val result = openAiVisionHelper.analyzeVehicleImage(base64Image)
            result.onSuccess { analysis ->
                _uiState.update { state ->
                    val detectedBrand = Brand.entries.find { 
                        it.displayName.contains(analysis.brand ?: "", ignoreCase = true) || 
                        (analysis.brand ?: "").contains(it.name, ignoreCase = true)
                    } ?: state.selectedBrand

                    val updateSearch = state.selectedMasterData == null

                    // Apply to manual fields or search query
                    state.copy(
                        isAiAnalysing = false,
                        isAiBackgroundAnalysing = false,
                        aiAnalysisResult = analysis,
                        condition = VehicleCondition.fromString(analysis.condition),
                        // If user hasn't selected a car, auto-fill text fields
                        manualModelName = if (updateSearch) (analysis.model ?: state.manualModelName) else state.manualModelName,
                        manualBrand = if (updateSearch) detectedBrand else state.manualBrand,
                        manualSeries = if (updateSearch) (analysis.series ?: state.manualSeries) else state.manualSeries,
                        manualYear = if (updateSearch) (analysis.year ?: state.manualYear) else state.manualYear,
                        personalNote = analysis.conditionNote ?: state.personalNote,
                        estimatedValue = if (updateSearch && state.estimatedValue.isBlank()) (analysis.estimatedValue?.toString() ?: state.estimatedValue) else state.estimatedValue,
                        // If in search mode, update query
                        searchQuery = if (updateSearch) (analysis.model ?: state.searchQuery) else state.searchQuery,
                        selectedBrand = if (updateSearch) detectedBrand else state.selectedBrand
                    )
                }
                
                // If not in manual mode and no car selected, trigger a search with the AI-detected model name
                if (!_uiState.value.isManualMode && analysis.model != null && _uiState.value.selectedMasterData == null) {
                    _searchTrigger.value = Pair(_uiState.value.selectedBrand, analysis.model)
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isAiAnalysing = false,
                        isAiBackgroundAnalysing = false,
                        error = e.localizedMessage ?: "Analysis failed"
                    )
                }
            }
        }
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

    fun addCarToCollection() {
        saveCarToDatabase()
    }

    private fun parsePrice(price: String): Double? {
        if (price.isBlank()) return null
        return try {
            // Normalize: replace comma with dot
            price.replace(",", ".").toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun saveCarToDatabase() {
        val state = _uiState.value
        if (!state.isManualMode && state.selectedMasterData == null) return
        if (state.isManualMode && state.manualModelName.isBlank()) {
            _uiState.update { it.copy(error = "Model ismi boş bırakılamaz.") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                // Fetch live rates safely before computing saving multiplier, if not already cached
                // Fetch live rates safely before computing saving multiplier. 
                // Repository is initialized from persistent cache, so this is usually instant.
                val rates = currencyRepository.getRates().filterNotNull().first()

                // Use the rate of the selected currency in the dropdown, NOT the global setting.
                val selectedCode = state.selectedCurrency?.code ?: "EUR"
                val rate = if (selectedCode == "EUR") 1.0 else rates.rates[selectedCode] ?: 1.0
                
                // EUR = Input / Rate
                val storedPurchasePrice = parsePrice(state.purchasePrice)?.let { it / rate }
                val storedEstimatedValue = parsePrice(state.estimatedValue)?.let { it / rate }

                val cardTypeToSave = if (shouldPersistHwCardType(state)) state.hwCardType else null

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
                        userPhotoUrl = state.userPhotoUrl,
                        purchasePrice = storedPurchasePrice,
                        estimatedValue = storedEstimatedValue,
                        isCustom = state.isCustom,
                        isWishlist = false,
                        hwCardType = cardTypeToSave
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
                        isWishlist = false,
                        userPhotoUrl = state.userPhotoUrl,
                        purchasePrice = storedPurchasePrice,
                        estimatedValue = storedEstimatedValue,
                        isCustom = state.isCustom,
                        hwCardType = cardTypeToSave
                    )
                }

                addCarToCollectionUseCase(carToAdd)
                
                // If there's a deleteId (moving from wishlist), delete the old record
                state.deleteId?.let { idToRemove ->
                    if (idToRemove != -1L) {
                        userCarRepository.deleteCar(idToRemove)
                    }
                }

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearSaveSuccess() { _uiState.update { it.copy(saveSuccess = false) } }
}
