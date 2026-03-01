package com.taytek.basehw

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.taytek.basehw.data.worker.AssetSeedWorker
import com.taytek.basehw.data.worker.RemoteYearSyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class BaseHwApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var remoteConfig: com.taytek.basehw.data.remote.firebase.RemoteConfigDataSource

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
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
            ExistingWorkPolicy.KEEP,   // only run once to prevent database wipes
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
            repeatInterval = 7,
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
}
