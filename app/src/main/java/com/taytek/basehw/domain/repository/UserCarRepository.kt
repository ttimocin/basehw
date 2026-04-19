package com.taytek.basehw.domain.repository

import androidx.paging.PagingData
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.BoxStatusStats
import com.taytek.basehw.domain.model.BrandStats
import com.taytek.basehw.domain.model.HwTierStats
import com.taytek.basehw.domain.model.RankCarInput
import kotlinx.coroutines.flow.Flow

import com.taytek.basehw.domain.model.SortOrder
import com.taytek.basehw.domain.model.CollectionImportMode
import com.taytek.basehw.domain.model.CollectionImportStats
import com.taytek.basehw.domain.model.VariantHuntGroupSummary
import com.taytek.basehw.domain.model.VariantHuntMasterRow
import java.io.InputStream

interface UserCarRepository {
    fun getCollection(
        query: String? = null,
        brand: String? = null,
        year: Int? = null,
        series: String? = null,
        condition: String? = null,
        sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC
    ): Flow<PagingData<UserCar>>
    fun getCollectionRecentlyAdded(): Flow<PagingData<UserCar>>
    fun getWishlist(query: String? = null): Flow<PagingData<UserCar>>
    suspend fun addCar(car: UserCar): Long
    suspend fun deleteCar(id: Long)
    suspend fun deleteCars(ids: List<Long>)
    suspend fun updateCar(car: UserCar)
    fun triggerSync()
    suspend fun syncToSupabase()
    suspend fun syncFromSupabase()
    suspend fun mergeFromSupabase()
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
    fun getCustomStats(): Flow<com.taytek.basehw.domain.model.CustomStats>
    suspend fun addSeriesToWishlist(brand: Brand, series: String, year: Int?)
    suspend fun deleteWishlistSeries(brand: Brand, seriesName: String)
    suspend fun isSeriesInWishlist(brand: Brand, series: String): Boolean
    fun getWishlistSeriesTracking(): Flow<List<com.taytek.basehw.domain.model.SeriesTracking>>
    suspend fun getAllCarsWithMasterList(): List<UserCar>
    fun getAllCarsWithMasterListFlow(): Flow<List<UserCar>>
    fun getRankCars(): Flow<List<RankCarInput>>
    suspend fun hasCloudData(): Boolean
    suspend fun clearAllCars(): Result<Unit>

    /**
     * JSON / CSV / PDF dışa aktarım dosyasından metin alanlarıyla koleksiyon içe aktarır (foto yok).
     */
    suspend fun importCollection(
        inputStream: InputStream,
        mimeTypeHint: String?,
        mode: CollectionImportMode,
        conversionRate: Double
    ): Result<CollectionImportStats>

    fun observeActiveVariantHuntGroups(): Flow<List<VariantHuntGroupSummary>>
    fun observeVariantHuntGroupRows(groupId: Long): Flow<List<VariantHuntMasterRow>>
    suspend fun proposeVariantHuntKeywords(seedMasterDataId: Long): List<String>
    suspend fun countVariantHuntMatches(brand: String, keywords: List<String>): Int
    suspend fun createVariantHuntFromKeywords(
        seedMasterDataId: Long,
        seedUserCarId: Long?,
        keywords: List<String>
    ): Result<Long>
    suspend fun deleteVariantHuntGroup(groupId: Long)
    suspend fun refreshVariantHuntCompletion()
}
