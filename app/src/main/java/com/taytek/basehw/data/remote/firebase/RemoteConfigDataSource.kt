package com.taytek.basehw.data.remote.firebase

import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.taytek.basehw.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        private const val MIN_VERSION_NAME = "min_version_name"
        private const val LATEST_VERSION_NAME = "latest_version_name"
        private const val UPDATE_URL = "update_url"
    }

    private val _configUpdated = MutableStateFlow(System.currentTimeMillis())
    val configUpdated: StateFlow<Long> = _configUpdated.asStateFlow()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // Set to 0 for real-time testing
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        // Real-time updates
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener {
                    _configUpdated.value = System.currentTimeMillis()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                // Handle error
            }
        })
    }

    suspend fun fetchAndActivate(): Boolean {
        return try {
            val result = remoteConfig.fetchAndActivate().await()
            if (result) {
                _configUpdated.value = System.currentTimeMillis()
            }
            result
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

    fun getMinVersionName(): String {
        return remoteConfig.getString(MIN_VERSION_NAME)
    }

    fun getLatestVersionName(): String {
        return remoteConfig.getString(LATEST_VERSION_NAME)
    }

    fun getUpdateUrl(): String {
        return remoteConfig.getString(UPDATE_URL)
    }
}
