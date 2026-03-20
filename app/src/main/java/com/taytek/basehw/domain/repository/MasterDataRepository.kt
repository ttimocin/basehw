package com.taytek.basehw.domain.repository

import androidx.paging.PagingData
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import kotlinx.coroutines.flow.Flow

interface MasterDataRepository {
    fun searchAll(query: String): Flow<PagingData<MasterData>>
    fun searchByBrand(brand: Brand, query: String): Flow<PagingData<MasterData>>
    fun getAll(brand: Brand?): Flow<PagingData<MasterData>>
    suspend fun syncFromFandom(brand: Brand)
    suspend fun getBySeries(brand: Brand, series: String): List<MasterData>
    suspend fun getCount(): Int
    fun getSthCars(): Flow<PagingData<MasterData>>
    fun searchSthCars(query: String): Flow<PagingData<MasterData>>
    fun getSthYears(): Flow<List<Int>>
    fun getChaseYears(): Flow<List<Int>>
    fun getThYears(): Flow<List<Int>>
    fun searchSthCarsWithYear(query: String, year: Int?): Flow<PagingData<MasterData>>
    fun searchChaseCars(query: String, year: Int?): Flow<PagingData<MasterData>>
    fun searchThCars(query: String, year: Int?): Flow<PagingData<MasterData>>
    suspend fun getMasterDataById(id: Long): MasterData?
}
