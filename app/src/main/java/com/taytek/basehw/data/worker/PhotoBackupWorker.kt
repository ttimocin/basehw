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
        // Main Photo Backup
        val mainLocalPhoto = wrapper.car.userPhotoUrl
        var mainRemoteUrl = wrapper.car.backupPhotoUrl

        if (mainLocalPhoto != null && !mainLocalPhoto.startsWith("http")) {
            val uploadUri = carPhotoLocalStore.persistCompressed(mainLocalPhoto, carId)
                ?: return Result.retry()
            if (uploadUri != mainLocalPhoto) {
                userCarDao.updateUserPhotoUrl(carId, uploadUri)
            }
            mainRemoteUrl = supabaseStorageDataSource.uploadUserCarPhoto(userId, carId, uploadUri)
                ?: return Result.retry()
            userCarDao.updateBackupPhotoUrl(carId, mainRemoteUrl)
        }

        // Additional Photos Backup
        val currentAdditionalLocal = wrapper.car.additionalPhotos
        val currentAdditionalBackup = wrapper.car.additionalPhotosBackup.toMutableList()
        val newAdditionalLocal = currentAdditionalLocal.toMutableList()
        var changed = false

        for (i in currentAdditionalLocal.indices) {
            val photo = currentAdditionalLocal[i]
            if (!photo.startsWith("http")) {
                // To avoid re-uploading if we already have it in backup list at same index?
                // Actually simpler: if it's local, upload and update list.
                val compUri = carPhotoLocalStore.persistCompressed(photo, carId, "add_$i") 
                    ?: return Result.retry()
                if (compUri != photo) {
                    newAdditionalLocal[i] = compUri
                    changed = true
                }
                
                val remote = supabaseStorageDataSource.uploadUserCarPhoto(userId, carId, compUri, "add_$i")
                    ?: return Result.retry()
                
                // Track remote URLs in additionalPhotosBackup
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
        }

        // Sync to Firestore if needed
        if (wrapper.car.firestoreId.isNotBlank()) {
            if (mainRemoteUrl != null) {
                firestoreDataSource.updateCarBackupPhotoUrl(wrapper.car.firestoreId, mainRemoteUrl)
            }
            if (changed) {
                firestoreDataSource.updateAdditionalPhotosBackup(wrapper.car.firestoreId, currentAdditionalBackup)
            }
        }
        return Result.success()
    }

    companion object {
        const val KEY_CAR_ID = "key_car_id"
        const val WORK_NAME_PREFIX = "photo_backup_"
    }
}
