package com.taytek.basehw.domain.repository

import com.taytek.basehw.domain.model.DiecastNews

interface NewsRepository {
    suspend fun getLatest(limit: Int): Result<List<DiecastNews>>
    suspend fun getById(id: String): Result<DiecastNews?>

    companion object {
        /** Ana sayfa: Supabase `diecast_news` tablosunda `published_at` azalan sırayla en yeni N kayıt. */
        const val HOME_SCREEN_NEWS_LIMIT = 10
    }
}
