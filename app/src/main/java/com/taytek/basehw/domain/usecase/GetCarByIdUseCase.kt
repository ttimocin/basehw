package com.taytek.basehw.domain.usecase

import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.UserCarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCarByIdUseCase @Inject constructor(
    private val repository: UserCarRepository
) {
    operator fun invoke(id: Long): Flow<UserCar?> {
        return repository.getCarById(id)
    }
}
