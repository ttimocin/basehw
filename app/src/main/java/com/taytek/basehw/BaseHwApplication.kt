package com.taytek.basehw

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.taytek.basehw.data.worker.AssetSeedWorker
import com.taytek.basehw.data.worker.RemoteYearSyncWorker
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class BaseHwApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var remoteConfig: com.taytek.basehw.data.remote.firebase.RemoteConfigDataSource

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader = imageLoader

    override fun onCreate() {
        super.onCreate()
        
        // Create notification channel for sync workers
        createSyncNotificationChannel()

        // Fetch latest configuration at startup
        applicationScope.launch {
            remoteConfig.fetchAndActivate()
        }
        
        scheduleAssetSeed()
        scheduleRemoteYearSync()
    }

    /**
     * Import bundled JSON assets on first install (KEEP = runs only once).
     * Re-runs if user manually triggers "Veriyi Yenile" in Settings.
     */
    private fun scheduleAssetSeed() {
        val request = OneTimeWorkRequestBuilder<AssetSeedWorker>()
            .addTag(AssetSeedWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            AssetSeedWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Fetch current year's JSON from GitHub every week.
     * New cars added to the remote JSON appear in the app without any app update.
     * KEEP = won't reset the timer if already scheduled.
     */
    private fun scheduleRemoteYearSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<RemoteYearSyncWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .addTag(RemoteYearSyncWorker.WORK_NAME)
            // Also run immediately on first scheduling
            .setInitialDelay(0, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RemoteYearSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun createSyncNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Sistem Senkronizasyonu"
            val descriptionText = "Yedekleme ve katalog guncelleme islemleri"
            val importance = android.app.NotificationManager.IMPORTANCE_LOW
            val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "sync_channel"
    }
}
