package com.taytek.basehw.ui.screens.sth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.repository.MasterDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SthViewModel @Inject constructor(
    private val repository: MasterDataRepository
) : ViewModel() {

    enum class SthTab { STH, CHASE, TH }

    private val _currentTab = MutableStateFlow(SthTab.STH)
    val currentTab = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear = _selectedYear.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableYears: StateFlow<List<Int>> = _currentTab
        .flatMapLatest { tab ->
            when (tab) {
                SthTab.STH -> repository.getSthYears()
                SthTab.CHASE -> repository.getChaseYears()
                SthTab.TH -> repository.getThYears()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Auto-select the most recent year when available
        viewModelScope.launch {
            availableYears.filter { it.isNotEmpty() }.firstOrNull()?.let { years ->
                if (_selectedYear.value == null) {
                    // Default to null (All)
                    _selectedYear.value = null
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedYear(year: Int?) {
        _selectedYear.value = year
    }

    fun updateTab(tab: SthTab) {
        if (_currentTab.value != tab) {
            _currentTab.value = tab
            _selectedYear.value = null // Reset year when switching tabs
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val sthCarsPaged: Flow<PagingData<MasterData>> = combine(_searchQuery, _selectedYear) { query, year ->
        Pair(query, year)
    }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { (query, year) ->
            repository.searchSthCarsWithYear(query, year)
        }
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val chaseCarsPaged: Flow<PagingData<MasterData>> = combine(_searchQuery, _selectedYear) { query, year ->
        Pair(query, year)
    }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { (query, year) ->
            repository.searchChaseCars(query, year)
        }
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val thCarsPaged: Flow<PagingData<MasterData>> = combine(_searchQuery, _selectedYear) { query, year ->
        Pair(query, year)
    }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { (query, year) ->
            repository.searchThCars(query, year)
        }
        .cachedIn(viewModelScope)
}
