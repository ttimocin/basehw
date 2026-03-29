package com.taytek.basehw.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.taytek.basehw.data.local.entity.UserCarEntity
import com.taytek.basehw.data.local.entity.UserCarWithMaster
import com.taytek.basehw.data.local.entity.GroupedUserCarWithMaster
import kotlinx.coroutines.flow.Flow

data class BoxStatusCount(
    val isOpened: Boolean,
    val count: Int
)

data class BrandCount(
    val brand: String,
    val count: Int
)

data class HwTierCount(
    val regularCount: Int,
    val premiumCount: Int
)

data class CustomCount(
    val originalCount: Int,
    val customCount: Int
)

@Dao
interface UserCarDao {

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT *, COUNT(*) as rowCount FROM user_cars WHERE isWishlist = 0 GROUP BY COALESCE(masterDataId, -1), manualModelName, manualBrand, manualYear, isOpened ORDER BY id DESC")
    fun getAllWithMaster(): PagingSource<Int, GroupedUserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT *, COUNT(*) as rowCount FROM user_cars WHERE isWishlist = 0 AND isSeriesOnly = 0 GROUP BY COALESCE(masterDataId, -1), manualModelName, manualBrand, manualYear, isOpened ORDER BY id DESC")
    fun getCollectionRecentlyAdded(): PagingSource<Int, GroupedUserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT *, 1 as rowCount FROM user_cars WHERE isWishlist = 1 AND isSeriesOnly = 0 ORDER BY id DESC")
    fun getWishlistWithMaster(): PagingSource<Int, UserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT u.*, 1 as rowCount
        FROM user_cars u
        LEFT JOIN master_data m ON u.masterDataId = m.id
        WHERE u.isWishlist = 1
          AND u.isSeriesOnly = 0
          AND (
              :query IS NULL
              OR COALESCE(m.modelName, u.manualModelName, '') LIKE '%' || :query || '%'
              OR COALESCE(m.brand, u.manualBrand, '') LIKE '%' || :query || '%'
              OR COALESCE(m.series, u.manualSeries, '') LIKE '%' || :query || '%'
              OR COALESCE(m.seriesNum, u.manualSeriesNum, '') LIKE '%' || :query || '%'
          )
        ORDER BY u.id DESC
    """)
    fun getWishlistWithMasterFiltered(query: String?): PagingSource<Int, UserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT *, COUNT(*) as rowCount FROM user_cars u 
        LEFT JOIN master_data m ON u.masterDataId = m.id 
        WHERE u.isWishlist = 0 
        AND (:query IS NULL OR COALESCE(m.modelName, u.manualModelName) LIKE '%' || :query || '%')
        AND (:brand IS NULL OR COALESCE(m.brand, u.manualBrand) = :brand)
        AND (:year IS NULL OR COALESCE(m.year, u.manualYear) = :year)
        AND (:series IS NULL OR COALESCE(m.series, u.manualSeries) LIKE '%' || :series || '%')
        AND (:isOpened IS NULL OR u.isOpened = :isOpened)
        GROUP BY COALESCE(u.masterDataId, -1), u.manualModelName, u.manualBrand, u.manualYear, u.isOpened
        ORDER BY u.id DESC
    """)
    fun getFilteredSortDateDesc(
        query: String? = null, brand: String? = null, year: Int? = null,
        series: String? = null, isOpened: Boolean? = null
    ): PagingSource<Int, GroupedUserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT *, COUNT(*) as rowCount FROM user_cars u 
        LEFT JOIN master_data m ON u.masterDataId = m.id 
        WHERE u.isWishlist = 0 
        AND (:query IS NULL OR COALESCE(m.modelName, u.manualModelName) LIKE '%' || :query || '%')
        AND (:brand IS NULL OR COALESCE(m.brand, u.manualBrand) = :brand)
        AND (:year IS NULL OR COALESCE(m.year, u.manualYear) = :year)
        AND (:series IS NULL OR COALESCE(m.series, u.manualSeries) LIKE '%' || :series || '%')
        AND (:isOpened IS NULL OR u.isOpened = :isOpened)
        GROUP BY COALESCE(u.masterDataId, -1), u.manualModelName, u.manualBrand, u.manualYear, u.isOpened
        ORDER BY u.id ASC
    """)
    fun getFilteredSortDateAsc(
        query: String? = null, brand: String? = null, year: Int? = null,
        series: String? = null, isOpened: Boolean? = null
    ): PagingSource<Int, GroupedUserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT *, COUNT(*) as rowCount FROM user_cars u 
        LEFT JOIN master_data m ON u.masterDataId = m.id 
        WHERE u.isWishlist = 0 
        AND (:query IS NULL OR COALESCE(m.modelName, u.manualModelName) LIKE '%' || :query || '%')
        AND (:brand IS NULL OR COALESCE(m.brand, u.manualBrand) = :brand)
        AND (:year IS NULL OR COALESCE(m.year, u.manualYear) = :year)
        AND (:series IS NULL OR COALESCE(m.series, u.manualSeries) LIKE '%' || :series || '%')
        AND (:isOpened IS NULL OR u.isOpened = :isOpened)
        GROUP BY COALESCE(u.masterDataId, -1), u.manualModelName, u.manualBrand, u.manualYear, u.isOpened
        ORDER BY COALESCE(m.brand, u.manualBrand) ASC, COALESCE(m.modelName, u.manualModelName) ASC
    """)
    fun getFilteredSortBrandAsc(
        query: String? = null, brand: String? = null, year: Int? = null,
        series: String? = null, isOpened: Boolean? = null
    ): PagingSource<Int, GroupedUserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT *, COUNT(*) as rowCount FROM user_cars u 
        LEFT JOIN master_data m ON u.masterDataId = m.id 
        WHERE u.isWishlist = 0 
        AND (:query IS NULL OR COALESCE(m.modelName, u.manualModelName) LIKE '%' || :query || '%')
        AND (:brand IS NULL OR COALESCE(m.brand, u.manualBrand) = :brand)
        AND (:year IS NULL OR COALESCE(m.year, u.manualYear) = :year)
        AND (:series IS NULL OR COALESCE(m.series, u.manualSeries) LIKE '%' || :series || '%')
        AND (:isOpened IS NULL OR u.isOpened = :isOpened)
        GROUP BY COALESCE(u.masterDataId, -1), u.manualModelName, u.manualBrand, u.manualYear, u.isOpened
        ORDER BY COALESCE(m.year, u.manualYear) DESC
    """)
    fun getFilteredSortYearDesc(
        query: String? = null, brand: String? = null, year: Int? = null,
        series: String? = null, isOpened: Boolean? = null
    ): PagingSource<Int, GroupedUserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT *, COUNT(*) as rowCount FROM user_cars u 
        LEFT JOIN master_data m ON u.masterDataId = m.id 
        WHERE u.isWishlist = 0 
        AND (:query IS NULL OR COALESCE(m.modelName, u.manualModelName) LIKE '%' || :query || '%')
        AND (:brand IS NULL OR COALESCE(m.brand, u.manualBrand) = :brand)
        AND (:year IS NULL OR COALESCE(m.year, u.manualYear) = :year)
        AND (:series IS NULL OR COALESCE(m.series, u.manualSeries) LIKE '%' || :series || '%')
        AND (:isOpened IS NULL OR u.isOpened = :isOpened)
        GROUP BY COALESCE(u.masterDataId, -1), u.manualModelName, u.manualBrand, u.manualYear, u.isOpened
        ORDER BY COALESCE(m.year, u.manualYear) ASC
    """)
    fun getFilteredSortYearAsc(
        query: String? = null, brand: String? = null, year: Int? = null,
        series: String? = null, isOpened: Boolean? = null
    ): PagingSource<Int, GroupedUserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT *, COUNT(*) as rowCount FROM user_cars u 
        LEFT JOIN master_data m ON u.masterDataId = m.id 
        WHERE u.isWishlist = 0 
        AND (:query IS NULL OR COALESCE(m.modelName, u.manualModelName) LIKE '%' || :query || '%')
        AND (:brand IS NULL OR COALESCE(m.brand, u.manualBrand) = :brand)
        AND (:year IS NULL OR COALESCE(m.year, u.manualYear) = :year)
        AND (:series IS NULL OR COALESCE(m.series, u.manualSeries) LIKE '%' || :series || '%')
        AND (:isOpened IS NULL OR u.isOpened = :isOpened)
        GROUP BY COALESCE(u.masterDataId, -1), u.manualModelName, u.manualBrand, u.manualYear, u.isOpened
        ORDER BY CASE WHEN u.purchasePrice IS NULL THEN 1 ELSE 0 END ASC, u.purchasePrice DESC
    """)
    fun getFilteredSortPriceDesc(
        query: String? = null, brand: String? = null, year: Int? = null,
        series: String? = null, isOpened: Boolean? = null
    ): PagingSource<Int, GroupedUserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT *, COUNT(*) as rowCount FROM user_cars u 
        LEFT JOIN master_data m ON u.masterDataId = m.id 
        WHERE u.isWishlist = 0 
        AND (:query IS NULL OR COALESCE(m.modelName, u.manualModelName) LIKE '%' || :query || '%')
        AND (:brand IS NULL OR COALESCE(m.brand, u.manualBrand) = :brand)
        AND (:year IS NULL OR COALESCE(m.year, u.manualYear) = :year)
        AND (:series IS NULL OR COALESCE(m.series, u.manualSeries) LIKE '%' || :series || '%')
        AND (:isOpened IS NULL OR u.isOpened = :isOpened)
        GROUP BY COALESCE(u.masterDataId, -1), u.manualModelName, u.manualBrand, u.manualYear, u.isOpened
        ORDER BY CASE WHEN u.purchasePrice IS NULL THEN 1 ELSE 0 END ASC, u.purchasePrice ASC
    """)
    fun getFilteredSortPriceAsc(
        query: String? = null, brand: String? = null, year: Int? = null,
        series: String? = null, isOpened: Boolean? = null
    ): PagingSource<Int, GroupedUserCarWithMaster>


    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT *, 1 as rowCount FROM user_cars WHERE id = :id")
    fun getByIdWithMaster(id: Long): Flow<UserCarWithMaster?>

    @Query("SELECT COUNT(*) FROM user_cars WHERE isWishlist = 0")
    fun getTotalCarsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_cars WHERE isWishlist = 1")
    fun getWishlistCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM user_cars u 
        WHERE u.isWishlist = 1 
        AND (
            u.masterDataId IS NULL 
            OR u.masterDataId NOT IN (SELECT masterDataId FROM user_cars WHERE isWishlist = 0 AND masterDataId IS NOT NULL)
        )
    """)
    fun getWantedNotInCollectionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_cars u LEFT JOIN master_data m ON u.masterDataId = m.id WHERE u.isWishlist = 0 AND (m.feature = 'sth' OR m.feature = 'chase')")
    fun getSthCarsCount(): Flow<Int>

    @Query("SELECT SUM(purchasePrice) FROM user_cars WHERE isWishlist = 0")
    fun getTotalPurchasePrice(): Flow<Double?>

    @Query("SELECT SUM(COALESCE(estimatedValue, purchasePrice, 0)) FROM user_cars WHERE isWishlist = 0")
    fun getTotalEstimatedValue(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM user_cars WHERE isWishlist = 0 AND (purchaseDateMillis >= :startOfMonth OR purchaseDateMillis IS NULL)")
    fun getCarsAddedSinceCount(startOfMonth: Long): Flow<Int>

    @Query("SELECT SUM(COALESCE(estimatedValue, purchasePrice, 0)) FROM user_cars WHERE isWishlist = 0 AND (purchaseDateMillis >= :startOfMonth OR purchaseDateMillis IS NULL)")
    fun getValueAddedSince(startOfMonth: Long): Flow<Double?>

    @Query("SELECT isOpened, COUNT(*) as count FROM user_cars WHERE isWishlist = 0 GROUP BY isOpened")
    fun getBoxStatusCounts(): Flow<List<BoxStatusCount>>

    @Query("SELECT COALESCE(m.brand, u.manualBrand) as brand, COUNT(*) as count FROM user_cars u LEFT JOIN master_data m ON u.masterDataId = m.id WHERE u.isWishlist = 0 GROUP BY brand")
    fun getBrandCounts(): Flow<List<BrandCount>>

    @Query("""
        SELECT
            SUM(CASE WHEN COALESCE(m.isPremium, u.manualIsPremium) = 0 THEN 1 ELSE 0 END) as regularCount,
            SUM(CASE WHEN COALESCE(m.isPremium, u.manualIsPremium) = 1 THEN 1 ELSE 0 END) as premiumCount
        FROM user_cars u
        LEFT JOIN master_data m ON u.masterDataId = m.id
        WHERE u.isWishlist = 0 AND COALESCE(m.brand, u.manualBrand) = 'HOT_WHEELS'
    """)
    fun getHwTierCounts(): Flow<HwTierCount?>
    @Query("""
        SELECT 
            SUM(CASE WHEN isCustom = 0 THEN 1 ELSE 0 END) as originalCount,
            SUM(CASE WHEN isCustom = 1 THEN 1 ELSE 0 END) as customCount
        FROM user_cars
        WHERE isWishlist = 0
    """)
    fun getCustomCounts(): Flow<CustomCount?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(car: UserCarEntity): Long

    @Update
    suspend fun update(car: UserCarEntity)

    @Query("DELETE FROM user_cars WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_cars WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM user_cars WHERE firestoreId = ''")
    suspend fun getUnsyncedCars(): List<UserCarEntity>

    @Query("SELECT firestoreId FROM user_cars WHERE firestoreId != ''")
    suspend fun getAllFirestoreIds(): List<String>

    @Query("UPDATE user_cars SET firestoreId = :firestoreId WHERE id = :id")
    suspend fun updateFirestoreId(id: Long, firestoreId: String)

    @Query("UPDATE user_cars SET userPhotoUrl = :url WHERE id = :id")
    suspend fun updateUserPhotoUrl(id: Long, url: String?)

    @Query("UPDATE user_cars SET backupPhotoUrl = :url WHERE id = :id")
    suspend fun updateBackupPhotoUrl(id: Long, url: String?)

    @Query("UPDATE user_cars SET additionalPhotosBackup = :urls WHERE id = :id")
    suspend fun updateAdditionalPhotosBackup(id: Long, urls: List<String>)

    @Query("UPDATE user_cars SET additionalPhotos = :urls WHERE id = :id")
    suspend fun updateAdditionalPhotos(id: Long, urls: List<String>)

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT *, 1 as rowCount FROM user_cars")
    suspend fun getAllCarsWithMasterList(): List<UserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT *, 1 as rowCount FROM user_cars WHERE firestoreId = ''")
    suspend fun getUnsyncedWithMasterList(): List<UserCarWithMaster>

    @Query("DELETE FROM user_cars")
    suspend fun deleteAll()

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT *, 1 as rowCount FROM user_cars WHERE isWishlist = 1")
    suspend fun getWishlistWithMasterList(): List<UserCarWithMaster>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT *, 1 as rowCount FROM user_cars")
    fun getAllWithMasterListFlow(): Flow<List<UserCarWithMaster>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cars: List<UserCarEntity>)
}
