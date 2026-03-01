package com.taytek.basehw.domain.repository

import androidx.paging.PagingData
import com.taytek.basehw.domain.model.UserCar
import com.taytek.basehw.domain.model.BoxStatusStats
import com.taytek.basehw.domain.model.BrandStats
import kotlinx.coroutines.flow.Flow

interface UserCarRepository {
    fun getCollection(): Flow<PagingData<UserCar>>
    fun getWishlist(): Flow<PagingData<UserCar>>
    suspend fun addCar(car: UserCar): Long
    suspend fun deleteCar(id: Long)
    suspend fun updateCar(car: UserCar)
    suspend fun syncToFirestore()
    suspend fun syncFromFirestore()
    fun getCarById(id: Long): Flow<UserCar?>

    fun getTotalCarsCount(): Flow<Int>
    fun getBoxStatusCounts(): Flow<List<BoxStatusStats>>
    fun getBrandCounts(): Flow<List<BrandStats>>
}
