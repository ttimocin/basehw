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
        private const val SYNC_PROVIDER = "sync_provider"
        private const val SYNC_BASE_URL = "sync_base_url"
        private const val SYNC_EDGE_URL = "sync_edge_url"
        private const val SYNC_EDGE_API_KEY = "sync_edge_api_key"
        private const val SYNC_INTERVAL_DAYS = "sync_interval_days"
        private const val PHOTO_BACKUP_ENABLED = "photo_backup_enabled"
        private const val PHOTO_BACKUP_SUPABASE_URL = "photo_backup_supabase_url"
        private const val PHOTO_BACKUP_API_KEY = "photo_backup_api_key"
        private const val PHOTO_BACKUP_BUCKET = "photo_backup_bucket"
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

    fun getSyncProvider(): String {
        return remoteConfig.getString(SYNC_PROVIDER)
    }

    fun getSyncEdgeUrl(): String {
        return remoteConfig.getString(SYNC_EDGE_URL)
    }

    fun getSyncEdgeApiKey(): String {
        return remoteConfig.getString(SYNC_EDGE_API_KEY)
    }

    fun getSyncIntervalDays(): Long {
        return remoteConfig.getLong(SYNC_INTERVAL_DAYS)
    }

    fun isPhotoBackupEnabled(): Boolean {
        return remoteConfig.getBoolean(PHOTO_BACKUP_ENABLED)
    }

    fun getPhotoBackupSupabaseUrl(): String {
        return remoteConfig.getString(PHOTO_BACKUP_SUPABASE_URL)
    }

    fun getPhotoBackupApiKey(): String {
        return remoteConfig.getString(PHOTO_BACKUP_API_KEY)
    }

    fun getPhotoBackupBucket(): String {
        return remoteConfig.getString(PHOTO_BACKUP_BUCKET)
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
