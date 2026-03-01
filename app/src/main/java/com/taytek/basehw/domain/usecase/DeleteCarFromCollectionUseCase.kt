package com.taytek.basehw.domain.usecase

import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.UserCarRepository
import javax.inject.Inject

class DeleteCarFromCollectionUseCase @Inject constructor(
    private val repository: UserCarRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteCar(id)
    }
}
