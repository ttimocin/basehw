package com.taytek.basehw.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
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
        const val WORK_NAME = "hw_asset_seed_v1" // Reset for development

        private data class SeedConfig(
            val folder: String,
            val brand: Brand,
            val regex: Regex,
            val isPremiumOverride: Boolean? = null,
            val dataSourceOverride: String? = null
        )

        private val SEED_CONFIGS = listOf(
            // 1. Specialized collections (Small files, high priority)
            SeedConfig("hotwheels/Premium/carculture", Brand.HOT_WHEELS, Regex("cc\\.json")),
            SeedConfig("hotwheels/Premium/popculture", Brand.HOT_WHEELS, Regex("pop\\.json")),
            SeedConfig("hotwheels/Premium/faf", Brand.HOT_WHEELS, Regex("faf\\.json")),
            SeedConfig("hotwheels/Premium/Boulevard", Brand.HOT_WHEELS, Regex("boulevard\\.json"), isPremiumOverride = true),
            SeedConfig("hotwheels/Premium/rlc", Brand.HOT_WHEELS, Regex("rlc\\.json"), isPremiumOverride = true),
            
            // 2. STH/TH specific updates (MUST be before mainlines to ensure tags are set)
            SeedConfig("hotwheels", Brand.HOT_WHEELS, Regex("hotwheels_th_sth\\.json")),
            
            // 3. Other Brands (Reliable smaller files)
            SeedConfig("matchbox", Brand.MATCHBOX, Regex("matchbox\\.json")),
            SeedConfig("minigt", Brand.MINI_GT, Regex("minigt\\.json")),
            SeedConfig("majorette", Brand.MAJORETTE, Regex("majorette\\.json")),
            SeedConfig("siku", Brand.SIKU, Regex("siku\\.json")),
            SeedConfig("kaido", Brand.KAIDO_HOUSE, Regex("kaido\\.json")),
            
            // 4. Massive Mainlines (Last priority due to size)
            SeedConfig("hotwheels", Brand.HOT_WHEELS, Regex("hotwheels\\.json")),
            SeedConfig("hotwheels/other", Brand.HOT_WHEELS, Regex("other\\.json"), isPremiumOverride = false)
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "▶ Starting optimized asset seed import ($WORK_NAME)")

        return@withContext try {
            val assets = context.assets
            var totalInsertedCount = 0
            var totalProcessedCount = 0
            var totalChaseCount = 0
            var totalSthCount = 0
            var totalThCount = 0

            val processedStrongKeysInThisRun = mutableSetOf<String>()

            SEED_CONFIGS.forEach { config ->
                val files = assets.list(config.folder)
                    ?.filter { config.regex.matches(it) }
                    ?.sortedBy { config.regex.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0 }
                    ?: emptyList()

                if (files.isEmpty()) return@forEach

                // Instead of fetching all entities (causes OOM), fetch only the small string keys!
                val existingIdentityKeys = masterDataDao.getStrongIdentityKeysByBrand(config.brand.name).toMutableSet()


                files.forEach { filename ->
                    val yearFromFilename = config.regex.find(filename)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    val batch = mutableListOf<MasterDataEntity>()
                    val updateBatch = mutableListOf<MasterDataEntity>()
                    val batchSize = 500

                    Log.d(TAG, "Processing asset file: ${config.folder}/$filename")
                    assets.open("${config.folder}/$filename").use { inputStream ->
                        val reader = com.google.gson.stream.JsonReader(inputStream.bufferedReader())
                        reader.beginArray()
                        while (reader.hasNext()) {
                            val carDto: BrandCarDto = gson.fromJson(reader, BrandCarDto::class.java)
                            
                            val safeStr = { s: String? -> s ?: "" }
                            
                            val dtoModelName = safeStr(carDto.modelName)
                            if (dtoModelName.isNotBlank() && dtoModelName != "Model Name") {
                                val dtoYear = safeStr(carDto.year)
                                val dtoYears = safeStr(carDto.years)
                                
                                val parsedYear = dtoYear.replace(Regex("[^0-9]"), "").toIntOrNull()
                                    ?: dtoYears.replace(Regex("[^0-9]"), "").toIntOrNull()
                                    ?: carDto.listYear ?: yearFromFilename
                                
                                val isPremium = config.isPremiumOverride ?: config.folder.contains("Premium", ignoreCase = true)
                                val currentDataSource = config.dataSourceOverride ?: config.folder
                                
                                val dtoBodyColor = safeStr(carDto.bodyColor)
                                val dtoOriginalBrand = safeStr(carDto.originalBrand)
                                
                                val resolvedColor = when (config.brand) {
                                    Brand.MINI_GT -> ""
                                    else -> dtoBodyColor.ifBlank { dtoOriginalBrand }
                                }
                                
                                val dtoFeature = safeStr(carDto.feature).trim().lowercase()
                                var feature = dtoFeature.takeIf { it.isNotBlank() }

                                val dtoSeriesNum = safeStr(carDto.seriesNum)
                                // Automatic detection for Chase based on series_num
                                if (feature.isNullOrBlank()) {
                                    if (dtoSeriesNum.trim() == "0/5" || dtoSeriesNum.trim().startsWith("0/")) {
                                        feature = "chase"
                                    }
                                }

                                val dtoSeries = safeStr(carDto.series)
                                val dtoSeriesType = safeStr(carDto.seriesType)
                                val dtoSetName = safeStr(carDto.setName)
                                val dtoDriveType = safeStr(carDto.driveType)
                                val dtoPageSource = safeStr(carDto.pageSource)

                                val seriesResolved = dtoSeries.ifBlank { dtoSeriesType }.ifBlank { dtoSetName }.ifBlank { dtoDriveType }.ifBlank { dtoPageSource }

                                val dtoImageUrl = safeStr(carDto.imageUrl)
                                val dtoScale = safeStr(carDto.scale)
                                val dtoToyNum = safeStr(carDto.toyNum)
                                val dtoColNum = safeStr(carDto.colNum)
                                val dtoSerieNr = safeStr(carDto.serieNr)
                                val dtoManNum = safeStr(carDto.manNum)
                                val dtoCode = safeStr(carDto.code)
                                val dtoCase = safeStr(carDto.case)

                                val colNumResolved = dtoColNum.ifBlank { dtoSerieNr }.ifBlank { dtoManNum }.ifBlank { dtoCode }

                                val entity = MasterDataEntity(
                                    brand = config.brand.name,
                                    modelName = dtoModelName,
                                    series = seriesResolved,
                                    seriesNum = dtoSeriesNum,
                                    year = parsedYear,
                                    color = resolvedColor,
                                    imageUrl = dtoImageUrl,
                                    scale = dtoScale.ifBlank { "1:64" },
                                    toyNum = dtoToyNum,
                                    colNum = colNumResolved,
                                    isPremium = isPremium,
                                    dataSource = currentDataSource,
                                    caseNum = dtoCase,
                                    feature = feature
                                )

                                if (feature == "chase") totalChaseCount++
                                if (feature == "sth") totalSthCount++
                                if (feature == "th") totalThCount++
                                totalProcessedCount++

                                val yearStr = entity.year?.toString() ?: ""
                                val strongKey = "${entity.modelName.trim().lowercase()}|${yearStr}|${entity.series.trim().lowercase()}|${entity.color.trim().lowercase()}|${entity.toyNum.trim().lowercase()}|${entity.dataSource.trim().lowercase()}"
                                
                                if (processedStrongKeysInThisRun.contains(strongKey)) {
                                    continue // Ayni dosyada birden fazla olan tam kopyalari (json hatasi) direkt atla
                                }
                                processedStrongKeysInThisRun.add(strongKey)

                                // OOM ve DB kilitlenmesini onlemek icin hizli varyasyon sorgusu:
                                val existingEntity = if (existingIdentityKeys.contains(strongKey)) {
                                    val candidates = masterDataDao.getVariationsLight(entity.dataSource, entity.year, entity.modelName, entity.brand)
                                    candidates.find { 
                                        it.series.trim().equals(entity.series.trim(), ignoreCase = true) &&
                                        it.color.trim().equals(entity.color.trim(), ignoreCase = true) &&
                                        it.toyNum.trim().equals(entity.toyNum.trim(), ignoreCase = true)
                                    }
                                } else null

                                if (existingEntity != null) {
                                    val updatedEntity = existingEntity.copy(
                                        brand = entity.brand,
                                        modelName = entity.modelName,
                                        series = entity.series,
                                        seriesNum = entity.seriesNum,
                                        year = entity.year,
                                        color = entity.color,
                                        imageUrl = entity.imageUrl,
                                        scale = entity.scale,
                                        toyNum = entity.toyNum.ifBlank { existingEntity.toyNum },
                                        colNum = entity.colNum,
                                        isPremium = entity.isPremium,
                                        dataSource = entity.dataSource,
                                        caseNum = entity.caseNum.ifBlank { existingEntity.caseNum },
                                        feature = entity.feature.takeIf { !it.isNullOrBlank() } ?: existingEntity.feature
                                    )
                                    if (updatedEntity != existingEntity) {
                                        updateBatch.add(updatedEntity)
                                        if (updatedEntity.caseNum != existingEntity.caseNum && !updatedEntity.feature.isNullOrBlank()) {
                                            Log.d(TAG, "Updated ${updatedEntity.feature} [${updatedEntity.modelName}] with Case: ${updatedEntity.caseNum}")
                                        }
                                    }
                                } else {
                                    batch.add(entity)
                                    existingIdentityKeys.add(strongKey)
                                }
                                
                                if (carDto.case.isNotBlank() && entity.toyNum.isNotBlank()) {
                                    masterDataDao.updateCaseNum(entity.toyNum, entity.dataSource, carDto.case)
                                }
                            }

                            if (batch.size >= batchSize) {
                                masterDataDao.insertAll(batch)
                                totalInsertedCount += batch.size
                                batch.clear()
                            }
                            if (updateBatch.size >= 100) {
                                updateBatch.forEach { masterDataDao.update(it) }
                                updateBatch.clear()
                            }
                        }
                        reader.endArray()
                        if (batch.isNotEmpty()) {
                            masterDataDao.insertAll(batch)
                            totalInsertedCount += batch.size
                        }
                        if (updateBatch.isNotEmpty()) {
                            updateBatch.forEach { masterDataDao.update(it) }
                        }
                    }
                }
            }

            Log.d(TAG, "✅ Asset seed complete — Processed: $totalProcessedCount, New: $totalInsertedCount, Chase: $totalChaseCount, STH: $totalSthCount, TH: $totalThCount")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Asset seed failed: ${e.message}", e)
            Result.retry()
        }
    }
}
