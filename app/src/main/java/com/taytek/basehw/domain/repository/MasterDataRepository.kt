package com.taytek.basehw.domain.repository

import androidx.paging.PagingData
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import kotlinx.coroutines.flow.Flow

interface MasterDataRepository {
    fun searchByBrand(brand: Brand, query: String): Flow<PagingData<MasterData>>
    fun getAll(brand: Brand?): Flow<PagingData<MasterData>>
    suspend fun syncFromFandom(brand: Brand)
    suspend fun getCount(): Int
}
