package com.taytek.basehw.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.taytek.basehw.data.local.CarPhotoLocalStore
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.local.dao.UserCarDao
import com.taytek.basehw.data.local.entity.CollectionCarCrossRef
import com.taytek.basehw.data.local.entity.CustomCollectionEntity
import com.taytek.basehw.data.local.entity.UserCarEntity
import com.taytek.basehw.data.mapper.toDomain
import com.taytek.basehw.data.mapper.toEntity
import com.taytek.basehw.data.remote.firebase.FirestoreDataSource
import com.taytek.basehw.data.remote.network.SupabaseStorageDataSource
import com.taytek.basehw.data.worker.PhotoBackupWorker
import com.taytek.basehw.domain.repository.AuthRepository
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.BoxStatusStats
import com.taytek.basehw.domain.model.BrandStats
import com.taytek.basehw.domain.model.HwTierStats
import com.taytek.basehw.domain.model.SortOrder
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserCarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: UserCarDao,
    private val masterDataDao: MasterDataDao,
    private val customCollectionDao: com.taytek.basehw.data.local.dao.CustomCollectionDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val supabaseStorageDataSource: SupabaseStorageDataSource,
    private val carPhotoLocalStore: CarPhotoLocalStore,
    private val authRepository: AuthRepository,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager
) : UserCarRepository {

    override fun getCollection(
        query: String?,
        brand: String?,
        year: Int?,
        series: String?,
        isOpened: Boolean?,
        sortOrder: SortOrder
    ): Flow<PagingData<UserCar>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            val q = query
            val b = brand
            val y = year
            val s = series
            val o = isOpened
            when (sortOrder) {
                SortOrder.DATE_ADDED_DESC -> dao.getFilteredSortDateDesc(q, b, y, s, o)
                SortOrder.DATE_ADDED_ASC  -> dao.getFilteredSortDateAsc(q, b, y, s, o)
                SortOrder.BRAND_ASC       -> dao.getFilteredSortBrandAsc(q, b, y, s, o)
                SortOrder.YEAR_DESC       -> dao.getFilteredSortYearDesc(q, b, y, s, o)
                SortOrder.YEAR_ASC        -> dao.getFilteredSortYearAsc(q, b, y, s, o)
                SortOrder.PRICE_DESC      -> dao.getFilteredSortPriceDesc(q, b, y, s, o)
                SortOrder.PRICE_ASC       -> dao.getFilteredSortPriceAsc(q, b, y, s, o)
            }
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getCollectionRecentlyAdded(): Flow<PagingData<UserCar>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            dao.getCollectionRecentlyAdded()
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getWishlist(query: String?): Flow<PagingData<UserCar>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            val q = query?.trim()?.takeIf { it.isNotEmpty() }
            if (q == null) dao.getWishlistWithMaster() else dao.getWishlistWithMasterFiltered(q)
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun addCar(car: UserCar): Long {
        val persistedPhotoUrl = preprocessPhotoUrl(car.userPhotoUrl)
        val persistedAdditional = car.additionalPhotos.map { preprocessPhotoUrl(it) ?: it }
        
        val preservedBackupUrl = car.backupPhotoUrl.takeIf { isRemoteUrl(it) }
        val finalBackupUrl = if (isRemoteUrl(persistedPhotoUrl)) {
            persistedPhotoUrl
        } else {
            preservedBackupUrl
        }
        val entity = car.copy(
            userPhotoUrl = persistedPhotoUrl,
            backupPhotoUrl = finalBackupUrl,
            additionalPhotos = persistedAdditional
        ).toEntity()
        val id = dao.insert(entity)

        if (persistedPhotoUrl != entity.userPhotoUrl) {
            dao.updateUserPhotoUrl(id, persistedPhotoUrl)
        }

        // Real-time sync to Firestore if possible
        dao.getByIdWithMaster(id).firstOrNull()?.let { wrapper ->
            uploadCarToFirestore(wrapper.car, wrapper.master?.brand, wrapper.master?.modelName, wrapper.master?.year)
        }

        enqueuePhotoBackupIfNeeded(id)
        return id
    }

    override suspend fun deleteCar(id: Long) {
        val carWithMaster = dao.getByIdWithMaster(id).firstOrNull()
        val photoUrl = carWithMaster?.car?.backupPhotoUrl ?: carWithMaster?.car?.userPhotoUrl
        val firestoreId = carWithMaster?.car?.firestoreId
        if (!firestoreId.isNullOrBlank()) {
            firestoreDataSource.deleteCar(firestoreId)
        }
        deletePhotoFromSupabaseIfNeeded(photoUrl)
        carWithMaster?.car?.additionalPhotosBackup?.forEach { deletePhotoFromSupabaseIfNeeded(it) }
        dao.deleteById(id)
    }

    override suspend fun deleteCars(ids: List<Long>) {
        ids.forEach { id ->
            val carWithMaster = dao.getByIdWithMaster(id).firstOrNull()
            val photoUrl = carWithMaster?.car?.backupPhotoUrl ?: carWithMaster?.car?.userPhotoUrl
            val firestoreId = carWithMaster?.car?.firestoreId
            if (!firestoreId.isNullOrBlank()) {
                firestoreDataSource.deleteCar(firestoreId)
            }
            deletePhotoFromSupabaseIfNeeded(photoUrl)
            carWithMaster?.car?.additionalPhotosBackup?.forEach { deletePhotoFromSupabaseIfNeeded(it) }
        }
        dao.deleteByIds(ids)
    }

    override suspend fun updateCar(car: UserCar) {
        val previous = dao.getByIdWithMaster(car.id).firstOrNull()?.car
        val previousLocalPhotoUrl = previous?.userPhotoUrl
        val previousBackupPhotoUrl = previous?.backupPhotoUrl
        val persistedPhotoUrl = preprocessPhotoUrl(car.userPhotoUrl)
        
        // Handle additional photos preprocessing
        val persistedAdditional = car.additionalPhotos.map { preprocessPhotoUrl(it) ?: it }
        
        val preservedBackupUrl = car.backupPhotoUrl.takeIf { isRemoteUrl(it) }
        val finalBackupUrl = when {
            persistedPhotoUrl.isNullOrBlank() -> null
            isRemoteUrl(persistedPhotoUrl) -> persistedPhotoUrl
            previousLocalPhotoUrl == persistedPhotoUrl -> preservedBackupUrl
            else -> null
        }

        val entity = car.copy(
            userPhotoUrl = persistedPhotoUrl,
            backupPhotoUrl = finalBackupUrl,
            additionalPhotos = persistedAdditional
        ).toEntity()
        dao.update(entity)

        if (previousBackupPhotoUrl != null && previousLocalPhotoUrl != persistedPhotoUrl) {
            deletePhotoFromSupabaseIfNeeded(previousBackupPhotoUrl)
        }

        enqueuePhotoBackupIfNeeded(entity.id)

        // Real-time sync to Firestore if possible
        if (authRepository.currentUser != null) {
            uploadCarToFirestore(entity, car.masterData?.brand?.name, car.masterData?.modelName, car.masterData?.year)
        }
    }

    private suspend fun uploadCarToFirestore(carEntity: UserCarEntity, brand: String?, modelName: String?, year: Int?) {
        val currentUser = authRepository.currentUser ?: return
        val finalPhotoUrl = sanitizePhotoUrlForCloud(carEntity.userPhotoUrl, carEntity.backupPhotoUrl)

        val data: Map<String, Any?> = mapOf(
            "masterDataId" to carEntity.masterDataId,
            // Catalog search fields
            "brand" to brand,
            "modelName" to modelName,
            "year" to year,
            // Manual entry fields
            "manualModelName" to carEntity.manualModelName,
            "manualBrand" to carEntity.manualBrand,
            "manualSeries" to carEntity.manualSeries,
            "manualSeriesNum" to carEntity.manualSeriesNum,
            "manualYear" to carEntity.manualYear,
            "manualScale" to carEntity.manualScale,
            "manualIsPremium" to carEntity.manualIsPremium,
            // Common fields
            "isOpened" to carEntity.isOpened,
            "purchaseDateMillis" to carEntity.purchaseDateMillis,
            "personalNote" to carEntity.personalNote,
            "storageLocation" to carEntity.storageLocation,
            "purchasePrice" to carEntity.purchasePrice,
            "estimatedValue" to carEntity.estimatedValue,
            "isWishlist" to carEntity.isWishlist,
            "isFavorite" to carEntity.isFavorite,
            "isSeriesOnly" to carEntity.isSeriesOnly,
            "userPhotoUrl" to finalPhotoUrl,
            "backupPhotoUrl" to finalPhotoUrl,
            "additionalPhotosBackup" to carEntity.additionalPhotosBackup
        )
        val firestoreId = firestoreDataSource.uploadCarMap(data, carEntity.firestoreId)
        if (!firestoreId.isNullOrBlank() && carEntity.firestoreId.isBlank()) {
            dao.updateFirestoreId(carEntity.id, firestoreId)
        }
    }

    override suspend fun syncToFirestore() {
        // 0. Sync Preferences (Currency)
        val currency = appSettingsManager.currencyFlow.value
        if (currency.isNotBlank()) {
            firestoreDataSource.saveUserPreferences(mapOf("currency" to currency))
        }

        // 1. Sync Cars
        val allCars = dao.getAllCarsWithMasterList()
        allCars.forEach { wrapper ->
            uploadCarToFirestore(wrapper.car, wrapper.master?.brand, wrapper.master?.modelName, wrapper.master?.year)
        }

        // 2. Sync Folders
        val allFolders = customCollectionDao.getAllCollectionsList()
        allFolders.forEach { folder ->
            val folderData = mapOf(
                "name" to folder.name,
                "description" to folder.description,
                "coverPhotoUrl" to folder.coverPhotoUrl,
                "createdAtMillis" to folder.createdAtMillis
            )
            val firestoreId = firestoreDataSource.uploadFolderMap(folderData, folder.firestoreId)
            if (!firestoreId.isNullOrBlank() && folder.firestoreId.isBlank()) {
                customCollectionDao.updateFirestoreId(folder.id, firestoreId)
            }
        }

        // 3. Sync Mappings
        val allMappings = customCollectionDao.getAllCrossRefs()
        allMappings.forEach { crossRef ->
            val car = dao.getByIdWithMaster(crossRef.userCarId).firstOrNull()?.car
            val folder = customCollectionDao.getAllCollectionsList().find { it.id == crossRef.collectionId }
            
            if (car != null && folder != null && car.firestoreId.isNotBlank() && folder.firestoreId.isNotBlank()) {
                firestoreDataSource.uploadMapping(folder.firestoreId, car.firestoreId)
            }
        }
    }

    override suspend fun syncFromFirestore() {
        // 0. Sync Preferences
        val prefs = firestoreDataSource.fetchUserPreferences()
        prefs?.get("currency")?.let {
            if (it is String && it.isNotBlank()) {
                appSettingsManager.setCurrency(it)
            }
        }

        // 1. Sync Cars
        val remoteCars = firestoreDataSource.fetchAllCars()
        val localFirestoreIds = dao.getAllFirestoreIds().toSet()
        
        remoteCars.forEach { data ->
            val firestoreId = data["firestoreId"] as? String ?: ""
            if (firestoreId.isBlank() || localFirestoreIds.contains(firestoreId)) return@forEach

            val brand = data["brand"] as? String ?: ""
            val modelName = data["modelName"] as? String ?: ""
            val year = (data["year"] as? Number)?.toInt()

            val resolvedId = masterDataDao.getIdByIdentity(brand, modelName, year)
            // Allow restoration even if not in master list (e.g. manual entries or legacy catalog items)
            val additionalRemote = (data["additionalPhotosBackup"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            
            val entity = UserCarEntity(
                masterDataId = resolvedId,
                manualModelName = data["manualModelName"] as? String,
                manualBrand = data["manualBrand"] as? String,
                manualSeries = data["manualSeries"] as? String,
                manualSeriesNum = data["manualSeriesNum"] as? String,
                manualYear = (data["manualYear"] as? Number)?.toInt(),
                manualScale = data["manualScale"] as? String,
                manualIsPremium = data["manualIsPremium"] as? Boolean,
                isOpened = data["isOpened"] as? Boolean ?: false,
                purchaseDateMillis = (data["purchaseDateMillis"] as? Number)?.toLong(),
                personalNote = data["personalNote"] as? String ?: "",
                storageLocation = data["storageLocation"] as? String ?: "",
                purchasePrice = (data["purchasePrice"] as? Number)?.toDouble(),
                estimatedValue = (data["estimatedValue"] as? Number)?.toDouble(),
                isWishlist = data["isWishlist"] as? Boolean ?: false,
                isFavorite = data["isFavorite"] as? Boolean ?: false,
                isSeriesOnly = data["isSeriesOnly"] as? Boolean ?: false,
                firestoreId = firestoreId,
                userPhotoUrl = (data["userPhotoUrl"] as? String),
                backupPhotoUrl = (data["backupPhotoUrl"] as? String) ?: (data["userPhotoUrl"] as? String),
                additionalPhotos = additionalRemote,
                additionalPhotosBackup = additionalRemote
            )
            dao.insert(entity)
        }

        // 2. Sync Folders
        val remoteFolders = firestoreDataSource.fetchAllFolders()
        val localFolderFirestoreIds = customCollectionDao.getAllFirestoreIds().toSet()

        remoteFolders.forEach { data ->
            val firestoreId = data["firestoreId"] as? String ?: ""
            if (firestoreId.isBlank() || localFolderFirestoreIds.contains(firestoreId)) return@forEach

            val entity = CustomCollectionEntity(
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                coverPhotoUrl = data["coverPhotoUrl"] as? String,
                createdAtMillis = (data["createdAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                firestoreId = firestoreId
            )
            customCollectionDao.insertCollection(entity)
        }

        // 3. Sync Mappings
        val remoteMappings = firestoreDataSource.fetchAllMappings()
        val allLocalCars = dao.getAllCarsWithMasterList()
        val allLocalFolders = customCollectionDao.getAllCollectionsList()

        remoteMappings.forEach { data ->
            val folderFirestoreId = data["folderId"] as? String ?: ""
            val carFirestoreId = data["carId"] as? String ?: ""

            val localCar = allLocalCars.find { it.car.firestoreId == carFirestoreId }
            val localFolder = allLocalFolders.find { it.firestoreId == folderFirestoreId }

            if (localCar != null && localFolder != null) {
                customCollectionDao.insertCrossRef(
                    CollectionCarCrossRef(
                        collectionId = localFolder.id,
                        userCarId = localCar.car.id
                    )
                )
            }
        }
    }

    override suspend fun deleteCloudData(): Result<Unit> {
        return try {
            // 1) Delete user's Supabase photo objects referenced by local records.
            // This runs before auth account deletion while Firebase ID token is still valid.
            val supabaseUrls = dao.getAllCarsWithMasterList()
                .flatMap { 
                     val urls = mutableListOf<String>()
                     (it.car.backupPhotoUrl ?: it.car.userPhotoUrl)?.let { u -> urls.add(u) }
                     urls.addAll(it.car.additionalPhotosBackup)
                     urls
                }
                .filter { it.contains("/storage/v1/object/public/") }
                .distinct()

            supabaseUrls.forEach { url ->
                deletePhotoFromSupabaseIfNeeded(url)
            }

            // 2) Delete Firestore user data (cars/folders/mappings/user doc).
            firestoreDataSource.deleteUserAccountData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearLocalData() {
        dao.deleteAll()
        customCollectionDao.clearAllCrossRefs()
        customCollectionDao.deleteAllCollections()
    }

    override fun getCarById(id: Long): Flow<UserCar?> {
        return dao.getByIdWithMaster(id).map { it?.toDomain() }
    }

    override fun getTotalCarsCount(): Flow<Int> {
        return dao.getTotalCarsCount()
    }

    override fun getWishlistCount(): Flow<Int> {
        return dao.getWishlistCount()
    }

    override fun getWantedNotInCollectionCount(): Flow<Int> {
        return dao.getWantedNotInCollectionCount()
    }

    override fun getSthCarsCount(): Flow<Int> {
        return dao.getSthCarsCount()
    }

    override fun getTotalPurchasePrice(): Flow<Double> {
        return dao.getTotalPurchasePrice().map { it ?: 0.0 }
    }

    override fun getTotalEstimatedValue(): Flow<Double> {
        return dao.getTotalEstimatedValue().map { it ?: 0.0 }
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

    override fun getHwTierStats(): Flow<HwTierStats> {
        return dao.getHwTierCounts().map { tier ->
            HwTierStats(
                regularCount = tier?.regularCount ?: 0,
                premiumCount = tier?.premiumCount ?: 0
            )
        }
    }

    override fun getCarsAddedSinceCount(startOfMonth: Long): Flow<Int> {
        return dao.getCarsAddedSinceCount(startOfMonth)
    }

    override fun getValueAddedSince(startOfMonth: Long): Flow<Double> {
        return dao.getValueAddedSince(startOfMonth).map { it ?: 0.0 }
    }

    override fun getCustomStats(): Flow<com.taytek.basehw.domain.model.CustomStats> {
        return dao.getCustomCounts().map { count ->
            com.taytek.basehw.domain.model.CustomStats(
                originalCount = count?.originalCount ?: 0,
                customCount = count?.customCount ?: 0
            )
        }
    }

    override suspend fun addSeriesToWishlist(brand: Brand, series: String, year: Int?) {
        val seriesItems = masterDataDao.getListBySeriesAndYear(brand.name, series, year)
        seriesItems.forEach { master ->
            // Check if already in wishlist or collection to avoid duplicates if desired,
            // but for wishlist, usually we can just add it if it's "wanted".
            // Here we just add it as a wishlist item if not already present in wishlist.
            val existing = dao.getWishlistWithMasterList().find { it.master?.id == master.id }
            if (existing == null) {
                val entity = UserCarEntity(
                    masterDataId = master.id,
                    isWishlist = true,
                    isSeriesOnly = true
                )
                val newId = dao.insert(entity)
                
                // Trigger real-time sync if user is logged in
                if (authRepository.currentUser != null) {
                    uploadCarToFirestore(entity.copy(id = newId), master.brand, master.modelName, master.year)
                }
            }
        }
    }

    override suspend fun deleteWishlistSeries(brand: Brand, seriesName: String) {
        val wishlistWithMaster = dao.getWishlistWithMasterList()
        val idsToDelete = wishlistWithMaster
            .filter { it.master?.brand == brand.name && it.master?.series == seriesName }
            .map { it.car.id }
        if (idsToDelete.isNotEmpty()) {
            deleteCars(idsToDelete)
        }
    }

    override suspend fun isSeriesInWishlist(brand: Brand, series: String): Boolean {
        return dao.getWishlistWithMasterList().any {
            it.car.isSeriesOnly && it.master?.brand == brand.name && it.master?.series == series
        }
    }

    override fun getWishlistSeriesTracking(): Flow<List<com.taytek.basehw.domain.model.SeriesTracking>> {
        return dao.getAllWithMasterListFlow().map { allCars ->
            val wishlistItems = allCars.filter { it.car.isWishlist }
            val collectionItems = allCars.filter { !it.car.isWishlist }

            // Group wishlist items by series
            val wishlistBySeries = wishlistItems
                .filter { it.car.isSeriesOnly && it.master?.series?.isNotBlank() == true }
                .groupBy { it.master?.brand?.let { b -> Brand.valueOf(b) } to it.master?.series }

            val resultList = mutableListOf<com.taytek.basehw.domain.model.SeriesTracking>()
            
            for ((key, _) in wishlistBySeries) {
                val (brand, seriesName) = key
                if (brand == null || seriesName == null) continue

                // Show only years that were explicitly tracked via "add series to wishlist".
                val trackedYears = wishlistItems
                    .filter {
                        it.car.isSeriesOnly &&
                            it.master?.brand == brand.name &&
                            it.master?.series == seriesName
                    }
                    .mapNotNull { it.master?.year }
                    .toSet()

                val allSeriesMasterData = if (trackedYears.isNotEmpty()) {
                    trackedYears
                        .flatMap { year -> masterDataDao.getListBySeriesAndYear(brand.name, seriesName, year) }
                        .distinctBy { it.id }
                } else {
                    emptyList()
                }
                
                val trackingItems = allSeriesMasterData.map { master ->
                    val masterDomain = master.toDomain()
                    val isInCollection = collectionItems.any { it.master?.id == master.id }
                    val isInWishlist = wishlistItems.any { it.master?.id == master.id }
                    val wishlistId = wishlistItems.find { it.master?.id == master.id }?.car?.id
                    
                    com.taytek.basehw.domain.model.SeriesTrackingItem(
                        masterData = masterDomain,
                        isInCollection = isInCollection,
                        isInWishlist = isInWishlist,
                        wishlistId = wishlistId
                    )
                }

                resultList.add(
                    com.taytek.basehw.domain.model.SeriesTracking(
                        brand = brand,
                        seriesName = seriesName,
                        items = trackingItems
                    )
                )
            }
            resultList
        }
    }

    override suspend fun getAllCarsWithMasterList(): List<UserCar> {
        return dao.getAllCarsWithMasterList().map { it.toDomain() }
    }

    private fun preprocessPhotoUrl(userPhotoUrl: String?): String? {
        if (userPhotoUrl.isNullOrBlank()) return null
        if (userPhotoUrl.startsWith("http://") || userPhotoUrl.startsWith("https://")) return userPhotoUrl
        return carPhotoLocalStore.persistCompressed(userPhotoUrl) ?: userPhotoUrl
    }

    private fun sanitizePhotoUrlForCloud(userPhotoUrl: String?, backupPhotoUrl: String?): String? {
        if (isRemoteUrl(backupPhotoUrl)) return backupPhotoUrl
        if (isRemoteUrl(userPhotoUrl)) return userPhotoUrl
        return null
    }

    private suspend fun deletePhotoFromSupabaseIfNeeded(userPhotoUrl: String?) {
        val url = userPhotoUrl ?: return
        if (!url.contains("/storage/v1/object/public/")) return
        supabaseStorageDataSource.deleteByPublicUrl(url)
    }

    private fun isRemoteUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }

    override suspend fun hasCloudData(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val user = authRepository.currentUser
        println("DEBUG_BACKUP: hasCloudData check. User: ${user?.uid}")
        if (user == null) return@withContext false
        try {
            kotlinx.coroutines.withTimeout(5000) {
                val cars = firestoreDataSource.fetchAllCars()
                val folders = firestoreDataSource.fetchAllFolders()
                val mappings = firestoreDataSource.fetchAllMappings()
                println("DEBUG_BACKUP: Cloud counts - Cars: ${cars.size}, Folders: ${folders.size}, Mappings: ${mappings.size}")
                cars.isNotEmpty() || folders.isNotEmpty() || mappings.isNotEmpty()
            }
        } catch (e: Exception) {
            println("DEBUG_BACKUP: hasCloudData error: ${e.message}")
            false
        }
    }

    private fun enqueuePhotoBackupIfNeeded(carId: Long) {
        if (authRepository.currentUser == null) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putLong(PhotoBackupWorker.KEY_CAR_ID, carId)
            .build()

        val request = OneTimeWorkRequestBuilder<PhotoBackupWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${PhotoBackupWorker.WORK_NAME_PREFIX}${carId}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
