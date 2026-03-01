package com.taytek.basehw.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.taytek.basehw.data.local.entity.UserCarEntity
import com.taytek.basehw.data.local.entity.UserCarWithMaster
import kotlinx.coroutines.flow.Flow

data class BoxStatusCount(
    val isOpened: Boolean,
    val count: Int
)

data class BrandCount(
    val brand: String,
    val count: Int
)

@Dao
interface UserCarDao {

    @Transaction
    @Query("SELECT * FROM user_cars WHERE isWishlist = 0 ORDER BY id DESC")
    fun getAllWithMaster(): PagingSource<Int, UserCarWithMaster>

    @Transaction
    @Query("SELECT * FROM user_cars WHERE isWishlist = 1 ORDER BY id DESC")
    fun getWishlistWithMaster(): PagingSource<Int, UserCarWithMaster>


    @Transaction
    @Query("SELECT * FROM user_cars WHERE id = :id")
    fun getByIdWithMaster(id: Long): Flow<UserCarWithMaster?>

    @Query("SELECT COUNT(*) FROM user_cars WHERE isWishlist = 0")
    fun getTotalCarsCount(): Flow<Int>

    @Query("SELECT isOpened, COUNT(*) as count FROM user_cars WHERE isWishlist = 0 GROUP BY isOpened")
    fun getBoxStatusCounts(): Flow<List<BoxStatusCount>>

    @Query("SELECT m.brand, COUNT(*) as count FROM user_cars u INNER JOIN master_data m ON u.masterDataId = m.id WHERE u.isWishlist = 0 GROUP BY m.brand")
    fun getBrandCounts(): Flow<List<BrandCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(car: UserCarEntity): Long

    @Update
    suspend fun update(car: UserCarEntity)

    @Query("DELETE FROM user_cars WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM user_cars WHERE firestoreId = ''")
    suspend fun getUnsyncedCars(): List<UserCarEntity>

    @Query("UPDATE user_cars SET firestoreId = :firestoreId WHERE id = :id")
    suspend fun updateFirestoreId(id: Long, firestoreId: String)

    @Transaction
    @Query("SELECT * FROM user_cars")
    suspend fun getAllCarsWithMasterList(): List<UserCarWithMaster>

    @Transaction
    @Query("SELECT * FROM user_cars WHERE firestoreId = ''")
    suspend fun getUnsyncedWithMasterList(): List<UserCarWithMaster>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cars: List<UserCarEntity>)
}
