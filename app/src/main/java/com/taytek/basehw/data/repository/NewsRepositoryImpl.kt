package com.taytek.basehw.data.repository

import android.util.Log
import com.taytek.basehw.data.local.AppSettingsManager
import com.taytek.basehw.data.remote.supabase.model.SupabaseDiecastNewsRow
import com.taytek.basehw.domain.model.DiecastNews
import com.taytek.basehw.domain.repository.NewsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val appSettingsManager: AppSettingsManager
) : NewsRepository {

    override suspend fun getLatest(limit: Int): Result<List<DiecastNews>> {
        return try {
            val rows = supabaseClient.from("diecast_news").select {
                filter { eq("is_published", true) }
                order("published_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<SupabaseDiecastNewsRow>()
            Result.success(rows.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e("NewsRepository", "getLatest error", e)
            Result.failure(e)
        }
    }

    override suspend fun getById(id: String): Result<DiecastNews?> {
        return try {
            val row = supabaseClient.from("diecast_news").select {
                filter { eq("id", id) }
                filter { eq("is_published", true) }
                limit(1)
            }.decodeSingleOrNull<SupabaseDiecastNewsRow>()
            Result.success(row?.toDomain())
        } catch (e: Exception) {
            Log.e("NewsRepository", "getById error", e)
            Result.failure(e)
        }
    }

    private fun SupabaseDiecastNewsRow.toDomain(): DiecastNews {
        val lang = appSettingsManager.languageFlow.value.ifBlank { "en" }.lowercase()
        return DiecastNews(
            id = id,
            title = localizedTitle(lang),
            body = localizedBody(lang),
            imageUrl = imageUrl,
            publishedAt = publishedAt.orEmpty()
        )
    }

    private fun SupabaseDiecastNewsRow.localizedTitle(lang: String): String {
        val preferred = when (lang) {
            "tr" -> titleTr
            "en" -> titleEn
            "de" -> titleDe
            "es" -> titleEs
            "fr" -> titleFr
            "pt" -> titlePt
            "ru" -> titleRu
            "uk" -> titleUk
            "ar" -> titleAr
            else -> null
        }
        return preferred.orIfBlank(titleEn).orIfBlank(title).orIfBlank(titleTr).orEmpty()
    }

    private fun SupabaseDiecastNewsRow.localizedBody(lang: String): String {
        val preferred = when (lang) {
            "tr" -> bodyTr
            "en" -> bodyEn
            "de" -> bodyDe
            "es" -> bodyEs
            "fr" -> bodyFr
            "pt" -> bodyPt
            "ru" -> bodyRu
            "uk" -> bodyUk
            "ar" -> bodyAr
            else -> null
        }
        return preferred.orIfBlank(bodyEn).orIfBlank(body).orIfBlank(bodyTr).orEmpty()
    }

    private fun String?.orIfBlank(fallback: String?): String? {
        val v = this?.trim()
        return if (v.isNullOrEmpty()) fallback?.trim() else v
    }
}
