package com.taytek.basehw.domain.repository

import com.taytek.basehw.domain.model.CustomCollection
import kotlinx.coroutines.flow.Flow

interface CustomCollectionRepository {
    suspend fun createCollection(collection: CustomCollection): Long
    suspend fun deleteCollection(id: Long)
    suspend fun addCarToCollection(collectionId: Long, carId: Long)
    suspend fun removeCarFromCollection(collectionId: Long, carId: Long)
    fun getAllCollections(): Flow<List<CustomCollection>>
    fun getCollectionById(id: Long): Flow<CustomCollection?>
}
