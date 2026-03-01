package com.taytek.basehw.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.taytek.basehw.data.local.dao.UserCarDao
import com.taytek.basehw.data.mapper.toDomain
import com.taytek.basehw.data.mapper.toEntity
import com.taytek.basehw.data.remote.firebase.FirestoreDataSource
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.BoxStatusStats
import com.taytek.basehw.domain.model.BrandStats
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.UserCarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserCarRepositoryImpl @Inject constructor(
    private val dao: UserCarDao,
    private val firestoreDataSource: FirestoreDataSource
) : UserCarRepository {

    override fun getCollection(): Flow<PagingData<UserCar>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            dao.getAllWithMaster()
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getWishlist(): Flow<PagingData<UserCar>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            dao.getWishlistWithMaster()
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun addCar(car: UserCar): Long {
        val id = dao.insert(car.toEntity())
        return id
    }

    override suspend fun deleteCar(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun updateCar(car: UserCar) {
        dao.update(car.toEntity())
    }

    override suspend fun syncToFirestore() {
        val unsynced = dao.getUnsyncedCars()
        unsynced.forEach { entity ->
            val firestoreId = firestoreDataSource.uploadCar(entity)
            if (!firestoreId.isNullOrBlank()) {
                dao.updateFirestoreId(entity.id, firestoreId)
            }
        }
    }

    override suspend fun syncFromFirestore() {
        // Pull from Firestore and merge into local DB
        val remoteCars = firestoreDataSource.fetchAllCars()
        remoteCars.forEach { data ->
            val entity = try {
                com.taytek.basehw.data.local.entity.UserCarEntity(
                    masterDataId = (data["masterDataId"] as? Long) ?: return@forEach,
                    isOpened = data["isOpened"] as? Boolean ?: false,
                    purchaseDateMillis = data["purchaseDateMillis"] as? Long,
                    personalNote = data["personalNote"] as? String ?: "",
                    storageLocation = data["storageLocation"] as? String ?: "",
                    isWishlist = data["isWishlist"] as? Boolean ?: false,
                    firestoreId = data["firestoreId"] as? String ?: ""
                )
            } catch (e: Exception) { null }
            entity?.let { dao.insert(it) }
        }
    }

    override fun getCarById(id: Long): Flow<UserCar?> {
        return dao.getByIdWithMaster(id).map { it?.toDomain() }
    }

    override fun getTotalCarsCount(): Flow<Int> {
        return dao.getTotalCarsCount()
    }

    override fun getBoxStatusCounts(): Flow<List<BoxStatusStats>> {
        return dao.getBoxStatusCounts().map { list ->
            list.map { BoxStatusStats(isOpened = it.isOpened, count = it.count) }
        }
    }

    override fun getBrandCounts(): Flow<List<BrandStats>> {
        return dao.getBrandCounts().map { list ->
            list.map { entity ->
                // Mapped brand string matching the MasterDataEntity
                val brandEnum = try { Brand.valueOf(entity.brand) } catch (e: Exception) { Brand.HOT_WHEELS }
                BrandStats(brand = brandEnum, count = entity.count)
            }
        }
    }
}
