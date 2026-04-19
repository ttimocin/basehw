package com.taytek.basehw.domain.repository

import com.taytek.basehw.domain.model.DiecastNews

interface NewsRepository {
    suspend fun getLatest(limit: Int): Result<List<DiecastNews>>
    suspend fun getById(id: String): Result<DiecastNews?>
}
