package com.taytek.basehw.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.mapper.toDomain
import com.taytek.basehw.data.remote.api.MediaWikiApiService
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.repository.MasterDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterDataRepositoryImpl @Inject constructor(
    private val dao: MasterDataDao,
    private val apiService: MediaWikiApiService
) : MasterDataRepository {

    override fun searchByBrand(brand: Brand, query: String): Flow<PagingData<MasterData>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false)
        ) {
            if (query.isBlank()) {
                dao.getAllByBrand(brand.name)
            } else {
                dao.searchByBrand(brand.name, query)
            }
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getAll(brand: Brand?): Flow<PagingData<MasterData>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false)
        ) {
            if (brand != null) {
                dao.getAllByBrand(brand.name)
            } else {
                dao.getAll()
            }
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun syncFromFandom(brand: Brand) {
        // Actual sync is done via FandomSyncWorker (WorkManager)
        // This method is kept for direct use in tests or one-shot triggers
    }

    override suspend fun getCount(): Int = dao.getCount()
}
