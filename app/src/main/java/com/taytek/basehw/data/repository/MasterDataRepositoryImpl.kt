package com.taytek.basehw.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.mapper.toDomain
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.repository.MasterDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterDataRepositoryImpl @Inject constructor(
    private val dao: MasterDataDao
) : MasterDataRepository {

    override fun searchAll(query: String): Flow<PagingData<MasterData>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false)
        ) {
            dao.searchAll(query)
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

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

    override suspend fun syncFromSource(brand: Brand) {
        // Actual sync is done via RemoteYearSyncWorker (WorkManager)
        // This method is kept for direct use in tests or one-shot triggers
    }

    override suspend fun getBySeries(brand: Brand, series: String): List<MasterData> {
        return dao.getListBySeries(brand.name, series).map { it.toDomain() }
    }

    override suspend fun getCount(): Int = dao.getCount()

    override fun getSthCars(): Flow<PagingData<MasterData>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false)
        ) {
            dao.getSthCars()
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun searchSthCars(query: String): Flow<PagingData<MasterData>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false)
        ) {
            if (query.isBlank()) {
                dao.getSthCars()
            } else {
                dao.searchSthCars(query)
            }
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }
    override fun getSthYears(): Flow<List<Int>> {
        return dao.getSthYears()
    }

    override fun getChaseYears(): Flow<List<Int>> {
        return dao.getChaseYears()
    }

    override fun getThYears(): Flow<List<Int>> {
        return dao.getThYears()
    }

    override fun searchSthCarsWithYear(query: String, year: Int?): Flow<PagingData<MasterData>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false)
        ) {
            if (year == null && query.isBlank()) {
                dao.getSthCars()
            } else if (year == null) {
                dao.searchSthCars(query)
            } else {
                dao.searchSthCarsWithYear(query, year)
            }
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun searchChaseCars(query: String, year: Int?): Flow<PagingData<MasterData>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false)
        ) {
            if (year == null && query.isBlank()) {
                dao.getChaseCars()
            } else if (year == null) {
                dao.searchChaseCarsWithYear(query, null)
            } else {
                dao.searchChaseCarsWithYear(query, year)
            }
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun searchThCars(query: String, year: Int?): Flow<PagingData<MasterData>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false)
        ) {
            if (year == null && query.isBlank()) {
                dao.getThCars()
            } else if (year == null) {
                dao.searchThCarsWithYear(query, null)
            } else {
                dao.searchThCarsWithYear(query, year)
            }
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun getMasterDataById(id: Long): MasterData? {
        return dao.getById(id)?.toDomain()
    }
}
