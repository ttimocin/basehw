package com.taytek.basehw.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
import java.util.Calendar

/**
 * Fetches the CURRENT YEAR's Hot Wheels JSON from a remote URL (e.g. GitHub raw).
 * Runs once immediately, then repeats weekly via PeriodicWorkRequest.
 *
 * New cars added to the remote JSON appear in the app within a week —
 * NO app update required.
 *
 * Remote URL format:
 *   https://raw.githubusercontent.com/{USER}/{REPO}/main/{YEAR}.json
 *
 * Configure REMOTE_BASE_URL below to point to your GitHub repo.
 */
@HiltWorker
class RemoteYearSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val masterDataDao: MasterDataDao,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "RemoteYearSync"
        const val WORK_NAME = "hw_remote_year_sync"

        // ── CONFIGURE THIS ───────────────────────────────────────────────────
        const val REMOTE_BASE_URL = "https://raw.githubusercontent.com/ttimocin/basehw/main/database"
        // ─────────────────────────────────────────────────────────────────────
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        var anySuccess = false

        for (brand in Brand.values()) {
            val brandFolder = brand.name.lowercase().replace("_", "")
            val url = "$REMOTE_BASE_URL/$brandFolder/$currentYear.json"

            Log.d(TAG, "▶ Fetching $url")

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Cache-Control", "no-cache")
                    .build()

                val responseBody = okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "  HTTP ${response.code} — skipping $brand")
                        return@use null
                    }
                    response.body?.string()
                } ?: continue

                val listType = object : TypeToken<List<HotWheelsCarDto>>() {}.type
                val cars: List<HotWheelsCarDto> = gson.fromJson(responseBody, listType)

                val entities = cars
                    .filter { it.modelName.isNotBlank() }
                    .map { car ->
                        MasterDataEntity(
                            brand     = brand.name,
                            modelName = car.modelName,
                            series    = car.series,
                            year      = currentYear,
                            imageUrl  = car.imageUrl,
                            scale     = "1:64"
                        )
                    }

                // Only insert new cars that aren't already locally seeded
                val existingLocalCars = masterDataDao.getListByBrandAndYear(brand.name, currentYear)
                val existingNames = existingLocalCars.map { it.modelName }.toSet()

                val newEntitiesToInsert = entities.filter { !existingNames.contains(it.modelName) }

                if (newEntitiesToInsert.isNotEmpty()) {
                    masterDataDao.insertAll(newEntitiesToInsert)
                }
                anySuccess = true

                Log.d(TAG, "✅ Remote sync done — $brand $currentYear: F:${entities.size} - I:${newEntitiesToInsert.size}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Remote sync failed for $brand: ${e.message}", e)
            }
        }
        
        return@withContext if (anySuccess) Result.success() else Result.retry()
    }
}
