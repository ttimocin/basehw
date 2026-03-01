package com.taytek.basehw.data.mapper

import com.taytek.basehw.data.local.entity.MasterDataEntity
import com.taytek.basehw.data.local.entity.UserCarEntity
import com.taytek.basehw.data.local.entity.UserCarWithMaster
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
    colNum = colNum
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
    colNum = colNum
)

// UserCar mappers
fun UserCarEntity.toDomain(masterData: MasterData?): UserCar = UserCar(
    id = id,
    masterDataId = masterDataId,
    masterData = masterData,
    isOpened = isOpened,
    purchaseDate = purchaseDateMillis?.let { Date(it) },
    personalNote = personalNote,
    storageLocation = storageLocation,
    isWishlist = isWishlist,
    firestoreId = firestoreId
)

fun UserCarWithMaster.toDomain(): UserCar = UserCar(
    id = car.id,
    masterDataId = car.masterDataId,
    masterData = master?.toDomain(),
    isOpened = car.isOpened,
    purchaseDate = car.purchaseDateMillis?.let { Date(it) },
    personalNote = car.personalNote,
    storageLocation = car.storageLocation,
    isWishlist = car.isWishlist,
    firestoreId = car.firestoreId
)

fun UserCar.toEntity(): UserCarEntity = UserCarEntity(
    id = id,
    masterDataId = masterDataId,
    isOpened = isOpened,
    purchaseDateMillis = purchaseDate?.time,
    personalNote = personalNote,
    storageLocation = storageLocation,
    isWishlist = isWishlist,
    firestoreId = firestoreId
)
