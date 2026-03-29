package com.taytek.basehw.ui.screens.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class WishlistViewModel @Inject constructor(
    private val repository: UserCarRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _isSeriesView = MutableStateFlow(false)
    val isSeriesView: StateFlow<Boolean> = _isSeriesView.asStateFlow()

    fun toggleView() {
        _isSeriesView.value = !_isSeriesView.value
        clearSelection()
    }

    fun setSeriesView(isSeries: Boolean) {
        _isSeriesView.value = isSeries
        clearSelection()
    }

    val wishlistPaged: Flow<PagingData<UserCar>> = _searchQuery
        .flatMapLatest { query ->
            repository.getWishlist(query.takeIf { it.isNotBlank() })
        }
        .cachedIn(viewModelScope)

    val seriesTracking: StateFlow<List<com.taytek.basehw.domain.model.SeriesTracking>> = repository.getWishlistSeriesTracking()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Selection State ──
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // For individual car selection (car.id)
    private val _selectedCarIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCarIds: StateFlow<Set<Long>> = _selectedCarIds.asStateFlow()

    // For series selection (brand.name to seriesName)
    private val _selectedSeriesKeys = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
    val selectedSeriesKeys: StateFlow<Set<Pair<String, String>>> = _selectedSeriesKeys.asStateFlow()

    val selectionCount: StateFlow<Int> = kotlinx.coroutines.flow.combine(
        _selectedCarIds, _selectedSeriesKeys
    ) { cars, series -> cars.size + series.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleCarSelection(id: Long) {
        _selectedCarIds.update { ids -> if (ids.contains(id)) ids - id else ids + id }
        _selectedSeriesKeys.value = emptySet()
        _isSelectionMode.value = _selectedCarIds.value.isNotEmpty()
    }

    fun toggleSeriesSelection(brand: Brand, seriesName: String) {
        val key = brand.name to seriesName
        _selectedSeriesKeys.update { keys -> if (keys.contains(key)) keys - key else keys + key }
        _selectedCarIds.value = emptySet()
        _isSelectionMode.value = _selectedSeriesKeys.value.isNotEmpty()
    }

    fun clearSelection() {
        _selectedCarIds.value = emptySet()
        _selectedSeriesKeys.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelected() {
        viewModelScope.launch {
            if (_selectedCarIds.value.isNotEmpty()) {
                repository.deleteCars(_selectedCarIds.value.toList())
            }
            _selectedSeriesKeys.value.forEach { (brandName, seriesName) ->
                val brand = Brand.valueOf(brandName)
                repository.deleteWishlistSeries(brand, seriesName)
            }
            clearSelection()
        }
    }
}
