package com.taytek.basehw.data.remote.firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.taytek.basehw.R
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigDataSource @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    companion object {
        private const val SYNC_BASE_URL = "sync_base_url"
    }

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // Fetch every hour
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false
        }
    }

    fun getSyncBaseUrl(): String {
        return remoteConfig.getString(SYNC_BASE_URL)
    }
}
