package com.taytek.basehw.domain.usecase

import androidx.paging.PagingData
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.UserCarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import com.taytek.basehw.domain.model.SortOrder

class GetUserCollectionUseCase @Inject constructor(
    private val repository: UserCarRepository
) {
    operator fun invoke(
        query: String? = null,
        brand: String? = null,
        year: Int? = null,
        series: String? = null,
        isOpened: Boolean? = null,
        sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC
    ): Flow<PagingData<UserCar>> {
        return repository.getCollection(query, brand, year, series, isOpened, sortOrder)
    }
}
