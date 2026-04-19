package com.taytek.basehw.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taytek.basehw.BaseHwApplication
import com.taytek.basehw.domain.repository.UserCarRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import androidx.core.app.NotificationCompat
import com.taytek.basehw.R

@HiltWorker
class CollectionSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userCarRepository: UserCarRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        
        // Ensure Auth state is initialized (especially if process just started)
        delay(1000) 
        
        return try {
            userCarRepository.syncToSupabase()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background collection sync FAILED", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, BaseHwApplication.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Veri Senkronizasyonu")
            .setContentText("Koleksiyonunuz buluta yedekleniyor...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        return androidx.work.ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "CollectionSyncWorker"
        private const val NOTIFICATION_ID = 99
        const val WORK_NAME = "collection_full_sync"
    }
}
