package com.taytek.basehw.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.taytek.basehw.domain.model.SortOrder
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: UserCarRepository
) : ViewModel() {

    private data class CollectionQuery(
        val query: String?,
        val brand: String?,
        val year: Int?,
        val series: String?,
        val condition: String?,
        val sort: SortOrder
    )

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    private val _selectedSeries = MutableStateFlow<String?>(null)
    val selectedSeries: StateFlow<String?> = _selectedSeries.asStateFlow()

    private val _selectedCondition = MutableStateFlow<String?>(null)
    val selectedCondition: StateFlow<String?> = _selectedCondition.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val availableBrands: StateFlow<List<Brand>> = repository.getBrandCounts()
        .map { stats -> stats.map { it.brand }.sortedBy { it.displayName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val carsPaged: Flow<PagingData<UserCar>> = combine(
        combine(
            _searchQuery,
            _selectedBrand,
            _selectedYear,
            _selectedSeries,
            _selectedCondition
        ) { query, brand, year, series, condition ->
            CollectionQuery(
                query = query,
                brand = brand,
                year = year,
                series = series,
                condition = condition,
                sort = SortOrder.DATE_ADDED_DESC
            )
        },
        _sortOrder
    ) { query, sort ->
        query.copy(sort = sort)
    }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            repository.getCollection(q.query, q.brand, q.year, q.series, q.condition, q.sort)
        }
        .cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = if (query.isBlank()) null else query
    }

    fun updateFilters(brand: String?, year: Int?, series: String?, condition: String?, sortOrder: SortOrder) {
        _selectedBrand.value = brand
        _selectedYear.value = year
        _selectedSeries.value = if (series.isNullOrBlank()) null else series
        _selectedCondition.value = condition
        _sortOrder.value = sortOrder
    }

    fun toggleSelection(id: Long) {
        _selectedIds.update { ids ->
            if (ids.contains(id)) ids - id else ids + id
        }
        _isSelectionMode.value = _selectedIds.value.isNotEmpty()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelected() {
        viewModelScope.launch {
            repository.deleteCars(_selectedIds.value.toList())
            clearSelection()
        }
    }
}
