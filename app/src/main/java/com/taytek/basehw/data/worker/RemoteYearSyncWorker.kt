package com.taytek.basehw.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.local.entity.MasterDataEntity
import com.taytek.basehw.data.remote.dto.HotWheelsCarDto
import com.taytek.basehw.domain.model.Brand
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.taytek.basehw.BuildConfig
import com.taytek.basehw.data.local.AppSettingsManager
import com.taytek.basehw.data.remote.firebase.RemoteConfigDataSource
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder

/**
 * Fetches the brand-specific update JSON from a remote URL (e.g. GitHub raw).
 * Runs once immediately, then repeats weekly via PeriodicWorkRequest.
 *
 * New cars added to the remote JSON appear in the app within a week —
 * NO app update required.
 *
 * Remote URL format:
 *   https://raw.githubusercontent.com/{USER}/{REPO}/main/database/{brand}/update.json
 *
 * Configure REMOTE_BASE_URL below to point to your GitHub repo.
 */
@HiltWorker
class RemoteYearSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val masterDataDao: MasterDataDao,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val remoteConfig: RemoteConfigDataSource,
    private val appSettingsManager: AppSettingsManager,
    private val firebaseAuth: FirebaseAuth
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "RemoteYearSync"
        const val WORK_NAME = "hw_remote_year_sync_v3"
    }

    @androidx.annotation.Keep
    private data class SupabaseEdgeResponse(
        @com.google.gson.annotations.SerializedName("cursor") val cursor: String? = null,
        @com.google.gson.annotations.SerializedName("records") val records: List<SupabaseEdgeCarDto> = emptyList()
    )

    @androidx.annotation.Keep
    private data class SupabaseEdgeCarDto(
        @com.google.gson.annotations.SerializedName("brand") val brand: String = "",
        @com.google.gson.annotations.SerializedName("modelName") val modelName: String = "",
        @com.google.gson.annotations.SerializedName("series") val series: String = "",
        @com.google.gson.annotations.SerializedName("seriesNum") val seriesNum: String = "",
        @com.google.gson.annotations.SerializedName("year") val year: String? = null,
        @com.google.gson.annotations.SerializedName("color") val color: String = "",
        @com.google.gson.annotations.SerializedName("imageUrl") val imageUrl: String = "",
        @com.google.gson.annotations.SerializedName("scale") val scale: String = "1:64",
        @com.google.gson.annotations.SerializedName("toyNum") val toyNum: String = "",
        @com.google.gson.annotations.SerializedName("colNum") val colNum: String = "",
        @com.google.gson.annotations.SerializedName("dataSource") val dataSource: String = "",
        @com.google.gson.annotations.SerializedName("caseNum") val caseNum: String = "",
        @com.google.gson.annotations.SerializedName("feature") val feature: String? = null,
        @com.google.gson.annotations.SerializedName("category") val category: String? = null
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Refresh remote config before starting sync
        remoteConfig.fetchAndActivate()
        val edgeUrl = remoteConfig.getSyncEdgeUrl()
        val apiKey = remoteConfig.getSyncEdgeApiKey()

        if (edgeUrl.isBlank()) {
            Log.w(TAG, "Supabase edge URL is blank; sync skipped")
            return@withContext Result.failure()
        }
        Log.d(TAG, "🚀 Starting sync: url=$edgeUrl, hasApiKey=${apiKey.isNotBlank()}")

        syncFromSupabaseEdge(edgeUrl = edgeUrl, apiKey = apiKey)
    }

    private suspend fun syncFromSupabaseEdge(edgeUrl: String, apiKey: String): Result {
        if (edgeUrl.isBlank()) {
            Log.w(TAG, "Supabase edge URL is blank; retrying later")
            return Result.retry()
        }

        return try {
            val sinceCursor = appSettingsManager.getCatalogSyncCursor()
            val urlWithCursor = if (sinceCursor.isBlank()) {
                edgeUrl
            } else {
                val separator = if (edgeUrl.contains("?")) "&" else "?"
                val encodedCursor = URLEncoder.encode(sinceCursor, Charsets.UTF_8.name())
                "$edgeUrl${separator}since=$encodedCursor"
            }

            val requestBuilder = Request.Builder()
                .url(urlWithCursor)
                .header("Cache-Control", "no-cache")

            val firebaseToken = runCatching {
                firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
            }.onFailure {
                Log.w(TAG, "Firebase ID token alınamadı", it)
            }.getOrNull()


            if (apiKey.isNotBlank()) {
                requestBuilder.header("apikey", apiKey)
            }
            firebaseToken?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }

            val responseBody = okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Supabase edge HTTP ${response.code} - ${response.message}")
                    if (response.code == 401 || response.code == 403) {
                        return Result.failure()
                    }
                    return Result.retry()
                }
                response.body?.string().orEmpty()
            }

            val payload = gson.fromJson(responseBody, SupabaseEdgeResponse::class.java)

            var changedCount = 0
            payload.records.forEach { car ->
                val entity = car.toEntityOrNull() ?: return@forEach
                val changed = upsertMasterData(entity)
                if (changed) changedCount++
            }

            val newCursor = payload.cursor.orEmpty()
            if (newCursor.isNotBlank()) {
                appSettingsManager.setCatalogSyncCursor(newCursor)
            }

            Log.d(TAG, "✅ Supabase edge sync complete: records=${payload.records.size}, changed=$changedCount, cursor=${if (newCursor.isBlank()) "unchanged" else "updated"}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Supabase edge sync failed: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun upsertMasterData(entity: MasterDataEntity): Boolean {
        val existingByToyNum = entity.toyNum
            .takeIf { it.isNotBlank() }
            ?.let { masterDataDao.getByToyNumAndDataSource(it, entity.dataSource) }
        val existingByIdentity = if (existingByToyNum == null) {
            masterDataDao.getByIdentity(
                brand = entity.brand,
                modelName = entity.modelName,
                year = entity.year,
                dataSource = entity.dataSource
            )
        } else {
            null
        }
        val existing = existingByToyNum ?: existingByIdentity

        if (existing == null) {
            masterDataDao.insertAll(listOf(entity))
            if (entity.caseNum.isNotBlank() && entity.toyNum.isNotBlank()) {
                masterDataDao.updateCaseNum(entity.toyNum, entity.dataSource, entity.caseNum)
            }
            return true
        }

        val validFeatures = setOf("sth", "chase", "th")
        val normalizedNewFeature = entity.feature?.trim()?.lowercase()?.takeIf { it in validFeatures }
        val updatedEntity = existing.copy(
            brand = entity.brand,
            modelName = entity.modelName,
            series = entity.series,
            seriesNum = entity.seriesNum,
            year = entity.year,
            color = entity.color.ifBlank { existing.color },
            imageUrl = entity.imageUrl.ifBlank { existing.imageUrl },
            scale = entity.scale.ifBlank { existing.scale },
            toyNum = entity.toyNum.ifBlank { existing.toyNum },
            colNum = entity.colNum.ifBlank { existing.colNum },
            isPremium = entity.isPremium,
            dataSource = entity.dataSource,
            caseNum = entity.caseNum.ifBlank { existing.caseNum },
            feature = normalizedNewFeature ?: existing.feature
        )

        if (updatedEntity != existing) {
            masterDataDao.update(updatedEntity)
            return true
        }

        if (entity.caseNum.isNotBlank() && entity.toyNum.isNotBlank()) {
            masterDataDao.updateCaseNum(entity.toyNum, entity.dataSource, entity.caseNum)
        }
        return false
    }

    private fun SupabaseEdgeCarDto.toEntityOrNull(): MasterDataEntity? {
        if (modelName.isBlank()) return null
        val normalizedBrand = normalizeBrand(brand) ?: return null
        val normalizedDataSource = dataSource.ifBlank { normalizedBrand.lowercase() }
        val parsedYear = year?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
        val validFeatures = setOf("sth", "chase", "th")
        val normalizedFeature = feature?.trim()?.lowercase()?.takeIf { it in validFeatures }

        val isPremiumCalculated = when {
            category?.contains("Premium", ignoreCase = true) == true -> true
            category?.contains("Mainline", ignoreCase = true) == true -> false
            normalizedFeature == "sth" -> true
            category.isNullOrBlank() -> false
            else -> true // If specified but not mainline, assume it's a specific premium series
        }

        return MasterDataEntity(
            brand = normalizedBrand,
            modelName = modelName,
            series = series,
            seriesNum = seriesNum,
            year = parsedYear,
            color = color,
            imageUrl = imageUrl,
            scale = scale.ifBlank { "1:64" },
            toyNum = toyNum,
            colNum = colNum,
            isPremium = isPremiumCalculated,
            dataSource = normalizedDataSource,
            caseNum = caseNum,
            feature = normalizedFeature,
            category = category
        )
    }

    private fun normalizeBrand(raw: String): String? {
        if (raw.isBlank()) return null
        val normalized = raw.trim()
        val fromExact = Brand.values().firstOrNull { it.name.equals(normalized, ignoreCase = true) }
        if (fromExact != null) return fromExact.name

        val compact = normalized.lowercase().replace("_", "").replace("-", "").replace(" ", "")
        return Brand.values().firstOrNull { it.name.lowercase().replace("_", "") == compact }?.name
    }
}
