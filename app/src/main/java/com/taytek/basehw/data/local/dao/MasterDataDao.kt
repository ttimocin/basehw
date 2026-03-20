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
        AND (modelName LIKE '%' || :query || '%' 
             OR series LIKE '%' || :query || '%' 
             OR toyNum LIKE '%' || :query || '%' 
             OR colNum LIKE '%' || :query || '%')
        ORDER BY modelName ASC
    """)
    fun searchByBrand(brand: String, query: String): PagingSource<Int, MasterDataEntity>

    @Query("SELECT * FROM master_data WHERE brand = :brand")
    suspend fun getAllByBrandList(brand: String): List<MasterDataEntity>

    @Query("SELECT * FROM master_data WHERE brand = :brand ORDER BY modelName ASC")
    fun getAllByBrand(brand: String): PagingSource<Int, MasterDataEntity>

    @Query("SELECT id FROM master_data WHERE brand = :brand AND modelName = :modelName AND year = :year LIMIT 1")
    suspend fun getIdByIdentity(brand: String, modelName: String, year: Int?): Long?

    @Query("SELECT * FROM master_data ORDER BY brand ASC, modelName ASC")
    fun getAll(): PagingSource<Int, MasterDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MasterDataEntity>)

    @Query("UPDATE master_data SET caseNum = :caseNum WHERE toyNum = :toyNum AND dataSource = :dataSource")
    suspend fun updateCaseNum(toyNum: String, dataSource: String, caseNum: String)

    @Query("SELECT * FROM master_data WHERE toyNum = :toyNum AND dataSource = :dataSource LIMIT 1")
    suspend fun getByToyNumAndDataSource(toyNum: String, dataSource: String): MasterDataEntity?

    @Query("SELECT * FROM master_data WHERE brand = :brand AND modelName = :modelName AND year IS :year AND dataSource = :dataSource LIMIT 1")
    suspend fun getByIdentity(brand: String, modelName: String, year: Int?, dataSource: String): MasterDataEntity?

    @Update
    suspend fun update(item: MasterDataEntity)

    @Query("DELETE FROM master_data WHERE brand = :brand")
    suspend fun deleteByBrand(brand: String)

    @Query("DELETE FROM master_data WHERE brand = :brand AND year = :year")
    suspend fun deleteByBrandAndYear(brand: String, year: Int)

    @Query("SELECT COUNT(*) FROM master_data")
    suspend fun getCount(): Int

    @Query("SELECT * FROM master_data WHERE brand = :brand AND year = :year")
    suspend fun getListByBrandAndYear(brand: String, year: Int): List<MasterDataEntity>

    @Query("SELECT COUNT(*) FROM master_data WHERE brand = :brand")
    suspend fun getCountByBrand(brand: String): Int

    @Query("""
        SELECT * FROM master_data 
        WHERE brand = :brand 
        AND (modelName LIKE '%' || :query || '%' 
             OR series LIKE '%' || :query || '%' 
             OR toyNum LIKE '%' || :query || '%' 
             OR colNum LIKE '%' || :query || '%')
        ORDER BY year DESC, modelName ASC
    """)
    fun searchByBrandOrderedByYear(brand: String, query: String): PagingSource<Int, MasterDataEntity>
    @Query("SELECT * FROM master_data WHERE brand = :brand AND series = :series ORDER BY seriesNum ASC")
    suspend fun getListBySeries(brand: String, series: String): List<MasterDataEntity>

    @Query("""
        SELECT * FROM master_data
        WHERE brand = :brand
        AND series = :series
        AND (:year IS NULL OR year = :year)
        ORDER BY seriesNum ASC
    """)
    suspend fun getListBySeriesAndYear(brand: String, series: String, year: Int?): List<MasterDataEntity>

    @Query("SELECT * FROM master_data WHERE feature = 'sth' ORDER BY year DESC, modelName ASC")
    fun getSthCars(): PagingSource<Int, MasterDataEntity>

    @Query("""
        SELECT * FROM master_data 
        WHERE feature = 'sth' 
        AND (modelName LIKE '%' || :query || '%' 
             OR series LIKE '%' || :query || '%' 
             OR toyNum LIKE '%' || :query || '%' 
             OR colNum LIKE '%' || :query || '%')
        ORDER BY year DESC, modelName ASC
    """)
    fun searchSthCars(query: String): PagingSource<Int, MasterDataEntity>

    @Query("SELECT DISTINCT year FROM master_data WHERE feature = 'sth' AND year IS NOT NULL ORDER BY year DESC")
    fun getSthYears(): Flow<List<Int>>

    @Query("SELECT DISTINCT year FROM master_data WHERE feature = 'chase' AND year IS NOT NULL ORDER BY year DESC")
    fun getChaseYears(): Flow<List<Int>>

    @Query("""
        SELECT * FROM master_data 
        WHERE feature = 'sth'
        AND (:year IS NULL OR year = :year)
        AND (modelName LIKE '%' || :query || '%' 
             OR series LIKE '%' || :query || '%' 
             OR toyNum LIKE '%' || :query || '%' 
             OR colNum LIKE '%' || :query || '%')
        ORDER BY year DESC, modelName ASC
    """)
    fun searchSthCarsWithYear(query: String, year: Int?): PagingSource<Int, MasterDataEntity>

    @Query("SELECT * FROM master_data WHERE feature = 'chase' ORDER BY year DESC, modelName ASC")
    fun getChaseCars(): PagingSource<Int, MasterDataEntity>

    @Query("""
        SELECT * FROM master_data 
        WHERE feature = 'chase' 
        AND (:year IS NULL OR year = :year)
        AND (modelName LIKE '%' || :query || '%' 
             OR series LIKE '%' || :query || '%' 
             OR toyNum LIKE '%' || :query || '%' 
             OR colNum LIKE '%' || :query || '%')
        ORDER BY year DESC, modelName ASC
    """)
    fun searchChaseCarsWithYear(query: String, year: Int?): PagingSource<Int, MasterDataEntity>

    @Query("SELECT * FROM master_data WHERE feature = 'th' ORDER BY year DESC, modelName ASC")
    fun getThCars(): PagingSource<Int, MasterDataEntity>

    @Query("SELECT DISTINCT year FROM master_data WHERE feature = 'th' AND year IS NOT NULL ORDER BY year DESC")
    fun getThYears(): Flow<List<Int>>

    @Query("""
        SELECT * FROM master_data 
        WHERE feature = 'th' 
        AND (:year IS NULL OR year = :year)
        AND (modelName LIKE '%' || :query || '%' 
             OR series LIKE '%' || :query || '%' 
             OR toyNum LIKE '%' || :query || '%' 
             OR colNum LIKE '%' || :query || '%')
        ORDER BY year DESC, modelName ASC
    """)
    fun searchThCarsWithYear(query: String, year: Int?): PagingSource<Int, MasterDataEntity>

    @Query("SELECT * FROM master_data WHERE id = :id")
    suspend fun getById(id: Long): MasterDataEntity?

    @Query("""
        SELECT * FROM master_data 
        WHERE (modelName LIKE '%' || :query || '%' 
               OR series LIKE '%' || :query || '%' 
               OR toyNum LIKE '%' || :query || '%' 
               OR colNum LIKE '%' || :query || '%')
        ORDER BY modelName ASC
    """)
    fun searchAll(query: String): PagingSource<Int, MasterDataEntity>

    @Query("""
        SELECT 
            CASE 
                WHEN toyNum != '' THEN toyNum || '|' || dataSource
                ELSE modelName || '|' || year || '|' || dataSource
            END
        FROM master_data 
        WHERE brand = :brand
    """)
    suspend fun getIdentityKeysByBrand(brand: String): List<String>
}
