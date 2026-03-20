package com.taytek.basehw.domain.usecase

import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.repository.MasterDataRepository
import javax.inject.Inject

class GetMasterDataByIdUseCase @Inject constructor(
    private val repository: MasterDataRepository
) {
    suspend operator fun invoke(id: Long): MasterData? {
        return repository.getMasterDataById(id)
    }
}
