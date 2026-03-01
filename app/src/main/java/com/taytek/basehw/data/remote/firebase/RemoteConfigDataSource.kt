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
        private const val SYNC_INTERVAL_DAYS = "sync_interval_days"
        private const val MIN_VERSION_CODE = "min_version_code"
        private const val LATEST_VERSION_CODE = "latest_version_code"
        private const val LATEST_VERSION_NAME = "latest_version_name"
        private const val UPDATE_URL = "update_url"
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

    fun getSyncIntervalDays(): Long {
        return remoteConfig.getLong(SYNC_INTERVAL_DAYS)
    }

    fun getMinVersionCode(): Int {
        return remoteConfig.getLong(MIN_VERSION_CODE).toInt()
    }

    fun getLatestVersionCode(): Int {
        return remoteConfig.getLong(LATEST_VERSION_CODE).toInt()
    }

    fun getLatestVersionName(): String {
        return remoteConfig.getString(LATEST_VERSION_NAME)
    }

    fun getUpdateUrl(): String {
        return remoteConfig.getString(UPDATE_URL)
    }
}
