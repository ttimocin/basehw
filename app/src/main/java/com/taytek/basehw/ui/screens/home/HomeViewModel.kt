package com.taytek.basehw.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.auth.FirebaseUser
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.DiecastNews
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.model.User
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.AuthRepository
import com.taytek.basehw.domain.repository.NewsRepository
import com.taytek.basehw.domain.repository.UserCarRepository
import com.taytek.basehw.domain.usecase.SearchAllMasterDataUseCase
import com.taytek.basehw.domain.usecase.SearchMasterDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import com.taytek.basehw.data.local.AppSettingsManager
import com.taytek.basehw.domain.repository.CurrencyRepository
import com.taytek.basehw.domain.model.AppCurrency

data class HomeUiState(
    val userName: String = "Collector",
    val profilePhotoUrl: String? = null,
    val totalCars: Int = 0,
    val monthlyAdded: Int = 0,
    val wantedCount: Int = 0,
    val sthCount: Int = 0,
    val totalValue: Double = 0.0,
    val monthlyValueIncrease: Double = 0.0,
    val searchQuery: String = "",
    val currencySymbol: String = "€",
    val showRestorePrompt: Boolean = false,
    val isRestoring: Boolean = false,
    val isCloudCheckInProgress: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userCarRepository: UserCarRepository,
    private val newsRepository: NewsRepository,
    private val searchMasterDataUseCase: SearchMasterDataUseCase,
    private val searchAllMasterDataUseCase: SearchAllMasterDataUseCase,
    private val appSettingsManager: AppSettingsManager,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _newsItems = MutableStateFlow<List<DiecastNews>>(emptyList())
    val newsItems: StateFlow<List<DiecastNews>> = _newsItems.asStateFlow()

    private var hasCheckedForBackup = false
    private var lastCheckedBackupUid: String? = null
    private var backupCheckJob: Job? = null

    val currencyCode: StateFlow<String> = appSettingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EUR")

    val currencySymbol: StateFlow<String> = currencyCode
        .map { AppCurrency.fromCode(it).symbol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "€")

    private val conversionRate: StateFlow<Double> = combine(currencyRepository.getRates(), currencyCode) { rates, code ->
        val effectiveCode = if (code.isBlank()) "EUR" else code
        if (effectiveCode == "EUR") 1.0
        else rates?.rates?.get(effectiveCode) ?: 1.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

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
        loadDiecastNews()

        viewModelScope.launch {
            currencyRepository.refreshRates()
        }
    }

    private fun loadDiecastNews() {
        viewModelScope.launch {
            newsRepository.getLatest(NewsRepository.HOME_SCREEN_NEWS_LIMIT).onSuccess { list ->
                _newsItems.value = list
            }
        }
    }

    private fun observeUserData() {
        viewModelScope.launch {
            authRepository.currentUserFlow.collect { firebaseUser ->
                if (firebaseUser != null) {
                    loadUserPresentation(firebaseUser)

                    scheduleBackupCheck(firebaseUser.uid)
                    
                    // Profile data loaded
                } else {
                    backupCheckJob?.cancel()
                    backupCheckJob = null
                    lastCheckedBackupUid = null
                    hasCheckedForBackup = false
                    _uiState.update { it.copy(showRestorePrompt = false) }
                }
            }
        }
    }

    fun refreshUserData() {
        loadDiecastNews()
        val firebaseUser = authRepository.currentUser ?: return
        viewModelScope.launch {
            loadUserPresentation(firebaseUser)
        }
    }

    private suspend fun loadUserPresentation(firebaseUser: FirebaseUser) {
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

    private fun scheduleBackupCheck(uid: String) {
        if (hasCheckedForBackup && lastCheckedBackupUid == uid) return
        backupCheckJob?.cancel()
        backupCheckJob = viewModelScope.launch {
            _uiState.update { it.copy(isCloudCheckInProgress = true) }
            val totalCarsFlow = userCarRepository.getTotalCarsCount()
            val totalCars = totalCarsFlow.first()
            
            if (totalCars == 0) {
                val hasCloud = userCarRepository.hasCloudData()
                if (hasCloud) {
                    restoreFromCloud()
                }
            }
            hasCheckedForBackup = true
            lastCheckedBackupUid = uid
            _uiState.update { it.copy(isCloudCheckInProgress = false) }
        }
    }

    fun dismissRestorePrompt() {
        _uiState.update { it.copy(showRestorePrompt = false) }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(showRestorePrompt = false, isRestoring = true) }
            try {
                userCarRepository.mergeFromSupabase()
            } catch (e: Exception) {
                // Log or handle error if needed
            } finally {
                _uiState.update { it.copy(isRestoring = false) }
            }
        }
    }

    private fun observeStats() {
        data class BaseStats(
            val total: Int,
            val monthly: Int,
            val wanted: Int,
            val sth: Int,
            val rawValue: Double
        )

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        viewModelScope.launch {
            combine(
                combine(
                    userCarRepository.getTotalCarsCount(),
                    userCarRepository.getCarsAddedSinceCount(startOfMonth),
                    userCarRepository.getWantedNotInCollectionCount(),
                    userCarRepository.getSthCarsCount(),
                    userCarRepository.getTotalEstimatedValue()
                ) { total, monthly, wanted, sth, rawValue ->
                    BaseStats(total, monthly, wanted, sth, rawValue)
                },
                userCarRepository.getValueAddedSince(startOfMonth),
                conversionRate,
                currencySymbol
            ) { base, rawIncrease, rate, symbol ->

                _uiState.update { 
                    it.copy(
                        totalCars = base.total,
                        monthlyAdded = base.monthly,
                        wantedCount = base.wanted,
                        sthCount = base.sth,
                        totalValue = base.rawValue * rate,
                        monthlyValueIncrease = rawIncrease * rate,
                        currencySymbol = symbol
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

    fun getHomeStatsPagerInitialPage(): Int = appSettingsManager.getHomeStatsPagerPage()

    fun onHomeStatsPagerPageChanged(page: Int) {
        appSettingsManager.setHomeStatsPagerPage(page)
    }
}
