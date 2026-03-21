package com.taytek.basehw.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.model.User
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.AuthRepository
import com.taytek.basehw.domain.repository.UserCarRepository
import com.taytek.basehw.domain.usecase.SearchAllMasterDataUseCase
import com.taytek.basehw.domain.usecase.SearchMasterDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "Collector",
    val profilePhotoUrl: String? = null,
    val totalCars: Int = 0,
    val monthlyAdded: Int = 0,
    val wantedCount: Int = 0,
    val sthCount: Int = 0,
    val totalValue: Double = 0.0,
    val monthlyValueIncrease: Double = 0.0,
    val searchQuery: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userCarRepository: UserCarRepository,
    private val searchMasterDataUseCase: SearchMasterDataUseCase,
    private val searchAllMasterDataUseCase: SearchAllMasterDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBrand = MutableStateFlow<Brand?>(null)
    val selectedBrand: StateFlow<Brand?> = _selectedBrand.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val masterSearchResults: Flow<PagingData<MasterData>> = combine(
        _searchQuery
            .debounce(300L)
            .distinctUntilChanged(),
        _selectedBrand
    ) { query, brand -> query to brand }
        .flatMapLatest { (query, brand) ->
            when {
                query.isBlank() -> flowOf(PagingData.empty())
                brand != null -> searchMasterDataUseCase(brand, query)
                else -> searchAllMasterDataUseCase(query)
            }
        }
        .cachedIn(viewModelScope)

    val recentlyAddedCars: Flow<PagingData<UserCar>> = userCarRepository.getCollectionRecentlyAdded()
        .cachedIn(viewModelScope)

    init {
        observeUserData()
        observeStats()
    }

    private fun observeUserData() {
        viewModelScope.launch {
            authRepository.currentUserFlow.collect { firebaseUser ->
                if (firebaseUser != null) {
                    val profileResult = authRepository.getUserProfile()
                    profileResult.onSuccess { user ->
                        _uiState.update {
                            it.copy(
                                userName = user.username ?: user.email.substringBefore("@"),
                                profilePhotoUrl = user.photoUrl ?: firebaseUser.photoUrl?.toString()
                            )
                        }
                    }.onFailure {
                        _uiState.update {
                            it.copy(
                                userName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Collector",
                                profilePhotoUrl = firebaseUser.photoUrl?.toString()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeStats() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        viewModelScope.launch {
            combine(
                userCarRepository.getTotalCarsCount(),
                userCarRepository.getCarsAddedSinceCount(startOfMonth),
                userCarRepository.getWantedNotInCollectionCount(),
                userCarRepository.getSthCarsCount(),
                userCarRepository.getTotalEstimatedValue(),
                userCarRepository.getValueAddedSince(startOfMonth)
            ) { args ->
                val total = args[0] as Int
                val monthly = args[1] as Int
                val wanted = args[2] as Int
                val sth = args[3] as Int
                val value = args[4] as Double
                val valueIncrease = args[5] as Double

                _uiState.update { 
                    it.copy(
                        totalCars = total,
                        monthlyAdded = monthly,
                        wantedCount = wanted,
                        sthCount = sth,
                        totalValue = value,
                        monthlyValueIncrease = valueIncrease
                    )
                }
            }.collect()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
        _uiState.update { it.copy(searchQuery = "") }
    }

    fun updateSelectedBrand(brand: Brand?) {
        _selectedBrand.value = brand
    }
}
