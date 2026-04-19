package com.taytek.basehw.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taytek.basehw.domain.model.BoxStatusStats
import com.taytek.basehw.domain.model.BrandStats
import com.taytek.basehw.domain.model.HwTierStats
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class ValuePoint(
    val timestamp: Long,
    val cumulativeValue: Double,
    val label: String
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val userCarRepository: UserCarRepository,
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

    val customStats: StateFlow<com.taytek.basehw.domain.model.CustomStats> = userCarRepository.getCustomStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.taytek.basehw.domain.model.CustomStats(0, 0))

    val collectionHistory: StateFlow<List<ValuePoint>> = combine(
        userCarRepository.getAllCarsWithMasterListFlow(),
        conversionRate
    ) { cars, rate ->
        val filteredCars = cars.filter { !it.isWishlist }
        if (filteredCars.isEmpty()) return@combine emptyList()

        // Group by month to keep the chart smooth
        val monthFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
        
        val pointsByMonth = filteredCars
            .map { car ->
                val date = car.purchaseDate ?: Date(0) // Fallback for items without date
                val value = (car.estimatedValue ?: car.purchasePrice ?: 0.0) * car.quantity * rate
                date to value
            }
            .groupBy { monthFormat.format(it.first) }
            .mapValues { entry -> entry.value.sumOf { it.second } }

        // Sort months chronologically
        // To sort "MMM yy" correctly, we need to parse them back or use a better key
        val sortedMonths = pointsByMonth.keys.sortedBy { monthStr ->
            try { monthFormat.parse(monthStr)?.time ?: 0L } catch (e: Exception) { 0L }
        }

        var runningTotal = 0.0
        val historyPoints = sortedMonths.map { monthStr ->
            runningTotal += pointsByMonth[monthStr] ?: 0.0
            ValuePoint(
                timestamp = monthFormat.parse(monthStr)?.time ?: 0L,
                cumulativeValue = runningTotal,
                label = monthStr
            )
        }

        // Prepend a zero point if there's only one month or at the start of the collection
        if (historyPoints.size == 1) {
            val firstPoint = historyPoints.first()
            val startPoint = ValuePoint(
                timestamp = firstPoint.timestamp - (30L * 24 * 60 * 60 * 1000), // One month before
                cumulativeValue = 0.0,
                label = "" 
            )
            listOf(startPoint, firstPoint)
        } else {
            historyPoints
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            currencyRepository.refreshRates()
        }
        
        // Auto-advance hero stat every 10 seconds
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000)
                advanceHeroStatIndex()
            }
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

    private var _internalStatIndex = 0
    private val _heroStatIndexFlow = MutableStateFlow(0)
    val heroStatIndex: StateFlow<Int> = _heroStatIndexFlow.asStateFlow()

    fun advanceHeroStatIndex() {
        _internalStatIndex++
        _heroStatIndexFlow.value = _internalStatIndex % 4
    }
}
