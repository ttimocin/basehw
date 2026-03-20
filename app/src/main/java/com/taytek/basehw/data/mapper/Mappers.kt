package com.taytek.basehw.data.mapper

import com.taytek.basehw.data.local.entity.MasterDataEntity
import com.taytek.basehw.data.local.entity.UserCarEntity
import com.taytek.basehw.data.local.entity.UserCarWithMaster
import com.taytek.basehw.data.local.entity.GroupedUserCarWithMaster
import com.taytek.basehw.data.local.entity.CustomCollectionEntity
import com.taytek.basehw.data.local.entity.CollectionWithCars
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.MasterData
import com.taytek.basehw.domain.model.UserCar
import java.util.Date

// MasterData mappers
fun MasterDataEntity.toDomain(): MasterData = MasterData(
    id = id,
    brand = Brand.valueOf(brand),
    modelName = modelName,
    series = series,
    seriesNum = seriesNum,
    year = year,
    color = color,
    imageUrl = imageUrl,
    scale = scale,
    toyNum = toyNum,
    colNum = colNum,
    isPremium = isPremium,
    dataSource = dataSource,
    caseNum = caseNum,
    feature = feature
)

fun MasterData.toEntity(): MasterDataEntity = MasterDataEntity(
    id = id,
    brand = brand.name,
    modelName = modelName,
    series = series,
    seriesNum = seriesNum,
    year = year,
    color = color,
    imageUrl = imageUrl,
    scale = scale,
    toyNum = toyNum,
    colNum = colNum,
    isPremium = isPremium,
    dataSource = dataSource,
    caseNum = caseNum,
    feature = feature
)

// UserCar mappers
fun UserCarEntity.toDomain(masterData: MasterData?): UserCar = UserCar(
    id = id,
    masterDataId = masterDataId,
    masterData = masterData,
    manualModelName = manualModelName,
    manualBrand = manualBrand?.let { try { Brand.valueOf(it) } catch(e: Exception) { null } },
    manualYear = manualYear,
    manualSeries = manualSeries,
    manualSeriesNum = manualSeriesNum,
    manualScale = manualScale,
    manualIsPremium = manualIsPremium,
    isOpened = isOpened,
    purchaseDate = purchaseDateMillis?.let { Date(it) },
    personalNote = personalNote,
    storageLocation = storageLocation,
    isWishlist = isWishlist,
    firestoreId = firestoreId,
    userPhotoUrl = userPhotoUrl,
    backupPhotoUrl = backupPhotoUrl,
    purchasePrice = purchasePrice,
    estimatedValue = estimatedValue,
    isFavorite = isFavorite,
    isSeriesOnly = isSeriesOnly,
    quantity = 1
)

fun UserCarWithMaster.toDomain(): UserCar = UserCar(
    id = car.id,
    masterDataId = car.masterDataId,
    masterData = master?.toDomain(),
    manualModelName = car.manualModelName,
    manualBrand = car.manualBrand?.let { try { Brand.valueOf(it) } catch(e: Exception) { null } },
    manualYear = car.manualYear,
    manualSeries = car.manualSeries,
    manualSeriesNum = car.manualSeriesNum,
    manualScale = car.manualScale,
    manualIsPremium = car.manualIsPremium,
    isOpened = car.isOpened,
    purchaseDate = car.purchaseDateMillis?.let { Date(it) },
    personalNote = car.personalNote,
    storageLocation = car.storageLocation,
    isWishlist = car.isWishlist,
    firestoreId = car.firestoreId,
    userPhotoUrl = car.userPhotoUrl,
    backupPhotoUrl = car.backupPhotoUrl,
    purchasePrice = car.purchasePrice,
    estimatedValue = car.estimatedValue,
    isFavorite = car.isFavorite,
    isSeriesOnly = car.isSeriesOnly
)

fun GroupedUserCarWithMaster.toDomain(): UserCar = data.toDomain().copy(
    quantity = quantity ?: 1
)

fun UserCar.toEntity(): UserCarEntity = UserCarEntity(
    id = id,
    masterDataId = masterDataId,
    manualModelName = manualModelName,
    manualBrand = manualBrand?.name,
    manualSeries = manualSeries,
    manualSeriesNum = manualSeriesNum,
    manualYear = manualYear,
    manualScale = manualScale,
    manualIsPremium = manualIsPremium,
    isOpened = isOpened,
    purchaseDateMillis = purchaseDate?.time,
    personalNote = personalNote,
    storageLocation = storageLocation,
    isWishlist = isWishlist,
    firestoreId = firestoreId,
    userPhotoUrl = userPhotoUrl,
    backupPhotoUrl = backupPhotoUrl,
    purchasePrice = purchasePrice,
    estimatedValue = estimatedValue,
    isFavorite = isFavorite,
    isSeriesOnly = isSeriesOnly
)

// Custom Collection mappers
fun CustomCollectionEntity.toDomain() = com.taytek.basehw.domain.model.CustomCollection(
    id = id,
    name = name,
    description = description,
    coverPhotoUrl = coverPhotoUrl,
    firestoreId = firestoreId,
    createdAt = java.util.Date(createdAtMillis)
)

fun CollectionWithCars.toDomain() = com.taytek.basehw.domain.model.CustomCollection(
    id = collection.id,
    name = collection.name,
    description = collection.description,
    coverPhotoUrl = collection.coverPhotoUrl,
    firestoreId = collection.firestoreId,
    createdAt = java.util.Date(collection.createdAtMillis),
    cars = cars.map { it.toDomain() }
)

fun com.taytek.basehw.domain.model.CustomCollection.toEntity() = CustomCollectionEntity(
    id = id,
    name = name,
    description = description,
    coverPhotoUrl = coverPhotoUrl,
    firestoreId = firestoreId,
    createdAtMillis = createdAt.time
)
