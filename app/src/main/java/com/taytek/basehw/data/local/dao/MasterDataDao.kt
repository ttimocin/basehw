package com.taytek.basehw.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.taytek.basehw.data.local.entity.MasterDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterDataDao {

    @Query("""
        SELECT * FROM master_data 
        WHERE brand = :brand 
        AND (modelName LIKE '%' || :query || '%' OR series LIKE '%' || :query || '%')
        ORDER BY modelName ASC
    """)
    fun searchByBrand(brand: String, query: String): PagingSource<Int, MasterDataEntity>

    @Query("SELECT * FROM master_data WHERE brand = :brand ORDER BY modelName ASC")
    fun getAllByBrand(brand: String): PagingSource<Int, MasterDataEntity>

    @Query("SELECT * FROM master_data ORDER BY brand ASC, modelName ASC")
    fun getAll(): PagingSource<Int, MasterDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MasterDataEntity>)

    @Query("DELETE FROM master_data WHERE brand = :brand")
    suspend fun deleteByBrand(brand: String)

    @Query("DELETE FROM master_data WHERE brand = :brand AND year = :year")
    suspend fun deleteByBrandAndYear(brand: String, year: Int)

    @Query("SELECT COUNT(*) FROM master_data")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM master_data WHERE brand = :brand")
    suspend fun getCountByBrand(brand: String): Int

    @Query("""
        SELECT * FROM master_data 
        WHERE brand = :brand 
        AND (modelName LIKE '%' || :query || '%' OR series LIKE '%' || :query || '%')
        ORDER BY year DESC, modelName ASC
    """)
    fun searchByBrandOrderedByYear(brand: String, query: String): PagingSource<Int, MasterDataEntity>
}
