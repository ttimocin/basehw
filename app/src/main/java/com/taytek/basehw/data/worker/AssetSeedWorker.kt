package com.taytek.basehw.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.local.entity.MasterDataEntity
import com.taytek.basehw.data.remote.dto.BrandCarDto
import com.taytek.basehw.domain.model.Brand
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

/**
 * One-shot WorkManager worker that reads all yearly JSON files from
 * assets for each brand sub-folder and bulk-inserts them into master_data.
 */
@HiltWorker
class AssetSeedWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val masterDataDao: MasterDataDao,
    private val json: Json,
    private val appSettingsManager: com.taytek.basehw.data.local.AppSettingsManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "AssetSeedWorker"
        const val WORK_NAME = "hw_asset_seed_v5" // Updated to trigger 2026 cleanup

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
            SeedConfig("hotwheels/Premium/elite64", Brand.HOT_WHEELS, Regex("elite64\\.json"), isPremiumOverride = true),
            
            // 2. STH/TH specific updates (MUST be before mainlines to ensure tags are set)
            SeedConfig("hotwheels", Brand.HOT_WHEELS, Regex("hotwheels_th_sth\\.json")),
            
            // 3. Other Brands (Reliable smaller files)
            SeedConfig("matchbox", Brand.MATCHBOX, Regex("matchbox\\.json")),
            SeedConfig("minigt", Brand.MINI_GT, Regex("minigt\\.json")),
            SeedConfig("majorette", Brand.MAJORETTE, Regex("majorette\\.json")),
            SeedConfig("siku", Brand.SIKU, Regex("siku\\.json")),
            SeedConfig("kaido", Brand.KAIDO_HOUSE, Regex("kaido\\.json")),
            SeedConfig("greenlight", Brand.GREENLIGHT, Regex("greenlight\\.json"), isPremiumOverride = true),
            
            // 4. Massive Mainlines (Last priority due to size)
            SeedConfig("hotwheels", Brand.HOT_WHEELS, Regex("hotwheels\\.json")),
            SeedConfig("hotwheels/other", Brand.HOT_WHEELS, Regex("other\\.json"), isPremiumOverride = false)
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {


        return@withContext try {
            val assets = context.assets
            var totalInsertedCount = 0
            var totalProcessedCount = 0
            var totalChaseCount = 0
            var totalSthCount = 0
            var totalThCount = 0

            val processedStrongKeysInThisRun = mutableSetOf<String>()

            // --- ONE-TIME CLEANUP FOR V5 ---
            if (!appSettingsManager.hasCompleted2026Cleanup()) {

                masterDataDao.deleteByBrandAndYear(Brand.HOT_WHEELS.name, 2026)
                appSettingsManager.setCatalogSyncCursor("") // Force full re-sync from remote
                appSettingsManager.setCompleted2026Cleanup(true)
            }
            // -------------------------------

            // Group configs by brand so we load each brand's existing data only once
            val configsByBrand = SEED_CONFIGS.groupBy { it.brand }

            for ((brand, configs) in configsByBrand) {
                // PRE-LOAD: Load all existing records for this brand into memory ONCE
                val existingRecords = masterDataDao.getAllByBrandForSeed(brand.name)
                
                // Build lookup maps for O(1) dedup
                val byToyNum = mutableMapOf<String, MasterDataEntity>()
                val byIdentity = mutableMapOf<String, MutableList<MasterDataEntity>>()
                
                for (entity in existingRecords) {
                    if (entity.toyNum.isNotBlank()) {
                        byToyNum[entity.toyNum] = entity
                    }
                    val key = "${entity.modelName}|${entity.year}"
                    byIdentity.getOrPut(key) { mutableListOf() }.add(entity)
                }

                // Deferred case updates (toyNum -> caseNum)
                val deferredCaseUpdates = mutableMapOf<String, Pair<String, String>>() // toyNum -> (dataSource, caseNum)

                for (config in configs) {
                    val files = assets.list(config.folder)
                        ?.filter { config.regex.matches(it) }
                        ?.sortedBy { config.regex.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0 }
                        ?: emptyList()

                    if (files.isEmpty()) continue

                    files.forEach { filename ->
                        val yearFromFilename = config.regex.find(filename)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        val insertBatch = mutableListOf<MasterDataEntity>()
                        val updateBatch = mutableListOf<MasterDataEntity>()
                        val batchSize = 500


                        assets.open("${config.folder}/$filename").use { inputStream ->
                            val carDtos = try {
                                json.decodeFromStream<List<BrandCarDto>>(inputStream)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing $filename", e)
                                emptyList()
                            }
                            
                            for (carDto in carDtos) {
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
                                        // Skip — already processed in this run
                                    } else {
                                        processedStrongKeysInThisRun.add(strongKey)

                                        // IN-MEMORY dedup instead of per-record DB queries
                                        val existingEntity = if (entity.toyNum.isNotBlank()) {
                                            byToyNum[entity.toyNum]
                                        } else {
                                            val identityKey = "${entity.modelName}|${entity.year}"
                                            byIdentity[identityKey]?.firstOrNull { match ->
                                                match.toyNum.isBlank() || entity.toyNum.isBlank() || match.toyNum == entity.toyNum
                                            }
                                        }

                                        if (existingEntity != null) {
                                            // Found a match! Update features (TH/STH) but don't insert a new one
                                            val newFeature = entity.feature.takeIf { !it.isNullOrBlank() } ?: existingEntity.feature
                                            val updatedEntity = existingEntity.copy(
                                                brand = entity.brand,
                                                modelName = entity.modelName,
                                                series = entity.series.ifBlank { existingEntity.series },
                                                seriesNum = entity.seriesNum.ifBlank { existingEntity.seriesNum },
                                                year = entity.year ?: existingEntity.year,
                                                color = entity.color.ifBlank { existingEntity.color },
                                                imageUrl = entity.imageUrl.ifBlank { existingEntity.imageUrl },
                                                scale = entity.scale.ifBlank { existingEntity.scale },
                                                toyNum = entity.toyNum.ifBlank { existingEntity.toyNum },
                                                colNum = entity.colNum.ifBlank { existingEntity.colNum },
                                                isPremium = entity.isPremium || existingEntity.isPremium,
                                                dataSource = existingEntity.dataSource, // Keep old data source
                                                caseNum = entity.caseNum.ifBlank { existingEntity.caseNum },
                                                feature = newFeature
                                            )
                                            if (updatedEntity != existingEntity) {
                                                updateBatch.add(updatedEntity)
                                                // Update in-memory maps too
                                                if (updatedEntity.toyNum.isNotBlank()) {
                                                    byToyNum[updatedEntity.toyNum] = updatedEntity
                                                }
                                            }
                                        } else {
                                            insertBatch.add(entity)
                                            // Add to in-memory maps for future dedup within same run
                                            if (entity.toyNum.isNotBlank()) {
                                                byToyNum[entity.toyNum] = entity
                                            }
                                            val identityKey = "${entity.modelName}|${entity.year}"
                                            byIdentity.getOrPut(identityKey) { mutableListOf() }.add(entity)
                                        }
                                        
                                        // Defer case update
                                        if (dtoCase.isNotBlank() && entity.toyNum.isNotBlank()) {
                                            deferredCaseUpdates[entity.toyNum] = Pair(entity.dataSource, dtoCase)
                                        }
                                    }
                                }

                                // Flush insert batch
                                if (insertBatch.size >= batchSize) {
                                    masterDataDao.insertAll(insertBatch)
                                    totalInsertedCount += insertBatch.size
                                    insertBatch.clear()
                                }
                                // Flush update batch (batch of 200)
                                if (updateBatch.size >= 200) {
                                    masterDataDao.updateAll(updateBatch)
                                    updateBatch.clear()
                                }
                            }
                            // Flush remaining
                            if (insertBatch.isNotEmpty()) {
                                masterDataDao.insertAll(insertBatch)
                                totalInsertedCount += insertBatch.size
                            }
                            if (updateBatch.isNotEmpty()) {
                                masterDataDao.updateAll(updateBatch)
                            }
                        }
                    }
                }

                // Apply deferred case updates in batch
                if (deferredCaseUpdates.isNotEmpty()) {

                    deferredCaseUpdates.forEach { (toyNum, pair) ->
                        masterDataDao.updateCaseNum(toyNum, pair.first, pair.second)
                    }
                }
            }


            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Asset seed failed: ${e.message}", e)
            Result.retry()
        }
    }
}
