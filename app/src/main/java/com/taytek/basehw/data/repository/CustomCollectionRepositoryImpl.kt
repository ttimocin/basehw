package com.taytek.basehw.data.repository

import com.taytek.basehw.data.local.dao.CustomCollectionDao
import com.taytek.basehw.data.local.dao.UserCarDao
import com.taytek.basehw.data.local.entity.CollectionCarCrossRef
import com.taytek.basehw.data.mapper.toDomain
import com.taytek.basehw.data.mapper.toEntity
import com.taytek.basehw.data.remote.firebase.FirestoreDataSource
import com.taytek.basehw.domain.model.CustomCollection
import com.taytek.basehw.domain.repository.CustomCollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CustomCollectionRepositoryImpl @Inject constructor(
    private val dao: CustomCollectionDao,
    private val userCarDao: UserCarDao,
    private val firestoreDataSource: FirestoreDataSource
) : CustomCollectionRepository {

    override suspend fun createCollection(collection: CustomCollection): Long {
        val id = dao.insertCollection(collection.toEntity())
        // Auto-sync new collection to Firestore in the background
        val folderData = mapOf(
            "name" to collection.name,
            "description" to collection.description,
            "coverPhotoUrl" to collection.coverPhotoUrl,
            "createdAtMillis" to collection.createdAt.time
        )
        val firestoreId = firestoreDataSource.uploadFolderMap(folderData, "")
        if (!firestoreId.isNullOrBlank()) {
            dao.updateFirestoreId(id, firestoreId)
        }
        return id
    }

    override suspend fun deleteCollection(id: Long) {
        dao.deleteCollection(id)
    }

    override suspend fun addCarToCollection(collectionId: Long, carId: Long) {
        dao.addCarToCollection(CollectionCarCrossRef(collectionId, carId))
        // Auto-sync the collection mapping to Firestore
        val car = userCarDao.getByIdWithMaster(carId).firstOrNull()?.car
        val folder = dao.getAllCollectionsList().find { it.id == collectionId }
        if (car != null && folder != null &&
            car.firestoreId.isNotBlank() && folder.firestoreId.isNotBlank()
        ) {
            firestoreDataSource.uploadMapping(folder.firestoreId, car.firestoreId)
        }
    }

    override suspend fun removeCarFromCollection(collectionId: Long, carId: Long) {
        dao.removeCarFromCollection(collectionId, carId)
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
