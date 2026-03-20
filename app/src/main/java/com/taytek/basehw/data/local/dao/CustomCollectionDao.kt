package com.taytek.basehw.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.taytek.basehw.data.local.entity.CollectionCarCrossRef
import com.taytek.basehw.data.local.entity.CollectionWithCars
import com.taytek.basehw.data.local.entity.CustomCollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CustomCollectionEntity): Long

    @Query("DELETE FROM custom_collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCarToCollection(crossRef: CollectionCarCrossRef)

    @Query("DELETE FROM collection_car_cross_ref WHERE collectionId = :collectionId AND userCarId = :userCarId")
    suspend fun removeCarFromCollection(collectionId: Long, userCarId: Long)

    @Transaction
    @Query("SELECT * FROM custom_collections ORDER BY createdAtMillis DESC")
    fun getAllCollectionsWithCars(): Flow<List<CollectionWithCars>>

    @Transaction
    @Query("SELECT * FROM custom_collections WHERE id = :id")
    fun getCollectionById(id: Long): Flow<CollectionWithCars?>
    
    @Query("SELECT * FROM custom_collections ORDER BY createdAtMillis DESC")
    fun getAllCollections(): Flow<List<CustomCollectionEntity>>

    @Query("UPDATE custom_collections SET firestoreId = :firestoreId WHERE id = :id")
    suspend fun updateFirestoreId(id: Long, firestoreId: String)

    @Query("SELECT * FROM custom_collections")
    suspend fun getAllCollectionsList(): List<CustomCollectionEntity>

    @Query("SELECT firestoreId FROM custom_collections WHERE firestoreId != ''")
    suspend fun getAllFirestoreIds(): List<String>

    @Query("SELECT * FROM collection_car_cross_ref")
    suspend fun getAllCrossRefs(): List<CollectionCarCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: CollectionCarCrossRef)

    @Query("DELETE FROM collection_car_cross_ref")
    suspend fun clearAllCrossRefs()

    @Query("DELETE FROM custom_collections")
    suspend fun deleteAllCollections()
}
