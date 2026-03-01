package com.taytek.basehw.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.usecase.GetUserCollectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    getUserCollectionUseCase: GetUserCollectionUseCase
) : ViewModel() {

    val carsPaged: Flow<PagingData<UserCar>> = getUserCollectionUseCase()
        .cachedIn(viewModelScope)
}
