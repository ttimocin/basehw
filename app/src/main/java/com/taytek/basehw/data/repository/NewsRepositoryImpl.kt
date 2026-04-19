package com.taytek.basehw.data.repository

import android.util.Log
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
    private val supabaseClient: SupabaseClient
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

    private fun SupabaseDiecastNewsRow.toDomain() = DiecastNews(
        id = id,
        title = title,
        body = body,
        imageUrl = imageUrl,
        publishedAt = publishedAt.orEmpty()
    )
}
