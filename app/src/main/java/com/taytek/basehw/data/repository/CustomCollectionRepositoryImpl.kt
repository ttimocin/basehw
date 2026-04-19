package com.taytek.basehw.data.repository

import com.taytek.basehw.data.local.dao.CustomCollectionDao
import com.taytek.basehw.data.local.entity.CollectionCarCrossRef
import com.taytek.basehw.data.mapper.toDomain
import com.taytek.basehw.data.mapper.toEntity
import com.taytek.basehw.domain.model.CustomCollection
import com.taytek.basehw.domain.repository.CustomCollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CustomCollectionRepositoryImpl @Inject constructor(
    private val dao: CustomCollectionDao,
    private val userCarRepository: com.taytek.basehw.domain.repository.UserCarRepository
) : CustomCollectionRepository {

    override suspend fun createCollection(collection: CustomCollection): Long {
        val id = dao.insertCollection(collection.toEntity())
        userCarRepository.triggerSync()
        return id
    }

    override suspend fun deleteCollection(id: Long) {
        dao.deleteCollection(id)
        userCarRepository.triggerSync()
    }

    override suspend fun addCarToCollection(collectionId: Long, carId: Long) {
        dao.addCarToCollection(CollectionCarCrossRef(collectionId, carId))
        userCarRepository.triggerSync()
    }

    override suspend fun removeCarFromCollection(collectionId: Long, carId: Long) {
        dao.removeCarFromCollection(collectionId, carId)
        userCarRepository.triggerSync()
    }

    override fun getAllCollections(): Flow<List<CustomCollection>> {
        return dao.getAllCollectionsWithCars().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getCollectionById(id: Long): Flow<CustomCollection?> {
        return dao.getCollectionById(id).map { it?.toDomain() }
    }
}
