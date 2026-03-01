package com.taytek.basehw.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.local.dao.UserCarDao
import com.taytek.basehw.data.local.entity.MasterDataEntity
import com.taytek.basehw.data.local.entity.UserCarEntity

@Database(
    entities = [MasterDataEntity::class, UserCarEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun masterDataDao(): MasterDataDao
    abstract fun userCarDao(): UserCarDao

    companion object {
        const val DATABASE_NAME = "basehw_db"
    }
}
