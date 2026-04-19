package com.taytek.basehw.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taytek.basehw.data.local.CarPhotoLocalStore
import com.taytek.basehw.data.local.dao.UserCarDao
import com.taytek.basehw.data.remote.network.SupabaseStorageDataSource
import com.taytek.basehw.domain.repository.AuthRepository
import com.taytek.basehw.domain.repository.UserCarRepository
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
    private val userCarRepository: UserCarRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val carId = inputData.getLong(KEY_CAR_ID, -1L)
        if (carId <= 0L) return Result.failure()

        val user = authRepository.currentUser ?: return Result.success()
        if (!user.isEmailVerified) {
            Log.w(TAG, "Email not verified. Skipping photo backup for user ${user.uid}")
            return Result.success()
        }
        val userId = user.uid
        val wrapper = userCarDao.getByIdWithMaster(carId).firstOrNull() ?: return Result.success()
        // Main Photo Backup
        val mainLocalPhoto = wrapper.car.userPhotoUrl
        var mainRemoteUrl = wrapper.car.backupPhotoUrl
        var photosUploaded = false

        if (mainLocalPhoto != null && !mainLocalPhoto.startsWith("http")) {
            val uploadUri = carPhotoLocalStore.persistCompressed(mainLocalPhoto, carId)
                ?: return Result.retry()
            if (uploadUri != mainLocalPhoto) {
                userCarDao.updateUserPhotoUrl(carId, uploadUri)
            }
            mainRemoteUrl = supabaseStorageDataSource.uploadUserCarPhoto(userId, carId, uploadUri)
                ?: return Result.retry()
            userCarDao.updateBackupPhotoUrl(carId, mainRemoteUrl)
            photosUploaded = true

        }

        // Additional Photos Backup
        val currentAdditionalLocal = wrapper.car.additionalPhotos
        val currentAdditionalBackup = wrapper.car.additionalPhotosBackup.toMutableList()
        val newAdditionalLocal = currentAdditionalLocal.toMutableList()
        var changed = false

        for (i in currentAdditionalLocal.indices) {
            val photo = currentAdditionalLocal[i]
            if (!photo.startsWith("http")) {
                val compUri = carPhotoLocalStore.persistCompressed(photo, carId, "add_$i") 
                    ?: return Result.retry()
                if (compUri != photo) {
                    newAdditionalLocal[i] = compUri
                    changed = true
                }
                
                val remote = supabaseStorageDataSource.uploadUserCarPhoto(userId, carId, compUri, "add_$i")
                    ?: return Result.retry()
                
                if (i < currentAdditionalBackup.size) {
                    currentAdditionalBackup[i] = remote
                } else {
                    currentAdditionalBackup.add(remote)
                }
                changed = true
            }
        }

        if (changed) {
            userCarDao.updateAdditionalPhotos(carId, newAdditionalLocal)
            userCarDao.updateAdditionalPhotosBackup(carId, currentAdditionalBackup)
            photosUploaded = true

        }

        // After photos are uploaded, re-sync the collection snapshot so
        // the cloud backup contains the new remote photo URLs.
        if (photosUploaded) {

            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val request = androidx.work.OneTimeWorkRequestBuilder<CollectionSyncWorker>()
                .setConstraints(constraints)
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                CollectionSyncWorker.WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "PhotoBackupWorker"
        const val KEY_CAR_ID = "key_car_id"
        const val WORK_NAME_PREFIX = "photo_backup_"
    }
}
