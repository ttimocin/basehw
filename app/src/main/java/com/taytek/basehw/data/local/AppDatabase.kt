package com.taytek.basehw.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.local.dao.UserCarDao
import com.taytek.basehw.data.local.entity.MasterDataEntity
import com.taytek.basehw.data.local.entity.UserCarEntity
import com.taytek.basehw.data.local.entity.CustomCollectionEntity
import com.taytek.basehw.data.local.entity.CollectionCarCrossRef

@Database(
    entities = [
        MasterDataEntity::class, 
        UserCarEntity::class,
        CustomCollectionEntity::class,
        CollectionCarCrossRef::class
    ],
    version = 23,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun masterDataDao(): MasterDataDao
    abstract fun userCarDao(): UserCarDao
    abstract fun customCollectionDao(): com.taytek.basehw.data.local.dao.CustomCollectionDao

    companion object {
        const val DATABASE_NAME = "basehw_db"

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_master_data_brand_toyNum` ON `master_data` (`brand`, `toyNum`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_master_data_brand_modelName_year` ON `master_data` (`brand`, `modelName`, `year`)")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_cars ADD COLUMN additionalPhotosBackup TEXT")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_cars ADD COLUMN backupPhotoUrl TEXT")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to master_data
                db.execSQL("ALTER TABLE master_data ADD COLUMN isSth INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE master_data ADD COLUMN isChase INTEGER NOT NULL DEFAULT 0")
                
                // Migrate existing STH data based on old dataSource name
                db.execSQL("UPDATE master_data SET isSth = 1 WHERE dataSource = 'STH'")
                
                // Create indices for the new columns
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_master_data_isSth` ON `master_data` (`isSth`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_master_data_isChase` ON `master_data` (`isChase`)")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add feature column
                db.execSQL("ALTER TABLE master_data ADD COLUMN feature TEXT")
                
                // Migrate data from isSth and isChase to feature
                db.execSQL("UPDATE master_data SET feature = 'sth' WHERE isSth = 1")
                db.execSQL("UPDATE master_data SET feature = 'chase' WHERE isChase = 1 AND feature IS NULL")
                
                // Create index for feature
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_master_data_feature` ON `master_data` (`feature`)")
                
                // Note: Room doesn't support dropping columns easily in SQLite.
                // We'll leave isSth and isChase columns for now to avoid recreating the whole table,
                // or we can recreate it if we want to be clean. 
                // Given the context of a migration, just adding the new field is safer for data persistence.
                // However, the UserCarWithMaster mapping and other queries might be affected if we don't handle it.
                // Actually, Room ignores properties that are not in the Entity class if they exist in the DB.
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix incorrect 'ye' value to 'chase'
                db.execSQL("UPDATE master_data SET feature = 'chase' WHERE feature = 'ye'")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE master_data ADD COLUMN caseNum TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_master_data_dataSource_year_modelName` ON `master_data` (`dataSource`, `year`, `modelName`)")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE master_data ADD COLUMN dataSource TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_cars ADD COLUMN isSeriesOnly INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE master_data ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_cars ADD COLUMN purchasePrice REAL")
                db.execSQL("ALTER TABLE user_cars ADD COLUMN estimatedValue REAL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_collections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `description` TEXT NOT NULL, 
                        `coverPhotoUrl` TEXT, 
                        `createdAtMillis` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `collection_car_cross_ref` (
                        `collectionId` INTEGER NOT NULL, 
                        `userCarId` INTEGER NOT NULL, 
                        PRIMARY KEY(`collectionId`, `userCarId`), 
                        FOREIGN KEY(`collectionId`) REFERENCES `custom_collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`userCarId`) REFERENCES `user_cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_car_cross_ref_collectionId` ON `collection_car_cross_ref` (`collectionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_car_cross_ref_userCarId` ON `collection_car_cross_ref` (`userCarId`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE custom_collections ADD COLUMN firestoreId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_cars ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite ALTER TABLE doesn't support changing NOT NULL to NULL directly easily.
                // We need to recreate the table.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_cars_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `masterDataId` INTEGER, 
                        `manualModelName` TEXT, 
                        `manualBrand` TEXT, 
                        `manualSeries` TEXT, 
                        `manualSeriesNum` TEXT, 
                        `manualYear` INTEGER, 
                        `manualScale` TEXT, 
                        `manualIsPremium` INTEGER, 
                        `isOpened` INTEGER NOT NULL, 
                        `purchaseDateMillis` INTEGER, 
                        `personalNote` TEXT NOT NULL, 
                        `storageLocation` TEXT NOT NULL, 
                        `firestoreId` TEXT NOT NULL, 
                        `isWishlist` INTEGER NOT NULL, 
                        `userPhotoUrl` TEXT, 
                        `purchasePrice` REAL, 
                        `estimatedValue` REAL, 
                        `isFavorite` INTEGER NOT NULL,
                        FOREIGN KEY(`masterDataId`) REFERENCES `master_data`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("""
                    INSERT INTO user_cars_new (
                        id, masterDataId, isOpened, purchaseDateMillis, personalNote, 
                        storageLocation, firestoreId, isWishlist, userPhotoUrl, 
                        purchasePrice, estimatedValue, isFavorite
                    )
                    SELECT 
                        id, masterDataId, isOpened, purchaseDateMillis, personalNote, 
                        storageLocation, firestoreId, isWishlist, userPhotoUrl, 
                        purchasePrice, estimatedValue, isFavorite 
                    FROM user_cars
                """)
                db.execSQL("DROP TABLE user_cars")
                db.execSQL("ALTER TABLE user_cars_new RENAME TO user_cars")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_cars_masterDataId` ON `user_cars` (`masterDataId`)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE master_data ADD COLUMN category TEXT")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_cars ADD COLUMN additionalPhotos TEXT")
            }
        }
    }
}

