package com.taytek.basehw.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taytek.basehw.domain.model.BadgeType
import com.taytek.basehw.domain.model.BoxStatusStats
import com.taytek.basehw.domain.model.BrandStats
import com.taytek.basehw.domain.model.HwTierStats
import com.taytek.basehw.domain.usecase.GetEarnedBadgesUseCase
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val userCarRepository: UserCarRepository,
    private val getEarnedBadgesUseCase: GetEarnedBadgesUseCase,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager,
    private val currencyRepository: com.taytek.basehw.domain.repository.CurrencyRepository
) : ViewModel() {

    val currencyCode: StateFlow<String> = appSettingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EUR")

    val currencySymbol: StateFlow<String> = currencyCode
        .map { com.taytek.basehw.domain.model.AppCurrency.fromCode(it).symbol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "€")

    private val conversionRate: StateFlow<Double> = combine(currencyRepository.getRates(), currencyCode) { rates, code ->
        val effectiveCode = if (code.isBlank()) "EUR" else code
        if (effectiveCode == "EUR") 1.0
        else rates?.rates?.get(effectiveCode) ?: 1.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    val lastUpdateTime: Flow<String?> = currencyRepository.getRates().map { it?.date }

    val totalCars: StateFlow<Int> = userCarRepository.getTotalCarsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPurchasePrice: StateFlow<Double> = combine(userCarRepository.getTotalPurchasePrice(), conversionRate) { price, rate ->
        price * rate
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalEstimatedValue: StateFlow<Double> = combine(userCarRepository.getTotalEstimatedValue(), conversionRate) { value, rate ->
        value * rate
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val brandStats: StateFlow<List<BrandStats>> = userCarRepository.getBrandCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hwTierStats: StateFlow<HwTierStats> = userCarRepository.getHwTierStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HwTierStats(0, 0))

    val boxStatusStats: StateFlow<List<BoxStatusStats>> = userCarRepository.getBoxStatusCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val earnedBadges: StateFlow<List<BadgeType>> = getEarnedBadgesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            currencyRepository.refreshRates()
        }
    }

    private val startOfMonth: Long by lazy {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val monthlyAddedCount: StateFlow<Int> = userCarRepository.getCarsAddedSinceCount(startOfMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monthlyValueIncrease: StateFlow<Double> = combine(userCarRepository.getValueAddedSince(startOfMonth), conversionRate) { value, rate ->
        value * rate
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private var _internalStatIndex = -1
    private val _heroStatIndexFlow = MutableStateFlow(0)
    val heroStatIndex: StateFlow<Int> = _heroStatIndexFlow.asStateFlow()

    fun advanceHeroStatIndex() {
        _internalStatIndex++
        _heroStatIndexFlow.value = _internalStatIndex % 3
    }
}
