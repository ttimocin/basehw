package com.taytek.basehw.domain.usecase

import androidx.paging.PagingData
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.repository.MasterDataRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchAllMasterDataUseCase @Inject constructor(
    private val repository: MasterDataRepository
) {
    operator fun invoke(query: String): Flow<PagingData<MasterData>> {
        return repository.searchAll(query)
    }
}
