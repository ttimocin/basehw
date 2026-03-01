package com.taytek.basehw.di

import android.content.Context
import androidx.room.Room
import com.taytek.basehw.data.local.AppDatabase
import com.taytek.basehw.data.local.dao.MasterDataDao
import com.taytek.basehw.data.local.dao.UserCarDao
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
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMasterDataDao(db: AppDatabase): MasterDataDao = db.masterDataDao()

    @Provides
    fun provideUserCarDao(db: AppDatabase): UserCarDao = db.userCarDao()
}
