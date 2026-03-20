package com.taytek.basehw.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taytek.basehw.data.local.CarPhotoLocalStore
import com.taytek.basehw.data.local.dao.UserCarDao
import com.taytek.basehw.data.remote.firebase.FirestoreDataSource
import com.taytek.basehw.data.remote.network.SupabaseStorageDataSource
import com.taytek.basehw.domain.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class PhotoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val carPhotoLocalStore: CarPhotoLocalStore,
    private val userCarDao: UserCarDao,
    private val authRepository: AuthRepository,
    private val supabaseStorageDataSource: SupabaseStorageDataSource,
    private val firestoreDataSource: FirestoreDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val carId = inputData.getLong(KEY_CAR_ID, -1L)
        if (carId <= 0L) return Result.failure()

        val userId = authRepository.currentUser?.uid ?: return Result.success()
        val wrapper = userCarDao.getByIdWithMaster(carId).firstOrNull() ?: return Result.success()
        val localPhoto = wrapper.car.userPhotoUrl ?: return Result.success()

        if (localPhoto.startsWith("http://") || localPhoto.startsWith("https://")) {
            return Result.success()
        }

        val uploadUri = carPhotoLocalStore.persistCompressed(localPhoto, carId)
            ?: return Result.retry()
        if (uploadUri != localPhoto) {
            userCarDao.updateUserPhotoUrl(carId, uploadUri)
        }

        val remoteUrl = supabaseStorageDataSource.uploadUserCarPhoto(userId, carId, uploadUri)
            ?: return Result.retry()

        userCarDao.updateBackupPhotoUrl(carId, remoteUrl)

        if (wrapper.car.firestoreId.isNotBlank()) {
            firestoreDataSource.updateCarBackupPhotoUrl(wrapper.car.firestoreId, remoteUrl)
        }
        return Result.success()
    }

    companion object {
        const val KEY_CAR_ID = "key_car_id"
        const val WORK_NAME_PREFIX = "photo_backup_"
    }
}
