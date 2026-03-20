package com.taytek.basehw.domain.repository

import androidx.paging.PagingData
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.BoxStatusStats
import com.taytek.basehw.domain.model.BrandStats
import com.taytek.basehw.domain.model.HwTierStats
import kotlinx.coroutines.flow.Flow

import com.taytek.basehw.domain.model.SortOrder

interface UserCarRepository {
    fun getCollection(
        query: String? = null,
        brand: String? = null,
        year: Int? = null,
        series: String? = null,
        isOpened: Boolean? = null,
        sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC
    ): Flow<PagingData<UserCar>>
    fun getCollectionRecentlyAdded(): Flow<PagingData<UserCar>>
    fun getWishlist(query: String? = null): Flow<PagingData<UserCar>>
    suspend fun addCar(car: UserCar): Long
    suspend fun deleteCar(id: Long)
    suspend fun deleteCars(ids: List<Long>)
    suspend fun updateCar(car: UserCar)
    suspend fun syncToFirestore()
    suspend fun syncFromFirestore()
    suspend fun deleteCloudData(): Result<Unit>
    suspend fun clearLocalData()
    fun getCarById(id: Long): Flow<UserCar?>

    fun getTotalCarsCount(): Flow<Int>
    fun getWishlistCount(): Flow<Int>
    fun getWantedNotInCollectionCount(): Flow<Int>
    fun getSthCarsCount(): Flow<Int>
    fun getBoxStatusCounts(): Flow<List<BoxStatusStats>>
    fun getBrandCounts(): Flow<List<BrandStats>>
    fun getHwTierStats(): Flow<HwTierStats>
    fun getTotalPurchasePrice(): Flow<Double>
    fun getTotalEstimatedValue(): Flow<Double>
    fun getCarsAddedSinceCount(startOfMonth: Long): Flow<Int>
    fun getValueAddedSince(startOfMonth: Long): Flow<Double>
    suspend fun addSeriesToWishlist(brand: Brand, series: String, year: Int?)
    suspend fun deleteWishlistSeries(brand: Brand, seriesName: String)
    suspend fun isSeriesInWishlist(brand: Brand, series: String): Boolean
    fun getWishlistSeriesTracking(): Flow<List<com.taytek.basehw.domain.model.SeriesTracking>>
    suspend fun getAllCarsWithMasterList(): List<UserCar>
}
