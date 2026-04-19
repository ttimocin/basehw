package com.taytek.basehw.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OutOfQuotaPolicy
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.taytek.basehw.data.local.CarPhotoLocalStore
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.local.dao.UserCarDao
import com.taytek.basehw.data.local.dao.VariantHuntDao
import com.taytek.basehw.data.local.entity.CollectionCarCrossRef
import com.taytek.basehw.data.local.entity.CustomCollectionEntity
import com.taytek.basehw.data.local.entity.UserCarEntity
import com.taytek.basehw.data.local.entity.VariantHuntGroupEntity
import com.taytek.basehw.data.local.entity.UserCarWithMaster
import com.taytek.basehw.data.mapper.toDomain
import com.taytek.basehw.data.mapper.toEntity
import com.taytek.basehw.data.remote.network.SupabaseStorageDataSource
import com.taytek.basehw.data.worker.PhotoBackupWorker
import com.taytek.basehw.data.worker.CollectionSyncWorker
import com.taytek.basehw.domain.repository.AuthRepository
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.BoxStatusStats
import com.taytek.basehw.domain.model.BrandStats
import com.taytek.basehw.domain.model.HwTierStats
import com.taytek.basehw.domain.model.RankCarInput
import com.taytek.basehw.domain.model.SortOrder
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.model.VehicleCondition
import com.taytek.basehw.domain.repository.SupabaseSyncRepository
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import com.taytek.basehw.domain.model.CollectionImportMode
import com.taytek.basehw.domain.model.CollectionImportStats
import com.taytek.basehw.domain.model.VariantHuntGroupSummary
import com.taytek.basehw.domain.model.VariantHuntMasterRow
import java.io.InputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserCarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: UserCarDao,
    private val masterDataDao: MasterDataDao,
    private val variantHuntDao: VariantHuntDao,
    private val customCollectionDao: com.taytek.basehw.data.local.dao.CustomCollectionDao,
    private val supabaseSyncRepository: SupabaseSyncRepository,
    private val supabaseStorageDataSource: SupabaseStorageDataSource,
    private val carPhotoLocalStore: CarPhotoLocalStore,
    private val authRepository: AuthRepository,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager
) : UserCarRepository {

    private val snapshotJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun getCollection(
        query: String?,
        brand: String?,
        year: Int?,
        series: String?,
        condition: String?,
        sortOrder: SortOrder
    ): Flow<PagingData<UserCar>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) {
            val q = query
            val b = brand
            val y = year
            val s = series
            val c = condition
            when (sortOrder) {
                SortOrder.DATE_ADDED_DESC -> dao.getFilteredSortDateDesc(q, b, y, c)
                SortOrder.DATE_ADDED_ASC  -> dao.getFilteredSortDateAsc(q, b, y, c)
                SortOrder.BRAND_ASC       -> dao.getFilteredSortBrandAsc(q, b, y, c)
                SortOrder.YEAR_DESC       -> dao.getFilteredSortYearDesc(q, b, y, c)
                SortOrder.YEAR_ASC        -> dao.getFilteredSortYearAsc(q, b, y, c)
                SortOrder.PRICE_DESC      -> dao.getFilteredSortPriceDesc(q, b, y, c)
                SortOrder.PRICE_ASC       -> dao.getFilteredSortPriceAsc(q, b, y, c)
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

        syncSnapshotToSupabaseIfSignedIn()

        enqueuePhotoBackupIfNeeded(id)
        refreshVariantHuntCompletion()
        return id
    }

    override suspend fun deleteCar(id: Long) {
        val carWithMaster = dao.getByIdWithMaster(id).firstOrNull()
        val photoUrl = carWithMaster?.car?.backupPhotoUrl ?: carWithMaster?.car?.userPhotoUrl
        deletePhotoFromSupabaseIfNeeded(photoUrl)
        carWithMaster?.car?.additionalPhotosBackup?.forEach { deletePhotoFromSupabaseIfNeeded(it) }
        dao.deleteById(id)
        syncSnapshotToSupabaseIfSignedIn()
        refreshVariantHuntCompletion()
    }

    override suspend fun deleteCars(ids: List<Long>) {
        ids.forEach { id ->
            val carWithMaster = dao.getByIdWithMaster(id).firstOrNull()
            val photoUrl = carWithMaster?.car?.backupPhotoUrl ?: carWithMaster?.car?.userPhotoUrl
            deletePhotoFromSupabaseIfNeeded(photoUrl)
            carWithMaster?.car?.additionalPhotosBackup?.forEach { deletePhotoFromSupabaseIfNeeded(it) }
        }
        dao.deleteByIds(ids)
        syncSnapshotToSupabaseIfSignedIn()
        refreshVariantHuntCompletion()
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
        syncSnapshotToSupabaseIfSignedIn()
        refreshVariantHuntCompletion()
    }

    override suspend fun syncToSupabase() {

        val user = authRepository.currentUser ?: run {
            Log.w(TAG, "syncToSupabase: NO USER FOUND, ABORTING")
            return
        }
        if (!user.isEmailVerified) {
            Log.w(TAG, "syncToSupabase: EMAIL NOT VERIFIED, ABORTING")
            return
        }


        val snapshot = CollectionSnapshotPayload(
            currency = appSettingsManager.currencyFlow.value.ifBlank { "EUR" },
            cars = dao.getAllCarsWithMasterList().map { row ->
                val remoteMain = sanitizePhotoUrlForCloud(row.car.userPhotoUrl, row.car.backupPhotoUrl)
                val remoteAdditional = (row.car.additionalPhotosBackup + row.car.additionalPhotos)
                    .filter { isRemoteUrl(it) }
                    .distinct()
                SnapshotCar(
                    localId = row.car.id,
                    masterDataId = row.car.masterDataId,
                    masterBrand = row.master?.brand,
                    masterFeature = row.master?.feature,
                    masterModelName = row.master?.modelName,
                    masterYear = row.master?.year,
                    masterSeries = row.master?.series,
                    masterToyNum = row.master?.toyNum,
                    masterDataSource = row.master?.dataSource,
                    masterImageUrl = row.master?.imageUrl,
                    manualModelName = row.car.manualModelName,
                    manualBrand = row.car.manualBrand,
                    manualSeries = row.car.manualSeries,
                    manualSeriesNum = row.car.manualSeriesNum,
                    manualYear = row.car.manualYear,
                    manualScale = row.car.manualScale,
                    manualIsPremium = row.car.manualIsPremium,
                    feature = row.master?.feature,
                    condition = row.car.condition,
                    purchaseDateMillis = row.car.purchaseDateMillis,
                    personalNote = row.car.personalNote,
                    storageLocation = row.car.storageLocation,
                    firestoreId = row.car.firestoreId,
                    isWishlist = row.car.isWishlist,
                    userPhotoUrl = remoteMain,
                    backupPhotoUrl = row.car.backupPhotoUrl.takeIf { isRemoteUrl(it) },
                    purchasePrice = row.car.purchasePrice,
                    estimatedValue = row.car.estimatedValue,
                    isFavorite = row.car.isFavorite,
                    isSeriesOnly = row.car.isSeriesOnly,
                    isCustom = row.car.isCustom,
                    quantity = row.car.quantity,
                    additionalPhotos = remoteAdditional,
                    additionalPhotosBackup = remoteAdditional,
                    hwCardType = row.car.hwCardType
                )
            },
            folders = customCollectionDao.getAllCollectionsList().map { SnapshotFolder.fromEntity(it) },
            mappings = customCollectionDao.getAllCrossRefs().map { SnapshotMapping(it.collectionId, it.userCarId) }
        )

        val payload = snapshotJson.encodeToString(snapshot)

        supabaseSyncRepository.upsertCollectionSnapshot(user.uid, payload)
            .getOrElse { 
                Log.e(TAG, "syncToSupabase: UPSERT FAILED", it)
                throw it 
            }
        

        syncPublicListingsToSupabase(user.uid)
    }

    private suspend fun syncPublicListingsToSupabase(firebaseUid: String) {
        val profile = authRepository.getUserProfile().getOrNull() ?: return
        val shouldPublishCollection = profile.isCollectionPublic
        val shouldPublishWishlist = profile.isWishlistPublic

        supabaseSyncRepository.deletePublicListings(firebaseUid)
            .getOrElse { throw it }

        if (!shouldPublishCollection && !shouldPublishWishlist) return

        val listings = dao.getAllCarsWithMasterList()
            .map { it.toDomain() }
            .filter { car ->
                (shouldPublishCollection && !car.isWishlist) || (shouldPublishWishlist && car.isWishlist)
            }
            .distinctBy { car -> car.id }

        listings.forEach { car ->
            val title = listOfNotNull(
                car.masterData?.brand?.displayName ?: car.manualBrand?.displayName,
                car.masterData?.modelName ?: car.manualModelName
            ).joinToString(" ").ifBlank { "BaseHW" }
            val imageUrl = car.backupPhotoUrl ?: car.userPhotoUrl
            val listingId = if (car.isWishlist) "wishlist_${car.id}" else "collection_${car.id}"
            supabaseSyncRepository.publishListing(
                firebaseUid = firebaseUid,
                listingId = listingId,
                title = title,
                imageUrl = imageUrl
            ).getOrElse { throw it }
        }
    }

    override suspend fun syncFromSupabase() {
        val user = authRepository.currentUser ?: return
        val payload = supabaseSyncRepository.fetchCollectionSnapshot(user.uid)
            .getOrElse { throw it }
            ?: return

        val snapshot = snapshotJson.decodeFromString<CollectionSnapshotPayload>(payload)

        // Full restore for deterministic state.
        dao.deleteAll()
        customCollectionDao.clearAllCrossRefs()
        customCollectionDao.deleteAllCollections()

        if (snapshot.currency.isNotBlank()) {
            appSettingsManager.setCurrency(snapshot.currency)
        }

        val oldToNewCarIds = mutableMapOf<Long, Long>()
        snapshot.cars.forEach { row ->
            val resolvedMasterDataId = resolveMasterDataId(row)
            val newId = dao.insert(row.toEntity(resolvedMasterDataId))
            oldToNewCarIds[row.localId] = newId
        }

        val oldToNewFolderIds = mutableMapOf<Long, Long>()
        snapshot.folders.forEach { row ->
            val newId = customCollectionDao.insertCollection(row.toEntity())
            oldToNewFolderIds[row.localId] = newId
        }

        snapshot.mappings.forEach { row ->
            val newFolderId = oldToNewFolderIds[row.collectionLocalId] ?: return@forEach
            val newCarId = oldToNewCarIds[row.carLocalId] ?: return@forEach
            customCollectionDao.insertCrossRef(
                CollectionCarCrossRef(
                    collectionId = newFolderId,
                    userCarId = newCarId
                )
            )
        }
        refreshVariantHuntCompletion()
    }

    override suspend fun mergeFromSupabase() {
        val user = authRepository.currentUser ?: return
        val payload = supabaseSyncRepository.fetchCollectionSnapshot(user.uid)
            .getOrElse { throw it }
            ?: return

        val snapshot = snapshotJson.decodeFromString<CollectionSnapshotPayload>(payload)

        if (snapshot.currency.isNotBlank()) {
            appSettingsManager.setCurrency(snapshot.currency)
        }

        val existingKeys = dao.getAllCarsWithMasterList()
            .map { it.car }
            .map { buildMergeKey(it) }
            .toMutableSet()

        snapshot.cars.forEach { row ->
            val resolvedMasterDataId = resolveMasterDataId(row)
            val candidate = row.toEntity(resolvedMasterDataId)
            val key = buildMergeKey(candidate)
            if (existingKeys.add(key)) {
                dao.insert(candidate)
            }
        }
        refreshVariantHuntCompletion()
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

            // 2) Delete Supabase collection snapshot.
            val uid = authRepository.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                // Remove public collection mirrors along with backup snapshot.
                supabaseSyncRepository.deletePublicListings(uid)
                supabaseSyncRepository.deleteCollectionSnapshot(uid)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearLocalData() {
        dao.deleteAll()
        customCollectionDao.clearAllCrossRefs()
        customCollectionDao.deleteAllCollections()
        refreshVariantHuntCompletion()
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
            list.map { BoxStatusStats(condition = it.condition, count = it.count) }
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
                dao.insert(entity)
                syncSnapshotToSupabaseIfSignedIn()
            }
        }
        refreshVariantHuntCompletion()
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

    override fun getAllCarsWithMasterListFlow(): Flow<List<UserCar>> {
        return dao.getAllWithMasterListFlow().map { list -> list.map { it.toDomain() } }
    }


    override fun getRankCars(): Flow<List<RankCarInput>> {
        return dao.getRankCarRows().map { rows ->
            rows.map { row ->
                RankCarInput(
                    brand = row.brand?.let { raw ->
                        try {
                            Brand.valueOf(raw)
                        } catch (_: Exception) {
                            null
                        }
                    },
                    feature = row.feature,
                    condition = VehicleCondition.fromString(row.condition),
                    isPremium = row.isPremium == true,
                    isCustom = row.isCustom,
                    quantity = row.quantity
                )
            }
        }
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

    private suspend fun resolveMasterDataId(row: SnapshotCar): Long? {
        val brand = row.masterBrand ?: return null
        val modelName = row.masterModelName ?: return null

        if (!row.masterToyNum.isNullOrBlank()) {
            val byToy = masterDataDao.getByToyNumGlobal(brand, row.masterToyNum)
            val bestToyMatch = byToy.firstOrNull { candidate ->
                val sourceMatches = row.masterDataSource.isNullOrBlank() || candidate.dataSource == row.masterDataSource
                val seriesMatches = row.masterSeries.isNullOrBlank() || candidate.series == row.masterSeries
                val yearMatches = row.masterYear == null || candidate.year == row.masterYear
                sourceMatches && seriesMatches && yearMatches
            } ?: byToy.firstOrNull()
            if (bestToyMatch != null) return bestToyMatch.id
        }

        if (!row.masterDataSource.isNullOrBlank()) {
            val byIdentityWithSource = masterDataDao.getByIdentity(
                brand = brand,
                modelName = modelName,
                year = row.masterYear,
                dataSource = row.masterDataSource
            )
            if (byIdentityWithSource != null) return byIdentityWithSource.id
        }

        if (row.masterYear != null) {
            val byIdentityGlobal = masterDataDao.getByIdentityGlobal(brand, modelName, row.masterYear)
            val bestIdentityMatch = byIdentityGlobal.firstOrNull { candidate ->
                row.masterSeries.isNullOrBlank() || candidate.series == row.masterSeries
            } ?: byIdentityGlobal.firstOrNull()
            if (bestIdentityMatch != null) return bestIdentityMatch.id

            val simpleId = masterDataDao.getIdByIdentity(brand, modelName, row.masterYear)
            if (simpleId != null) return simpleId
        }

        return null
    }

    private suspend fun deletePhotoFromSupabaseIfNeeded(userPhotoUrl: String?) {
        val url = userPhotoUrl ?: return
        if (!url.contains("/storage/v1/object/public/")) return
        supabaseStorageDataSource.deleteByPublicUrl(url)
    }

    private fun buildMergeKey(car: UserCarEntity): String {
        val mainPhoto = car.backupPhotoUrl ?: car.userPhotoUrl
        return listOf(
            car.masterDataId?.toString().orEmpty(),
            car.manualBrand.orEmpty().trim().lowercase(),
            car.manualModelName.orEmpty().trim().lowercase(),
            car.manualSeries.orEmpty().trim().lowercase(),
            car.manualSeriesNum.orEmpty().trim().lowercase(),
            car.manualYear?.toString().orEmpty(),
            car.condition,
            car.isWishlist.toString(),
            car.isSeriesOnly.toString(),
            car.isCustom.toString(),
            car.quantity.toString(),
            car.hwCardType.orEmpty(),
            car.purchaseDateMillis?.toString().orEmpty(),
            car.personalNote.trim().lowercase(),
            car.storageLocation.trim().lowercase(),
            mainPhoto.orEmpty().trim().lowercase()
        ).joinToString("|")
    }

    private fun isRemoteUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }

    override suspend fun hasCloudData(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val user = authRepository.currentUser ?: return@withContext false
        supabaseSyncRepository.hasCollectionSnapshot(user.uid).getOrDefault(false)
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
    override fun triggerSync() {
        syncSnapshotToSupabaseIfSignedIn()
    }

    private fun syncSnapshotToSupabaseIfSignedIn() {
        val user = authRepository.currentUser
        if (user != null && user.isEmailVerified) {

        } else if (user != null && !user.isEmailVerified) {
            android.util.Log.w(TAG, "Auto-sync skipped: Email not verified for ${user.uid}")
            return
        } else {
            return
        }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<CollectionSyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            CollectionSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        private const val TAG = "UserCarRepo"
        private const val VARIANT_HUNT_MAX_MATCHES = 500

        private val VARIANT_HUNT_STOPWORDS = setOf(
            "the", "a", "an", "and", "or", "of", "in", "on", "for", "to", "with", "by", "from", "as", "at",
            "ve", "ile", "bir", "bu", "da", "de", "mi", "mu", "i̇le"
        )

        /**
         * modelName başındaki üretici adı (ör. «Audi») zaten `brand = HOT_WHEELS` ile sınırlı;
         * ayrıca model adında «Audi» geçmeyen (sadece seri/modelde «Avant RS2») satırları yanlışlıkla eler.
         */
        private val VARIANT_HUNT_LEADING_CARMAKER_TOKENS = setOf(
            "acura", "alfa", "aston", "audi", "bentley", "bmw", "bugatti", "buick", "cadillac", "chevrolet",
            "chevy", "chrysler", "citroen", "dodge", "ferrari", "fiat", "ford", "genesis", "gmc", "honda",
            "hyundai", "infiniti", "jaguar", "jeep", "kia", "lamborghini", "lancia", "land", "lexus", "lincoln",
            "lotus", "maserati", "mazda", "mclaren", "mercedes", "mercedesbenz", "mini", "mitsubishi", "nissan",
            "oldsmobile", "opel", "pagani", "peugeot", "plymouth", "polestar", "pontiac", "porsche", "ram",
            "renault", "rolls", "rover", "saab", "saturn", "scion", "seat", "skoda", "smart", "subaru", "suzuki",
            "tesla", "toyota", "triumph", "volkswagen", "vw", "volvo", "romeo"
        )
    }

    @Serializable
    private data class CollectionSnapshotPayload(
        val currency: String,
        val cars: List<SnapshotCar>,
        val folders: List<SnapshotFolder>,
        val mappings: List<SnapshotMapping>
    )

    @Serializable
    private data class SnapshotCar(
        val localId: Long,
        val masterDataId: Long?,
        val masterBrand: String? = null,
        val masterFeature: String? = null,
        val masterModelName: String? = null,
        val masterYear: Int? = null,
        val masterSeries: String? = null,
        val masterToyNum: String? = null,
        val masterDataSource: String? = null,
        val masterImageUrl: String? = null,
        val manualModelName: String? = null,
        val manualBrand: String? = null,
        val manualSeries: String? = null,
        val manualSeriesNum: String? = null,
        val manualYear: Int? = null,
        val manualScale: String? = null,
        val manualIsPremium: Boolean? = null,
        val feature: String? = null,
        val condition: String = "MINT",
        val purchaseDateMillis: Long? = null,
        val personalNote: String = "",
        val storageLocation: String = "",
        val firestoreId: String = "",
        val isWishlist: Boolean = false,
        val userPhotoUrl: String? = null,
        val backupPhotoUrl: String? = null,
        val purchasePrice: Double? = null,
        val estimatedValue: Double? = null,
        val isFavorite: Boolean = false,
        val isSeriesOnly: Boolean = false,
        val isCustom: Boolean = false,
        val quantity: Int = 1,
        val additionalPhotos: List<String> = emptyList(),
        val additionalPhotosBackup: List<String> = emptyList(),
        val hwCardType: String? = null
    ) {
        private fun isRemote(url: String?): Boolean {
            return !url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://"))
        }

        fun toEntity(resolvedMasterDataId: Long?): UserCarEntity {
            val normalizedMain = when {
                isRemote(backupPhotoUrl) -> backupPhotoUrl
                isRemote(userPhotoUrl) -> userPhotoUrl
                else -> null
            }
            val normalizedAdditional = (additionalPhotosBackup + additionalPhotos)
                .filter { isRemote(it) }
                .distinct()

            return UserCarEntity(
            masterDataId = resolvedMasterDataId ?: masterDataId,
            manualModelName = manualModelName,
            manualBrand = manualBrand,
            manualSeries = manualSeries,
            manualSeriesNum = manualSeriesNum,
            manualYear = manualYear,
            manualScale = manualScale,
            manualIsPremium = manualIsPremium,
            condition = condition,
            purchaseDateMillis = purchaseDateMillis,
            personalNote = personalNote,
            storageLocation = storageLocation,
            firestoreId = firestoreId,
            isWishlist = isWishlist,
            userPhotoUrl = normalizedMain,
            backupPhotoUrl = backupPhotoUrl.takeIf { isRemote(it) },
            purchasePrice = purchasePrice,
            estimatedValue = estimatedValue,
            isFavorite = isFavorite,
            isSeriesOnly = isSeriesOnly,
            isCustom = isCustom,
            quantity = quantity,
            additionalPhotos = normalizedAdditional,
            additionalPhotosBackup = normalizedAdditional,
            hwCardType = hwCardType
        )
        }
    }

    @Serializable
    private data class SnapshotFolder(
        val localId: Long,
        val name: String,
        val description: String = "",
        val coverPhotoUrl: String? = null,
        val firestoreId: String = "",
        val createdAtMillis: Long
    ) {
        fun toEntity(): CustomCollectionEntity = CustomCollectionEntity(
            name = name,
            description = description,
            coverPhotoUrl = coverPhotoUrl,
            firestoreId = firestoreId,
            createdAtMillis = createdAtMillis
        )

        companion object {
            fun fromEntity(entity: CustomCollectionEntity): SnapshotFolder = SnapshotFolder(
                localId = entity.id,
                name = entity.name,
                description = entity.description,
                coverPhotoUrl = entity.coverPhotoUrl,
                firestoreId = entity.firestoreId,
                createdAtMillis = entity.createdAtMillis
            )
        }
    }

    override suspend fun clearAllCars(): Result<Unit> = runCatching {
        dao.deleteAll()
        refreshVariantHuntCompletion()
    }

    override suspend fun importCollection(
        inputStream: InputStream,
        mimeTypeHint: String?,
        mode: CollectionImportMode,
        conversionRate: Double
    ): Result<CollectionImportStats> = withContext(Dispatchers.IO) {
        runCatching {
            val (rows, parseWarn) = CollectionImportHelper.parseRows(inputStream, mimeTypeHint, conversionRate)
            if (mode == CollectionImportMode.REPLACE) {
                clearLocalData()
            }
            val existingKeys = if (mode == CollectionImportMode.MERGE) {
                dao.getAllCarsWithMasterList().map { buildMergeKey(it.car) }.toMutableSet()
            } else {
                mutableSetOf()
            }
            var added = 0
            var skippedDup = 0
            var skippedIncomplete = 0
            for (row in rows) {
                val masterId = resolveImportMasterId(row)
                if (!CollectionImportHelper.isRowImportable(row, masterId != null)) {
                    skippedIncomplete++
                    continue
                }
                val entity = importRowToEntity(row, masterId)
                val key = buildMergeKey(entity)
                if (existingKeys.contains(key)) {
                    skippedDup++
                    continue
                }
                dao.insert(entity)
                existingKeys.add(key)
                added++
            }
            refreshVariantHuntCompletion()
            CollectionImportStats(
                added = added,
                skippedDuplicates = skippedDup,
                skippedIncomplete = skippedIncomplete,
                parseFailures = parseWarn
            )
        }
    }

    override fun observeActiveVariantHuntGroups(): Flow<List<VariantHuntGroupSummary>> =
        variantHuntDao.observeActiveGroups().map { list ->
            list.map { e ->
                VariantHuntGroupSummary(
                    id = e.id,
                    brandCode = e.brand,
                    title = e.title,
                    keywords = decodeKeywordsDelimited(e.keywordsDelimited),
                    createdAtMillis = e.createdAtMillis
                )
            }
        }

    override fun observeVariantHuntGroupRows(groupId: Long): Flow<List<VariantHuntMasterRow>> =
        variantHuntDao.observeGroupItemRows(groupId).map { rows ->
            rows.map { r ->
                VariantHuntMasterRow(
                    masterDataId = r.master.id,
                    modelName = r.master.modelName,
                    year = r.master.year,
                    series = r.master.series,
                    seriesNum = r.master.seriesNum,
                    toyNum = r.master.toyNum,
                    imageUrl = r.master.imageUrl,
                    inCollection = r.inCollection != 0
                )
            }
        }

    override suspend fun proposeVariantHuntKeywords(seedMasterDataId: Long): List<String> {
        val master = masterDataDao.getById(seedMasterDataId) ?: return emptyList()
        val tokens = tokenizeModelName(master.modelName)
        val withoutLeadingMake = tokens.dropWhile { it in VARIANT_HUNT_LEADING_CARMAKER_TOKENS }
        return withoutLeadingMake.ifEmpty { tokens }
    }

    override suspend fun countVariantHuntMatches(brand: String, keywords: List<String>): Int =
        withContext(Dispatchers.IO) {
            masterDataDao.rawSelectMasters(buildKeywordMatchQuery(brand, keywords, null)).size
        }

    override suspend fun createVariantHuntFromKeywords(
        seedMasterDataId: Long,
        seedUserCarId: Long?,
        keywords: List<String>
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val seed = masterDataDao.getById(seedMasterDataId)
                ?: error("seed_missing")
            val normalized = normalizeKeywordList(keywords)
            require(normalized.isNotEmpty()) { "keywords_empty" }
            val matches = masterDataDao.rawSelectMasters(
                buildKeywordMatchQuery(seed.brand, normalized, VARIANT_HUNT_MAX_MATCHES + 1)
            )
            require(matches.isNotEmpty()) { "no_matches" }
            require(matches.size <= VARIANT_HUNT_MAX_MATCHES) { "too_many" }
            val now = System.currentTimeMillis()
            val group = VariantHuntGroupEntity(
                brand = seed.brand,
                title = seed.modelName.take(120),
                seedMasterDataId = seed.id,
                seedUserCarId = seedUserCarId,
                keywordsDelimited = encodeKeywordsDelimited(normalized),
                createdAtMillis = now,
                completedAtMillis = null,
                isActive = true
            )
            val id = variantHuntDao.insertGroupWithItems(group, matches.map { it.id })
            refreshVariantHuntCompletion()
            id
        }
    }

    override suspend fun deleteVariantHuntGroup(groupId: Long) {
        withContext(Dispatchers.IO) {
            variantHuntDao.deleteGroup(groupId)
        }
    }

    override suspend fun refreshVariantHuntCompletion() {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            variantHuntDao.findGroupIdsReadyToComplete().forEach { gid ->
                variantHuntDao.markGroupCompleted(gid, now)
            }
        }
    }

    private fun encodeKeywordsDelimited(keywords: List<String>): String =
        keywords.map { it.replace("\n", " ").trim() }.filter { it.isNotEmpty() }.joinToString("\n")

    private fun decodeKeywordsDelimited(raw: String): List<String> =
        raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    private fun normalizeKeywordList(keywords: List<String>): List<String> {
        val cleaned = keywords.map { sanitizeLikeToken(it) }.filter { it.isNotEmpty() }.distinct()
        val dropped = cleaned.dropWhile { it in VARIANT_HUNT_LEADING_CARMAKER_TOKENS }
        return dropped.ifEmpty { cleaned }
    }

    private fun sanitizeLikeToken(s: String): String =
        s.lowercase().trim().replace("%", "").replace("_", "").trim()
            .filter { !it.isISOControl() }

    private fun tokenizeModelName(modelName: String): List<String> =
        modelName.lowercase()
            .replace("'", " ")
            .split(Regex("[^a-z0-9]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .filter { it !in VARIANT_HUNT_STOPWORDS }
            .distinct()
            .take(8)

    private fun buildKeywordMatchQuery(brand: String, keywords: List<String>, limit: Int?): SimpleSQLiteQuery {
        val args = mutableListOf<Any?>()
        val sb = StringBuilder("SELECT * FROM master_data WHERE brand = ? ")
        args.add(brand)
        val safe = keywords.map { sanitizeLikeToken(it) }.filter { it.isNotEmpty() }
        if (safe.isEmpty()) {
            sb.append(" AND 0 ")
        } else {
            // Katalog aramasıyla uyum: aynı kelime modelName, series, toyNum veya colNum içinde geçebilir.
            for (k in safe) {
                val pattern = "%$k%"
                sb.append(
                    " AND (lower(modelName) LIKE ? OR lower(series) LIKE ? OR lower(toyNum) LIKE ? OR lower(ifnull(colNum, '')) LIKE ?)"
                )
                repeat(4) { args.add(pattern) }
            }
        }
        sb.append(" ORDER BY CASE WHEN year IS NULL THEN 1 ELSE 0 END, year ASC, series ASC, seriesNum ASC ")
        if (limit != null) {
            sb.append(" LIMIT ").append(limit.coerceIn(1, 10_000))
        }
        return SimpleSQLiteQuery(sb.toString(), args.toTypedArray())
    }

    private suspend fun resolveImportMasterId(row: CollectionImportHelper.ImportRow): Long? {
        val brand = row.brandCode
        if (!row.toyNum.isNullOrBlank()) {
            val byToy = masterDataDao.getByToyNumGlobal(brand, row.toyNum)
            val bestToy = byToy.firstOrNull { c ->
                row.series.isNullOrBlank() || c.series == row.series
            } ?: byToy.firstOrNull()
            if (bestToy != null) return bestToy.id
        }
        val modelName = row.modelName?.takeIf { it.isNotBlank() } ?: return null
        if (row.year != null) {
            val byIdentityGlobal = masterDataDao.getByIdentityGlobal(brand, modelName, row.year)
            val bestIdentity = byIdentityGlobal.firstOrNull { c ->
                row.series.isNullOrBlank() || c.series == row.series
            } ?: byIdentityGlobal.firstOrNull()
            if (bestIdentity != null) return bestIdentity.id
            val simpleId = masterDataDao.getIdByIdentity(brand, modelName, row.year)
            if (simpleId != null) return simpleId
        }
        return null
    }

    private fun importRowToEntity(row: CollectionImportHelper.ImportRow, masterId: Long?): UserCarEntity {
        val manualModel = if (masterId == null) {
            row.modelName?.takeIf { it.isNotBlank() }
                ?: row.toyNum?.takeIf { it.isNotBlank() }
                ?: "?"
        } else {
            null
        }
        return UserCarEntity(
            masterDataId = masterId,
            manualModelName = manualModel,
            manualBrand = if (masterId == null) row.brandCode else null,
            manualSeries = if (masterId == null) row.series else null,
            manualSeriesNum = if (masterId == null) row.seriesNum else null,
            manualYear = if (masterId == null) row.year else null,
            manualScale = null,
            manualIsPremium = null,
            condition = row.conditionName,
            purchaseDateMillis = null,
            personalNote = row.personalNote,
            storageLocation = row.storageLocation,
            firestoreId = "",
            isWishlist = row.isWishlist,
            userPhotoUrl = null,
            backupPhotoUrl = null,
            purchasePrice = row.purchasePriceBase,
            estimatedValue = row.estimatedValueBase,
            isFavorite = row.isFavorite,
            isSeriesOnly = false,
            isCustom = row.isCustom,
            quantity = row.quantity,
            additionalPhotos = emptyList(),
            additionalPhotosBackup = emptyList()
        )
    }

    @Serializable
    private data class SnapshotMapping(
        val collectionLocalId: Long,
        val carLocalId: Long
    )
}
