package com.taytek.basehw.domain.usecase

import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.UserCarRepository
import javax.inject.Inject

class AddCarToCollectionUseCase @Inject constructor(
    private val repository: UserCarRepository
) {
    suspend operator fun invoke(car: UserCar): Long {
        return repository.addCar(car)
    }
}
