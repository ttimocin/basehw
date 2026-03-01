package com.taytek.basehw.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.local.entity.MasterDataEntity
import com.taytek.basehw.data.remote.api.MediaWikiApiService
import com.taytek.basehw.domain.model.Brand
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker — fetches model data from Fandom MediaWiki API
 * and populates the local master_data Room table.
 *
 * Input: KEY_BRAND = Brand.name()
 */
@HiltWorker
class FandomSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiService: MediaWikiApiService,
    private val masterDataDao: MasterDataDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "FandomSyncWorker"
        const val KEY_BRAND = "brand"

        // Full api.php URLs per brand wiki
        private val BRAND_API_URLS = mapOf(
            Brand.HOT_WHEELS to "https://hotwheels.fandom.com/api.php",
            Brand.MATCHBOX   to "https://matchbox.fandom.com/api.php",
            Brand.MINI_GT    to "https://mini-gt.fandom.com/api.php"
        )

        // Category names confirmed by querying each wiki
        private val BRAND_CATEGORIES = mapOf(
            Brand.HOT_WHEELS to "Category:Hot Wheels",
            Brand.MATCHBOX   to "Category:Matchbox",
            Brand.MINI_GT    to "Category:Mini GT"
        )
    }

    override suspend fun doWork(): Result {
        val brandName = inputData.getString(KEY_BRAND) ?: return Result.failure()
        val brand = runCatching { Brand.valueOf(brandName) }.getOrNull() ?: return Result.failure()

        val apiUrl  = BRAND_API_URLS[brand]  ?: return Result.failure()
        val category = BRAND_CATEGORIES[brand] ?: return Result.failure()

        Log.d(TAG, "▶ Starting sync — brand=$brandName  category=$category")

        return try {
            val entities = mutableListOf<MasterDataEntity>()
            var continueToken: String? = null
            var pagesFetched = 0

            // ── Step 1: list category members ──────────────────────────────
            do {
                val response = apiService.getCategoryMembers(
                    url = apiUrl,
                    categoryTitle = category,
                    limit = 500,
                    continueToken = continueToken
                )

                val members = response.query?.categoryMembers.orEmpty()
                Log.d(TAG, "  Got ${members.size} members (batch), continue=${response.continueData?.cmContinue}")

                // ── Step 2: fetch images in batches of 50 ──────────────────
                members
                    .filter { it.ns == 0 && it.title.isNotBlank() }
                    .chunked(50)
                    .forEach { chunk ->
                        val pageIds = chunk.joinToString("|") { it.pageId.toString() }
                        val imgResponse = runCatching {
                            apiService.getPageImages(
                                url = apiUrl,
                                pageIds = pageIds
                            )
                        }.getOrNull()

                        chunk.forEach { member ->
                            val page      = imgResponse?.query?.pages?.get(member.pageId.toString())
                            val imageUrl  = page?.thumbnail?.toHighResUrl() ?: ""
                            val (modelName, series, year) = parseTitle(member.title, brand)

                            entities.add(
                                MasterDataEntity(
                                    brand      = brand.name,
                                    modelName  = modelName,
                                    series     = series,
                                    year       = year,
                                    imageUrl   = imageUrl,
                                    scale      = "1:64"
                                )
                            )
                        }
                        pagesFetched += chunk.size
                    }

                continueToken = response.continueData?.cmContinue

            } while (continueToken != null && pagesFetched < 10_000)

            // ── Step 3: save to Room ────────────────────────────────────────
            if (entities.isNotEmpty()) {
                masterDataDao.deleteByBrand(brand.name)
                masterDataDao.insertAll(entities)
                Log.d(TAG, "✅ Synced ${entities.size} models for $brandName")
            } else {
                Log.w(TAG, "⚠ No models found for $brandName — check category name or wiki availability")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync failed for $brandName: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * "2023 Hot Wheels Bone Shaker" → ("Bone Shaker", "", 2023)
     * "Hot Wheels #1 Tesla Roadster" keeps model part only.
     */
    private fun parseTitle(title: String, brand: Brand): Triple<String, String, Int?> {
        val yearRegex = Regex("\\b(19|20)\\d{2}\\b")
        val yearMatch = yearRegex.find(title)
        val year      = yearMatch?.value?.toIntOrNull()

        var modelName = title
            .replace(brand.displayName, "", ignoreCase = true)
            .replace(yearMatch?.value ?: "", "")
            .replace(Regex("^\\s*#?\\d+\\s*"), "")   // strip leading "#123"
            .trim()
            .ifBlank { title }

        return Triple(modelName, "", year)
    }
}
