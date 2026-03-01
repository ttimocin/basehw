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
import com.taytek.basehw.data.remote.dto.BrandCarDto
import com.taytek.basehw.domain.model.Brand
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-shot WorkManager worker that reads all yearly JSON files from
 * assets for each brand sub-folder and bulk-inserts them into master_data.
 *
 * Folder layout:
 *   assets/hotwheels/hotwheels_YYYY.json   → HOT_WHEELS
 *   assets/matchbox/matchbox_YYYY.json     → MATCHBOX
 *
 * Triggered automatically on first launch when the DB is empty.
 */
@HiltWorker
class AssetSeedWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val masterDataDao: MasterDataDao,
    private val gson: Gson
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "AssetSeedWorker"
        const val WORK_NAME = "hw_asset_seed"

        /** Maps asset folder name → Brand enum + filename prefix */
        private val BRAND_CONFIGS = listOf(
            Triple("hotwheels", Brand.HOT_WHEELS, Regex("hotwheels_(\\d{4})\\.json")),
            Triple("matchbox",   Brand.MATCHBOX,   Regex("matchbox_(\\d{4})\\.json")),
            Triple("minigt",     Brand.MINI_GT,    Regex("minigt\\.json"))
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "▶ Starting asset seed import")

        return@withContext try {
            val assets = context.assets
            val listType = object : TypeToken<List<BrandCarDto>>() {}.type
            var totalInserted = 0

            BRAND_CONFIGS.forEach { (folder, brand, yearRegex) ->
                val files = assets.list(folder)
                    ?.filter { yearRegex.matches(it) }
                    ?.sortedBy { yearRegex.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0 }
                    ?: emptyList()

                if (files.isEmpty()) {
                    Log.w(TAG, "  ⚠ No files in assets/$folder/ — skipping ${brand.name}")
                    return@forEach
                }

                Log.d(TAG, "  ${brand.name}: ${files.size} files (${files.first()} … ${files.last()})")

                val entities = mutableListOf<MasterDataEntity>()

                files.forEach { filename ->
                    val yearFromName = yearRegex.find(filename)?.groupValues?.getOrNull(1)?.toIntOrNull()

                    val json = assets.open("$folder/$filename")
                        .bufferedReader()
                        .use { it.readText() }

                    val cars: List<BrandCarDto> = gson.fromJson(json, listType)

                    cars.forEach { car ->
                        if (car.modelName.isNotBlank()) {
                            val parsedYearFromField = car.year?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
                            
                            entities.add(
                                MasterDataEntity(
                                    brand     = brand.name,
                                    modelName = car.modelName,
                                    series    = car.series.ifBlank { car.driveType },
                                    seriesNum = car.seriesNum,
                                    year      = parsedYearFromField ?: yearFromName,
                                    color     = car.originalBrand,
                                    imageUrl  = car.imageUrl,
                                    scale     = "1:64",
                                    toyNum    = car.toyNum,
                                    colNum    = car.colNum.ifBlank { car.manNum }.ifBlank { car.code }
                                )
                            )
                        }
                    }
                }

                // Atomic replace per brand
                masterDataDao.deleteByBrand(brand.name)
                masterDataDao.insertAll(entities)
                totalInserted += entities.size
                Log.d(TAG, "  ✅ ${brand.name}: ${entities.size} models inserted")
            }

            Log.d(TAG, "✅ Asset seed complete — $totalInserted models total")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Asset seed failed: ${e.message}", e)
            Result.retry()
        }
    }
}
