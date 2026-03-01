package com.taytek.basehw.domain.usecase

import androidx.paging.PagingData
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.UserCarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserCollectionUseCase @Inject constructor(
    private val repository: UserCarRepository
) {
    operator fun invoke(): Flow<PagingData<UserCar>> {
        return repository.getCollection()
    }
}
