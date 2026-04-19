package com.taytek.basehw.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.taytek.basehw.data.local.entity.VariantHuntGroupEntity
import com.taytek.basehw.data.local.entity.VariantHuntGroupItemEntity
import com.taytek.basehw.data.local.entity.VariantHuntItemRow
import kotlinx.coroutines.flow.Flow

@Dao
interface VariantHuntDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGroup(entity: VariantHuntGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<VariantHuntGroupItemEntity>)

    @Transaction
    suspend fun insertGroupWithItems(group: VariantHuntGroupEntity, masterIds: List<Long>): Long {
        val groupId = insertGroup(group)
        if (masterIds.isNotEmpty()) {
            insertItems(masterIds.map { VariantHuntGroupItemEntity(groupId, it) })
        }
        return groupId
    }

    @Query(
        """
        SELECT * FROM variant_hunt_groups
        WHERE isActive = 1 AND completedAtMillis IS NULL
        ORDER BY createdAtMillis DESC
        """
    )
    fun observeActiveGroups(): Flow<List<VariantHuntGroupEntity>>

    @Query("SELECT * FROM variant_hunt_groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: Long): VariantHuntGroupEntity?

    @Query(
        """
        UPDATE variant_hunt_groups SET isActive = 0, completedAtMillis = :nowMillis
        WHERE id = :id AND isActive = 1
        """
    )
    suspend fun markGroupCompleted(id: Long, nowMillis: Long)

    @Query(
        """
        SELECT g.id FROM variant_hunt_groups g
        WHERE g.isActive = 1 AND g.completedAtMillis IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM variant_hunt_group_items i
            WHERE i.groupId = g.id
            AND NOT EXISTS (
                SELECT 1 FROM user_cars uc
                WHERE uc.masterDataId = i.masterDataId AND uc.isWishlist = 0
            )
        )
        """
    )
    suspend fun findGroupIdsReadyToComplete(): List<Long>

    @Query("DELETE FROM variant_hunt_groups WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    @Query(
        """
        SELECT m.*,
        CASE WHEN EXISTS (
            SELECT 1 FROM user_cars uc
            WHERE uc.masterDataId = m.id AND uc.isWishlist = 0
        ) THEN 1 ELSE 0 END AS inCollection
        FROM master_data m
        INNER JOIN variant_hunt_group_items i ON i.masterDataId = m.id
        WHERE i.groupId = :groupId
        ORDER BY CASE WHEN m.year IS NULL THEN 1 ELSE 0 END, m.year ASC, m.series ASC, m.seriesNum ASC
        """
    )
    fun observeGroupItemRows(groupId: Long): Flow<List<VariantHuntItemRow>>
}
