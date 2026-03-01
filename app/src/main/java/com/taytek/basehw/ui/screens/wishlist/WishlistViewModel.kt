package com.taytek.basehw.ui.screens.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    repository: UserCarRepository
) : ViewModel() {

    // Same behavior as CollectionViewModel, but uses the getWishlist() repository flow
    val wishlistPaged: Flow<PagingData<UserCar>> = repository.getWishlist()
        .cachedIn(viewModelScope)
}
