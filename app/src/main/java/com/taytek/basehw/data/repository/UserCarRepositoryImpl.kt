package com.taytek.basehw.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.taytek.basehw.data.local.dao.MasterDataDao
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
    private val masterDataDao: MasterDataDao,
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
        // Backup all local cars with their identity to ensure reinstallation safety
        val allCars = dao.getAllCarsWithMasterList()
        allCars.forEach { wrapper ->
            val data = mapOf(
                "masterDataId" to wrapper.car.masterDataId,
                "brand" to wrapper.master.brand,
                "modelName" to wrapper.master.modelName,
                "year" to wrapper.master.year,
                "isOpened" to wrapper.car.isOpened,
                "purchaseDateMillis" to wrapper.car.purchaseDateMillis,
                "personalNote" to wrapper.car.personalNote,
                "storageLocation" to wrapper.car.storageLocation,
                "isWishlist" to wrapper.car.isWishlist
            )
            val firestoreId = firestoreDataSource.uploadCarMap(data, wrapper.car.firestoreId)
            if (!firestoreId.isNullOrBlank() && wrapper.car.firestoreId.isBlank()) {
                dao.updateFirestoreId(wrapper.car.id, firestoreId)
            }
        }
    }

    override suspend fun syncFromFirestore() {
        val remoteDataList = firestoreDataSource.fetchAllCars()
        remoteDataList.forEach { data ->
            val brand = data["brand"] as? String ?: ""
            val modelName = data["modelName"] as? String ?: ""
            val year = (data["year"] as? Number)?.toInt()

            // Resolve proper masterDataId for this specific installation
            var targetMasterId = (data["masterDataId"] as? Number)?.toLong()
            
            // Validate if the ID still points to the same car (safety check)
            // Or just lookup by identity to be 100% sure after re-seed
            val resolvedId = masterDataDao.getIdByIdentity(brand, modelName, year)
            if (resolvedId != null) {
                targetMasterId = resolvedId
            }

            if (targetMasterId != null) {
                val entity = com.taytek.basehw.data.local.entity.UserCarEntity(
                    masterDataId = targetMasterId,
                    isOpened = data["isOpened"] as? Boolean ?: false,
                    purchaseDateMillis = (data["purchaseDateMillis"] as? Number)?.toLong(),
                    personalNote = data["personalNote"] as? String ?: "",
                    storageLocation = data["storageLocation"] as? String ?: "",
                    isWishlist = data["isWishlist"] as? Boolean ?: false,
                    firestoreId = data["firestoreId"] as? String ?: ""
                )
                dao.insert(entity)
            }
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
