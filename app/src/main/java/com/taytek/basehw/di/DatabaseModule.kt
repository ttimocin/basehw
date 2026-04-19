package com.taytek.basehw.di

import android.content.Context
import androidx.room.Room
import com.taytek.basehw.data.local.AppDatabase
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.local.dao.UserCarDao
import com.taytek.basehw.data.local.dao.CustomCollectionDao
import com.taytek.basehw.data.local.dao.VariantHuntDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_4_5)
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .addMigrations(AppDatabase.MIGRATION_6_7)
            .addMigrations(AppDatabase.MIGRATION_7_8)
            .addMigrations(AppDatabase.MIGRATION_8_9)
            .addMigrations(AppDatabase.MIGRATION_9_10)
            .addMigrations(AppDatabase.MIGRATION_10_11)
            .addMigrations(AppDatabase.MIGRATION_11_12)
            .addMigrations(AppDatabase.MIGRATION_12_13)
            .addMigrations(AppDatabase.MIGRATION_13_14)
                .addMigrations(AppDatabase.MIGRATION_14_15)
                .addMigrations(AppDatabase.MIGRATION_15_16)
                .addMigrations(AppDatabase.MIGRATION_16_17)
                .addMigrations(AppDatabase.MIGRATION_17_18)
                .addMigrations(AppDatabase.MIGRATION_19_20)
                .addMigrations(AppDatabase.MIGRATION_20_21)
                .addMigrations(AppDatabase.MIGRATION_21_22)
                .addMigrations(AppDatabase.MIGRATION_22_23)
                .addMigrations(AppDatabase.MIGRATION_23_24)
                .addMigrations(AppDatabase.MIGRATION_24_25)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMasterDataDao(db: AppDatabase): MasterDataDao = db.masterDataDao()

    @Provides
    fun provideUserCarDao(db: AppDatabase): UserCarDao = db.userCarDao()

    @Provides
    fun provideCustomCollectionDao(db: AppDatabase): CustomCollectionDao = db.customCollectionDao()

    @Provides
    fun provideVariantHuntDao(db: AppDatabase): VariantHuntDao = db.variantHuntDao()
}
