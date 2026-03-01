package com.taytek.basehw.domain.usecase

import androidx.paging.PagingData
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.repository.MasterDataRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchMasterDataUseCase @Inject constructor(
    private val repository: MasterDataRepository
) {
    operator fun invoke(brand: Brand, query: String): Flow<PagingData<MasterData>> {
        return repository.searchByBrand(brand, query)
    }
}
