package com.taytek.basehw.domain.usecase

import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.repository.MasterDataRepository
import javax.inject.Inject

class SyncMasterDataUseCase @Inject constructor(
    private val repository: MasterDataRepository
) {
    suspend operator fun invoke(brand: Brand) {
        repository.syncFromFandom(brand)
    }
}
