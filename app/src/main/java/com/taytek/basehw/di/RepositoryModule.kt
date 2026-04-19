package com.taytek.basehw.di

import com.taytek.basehw.data.repository.MasterDataRepositoryImpl
import com.taytek.basehw.data.repository.NewsRepositoryImpl
import com.taytek.basehw.data.repository.SupabaseSyncRepositoryImpl
import com.taytek.basehw.data.repository.UserCarRepositoryImpl
import com.taytek.basehw.domain.repository.MasterDataRepository
import com.taytek.basehw.domain.repository.NewsRepository
import com.taytek.basehw.domain.repository.SupabaseSyncRepository
import com.taytek.basehw.domain.repository.UserCarRepository
import com.taytek.basehw.domain.repository.CustomCollectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMasterDataRepository(
        impl: MasterDataRepositoryImpl
    ): MasterDataRepository

    @Binds
    @Singleton
    abstract fun bindUserCarRepository(
        impl: UserCarRepositoryImpl
    ): UserCarRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: com.taytek.basehw.data.remote.firebase.AuthRepositoryImpl
    ): com.taytek.basehw.domain.repository.AuthRepository

    @Binds
    @Singleton
    abstract fun bindCustomCollectionRepository(
        impl: com.taytek.basehw.data.repository.CustomCollectionRepositoryImpl
    ): CustomCollectionRepository

    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(
        impl: com.taytek.basehw.data.repository.CurrencyRepositoryImpl
    ): com.taytek.basehw.domain.repository.CurrencyRepository

    @Binds
    @Singleton
    abstract fun bindCommunityRepository(
        impl: com.taytek.basehw.data.repository.CommunityRepositoryImpl
    ): com.taytek.basehw.domain.repository.CommunityRepository

    @Binds
    @Singleton
    abstract fun bindSupabaseSyncRepository(
        impl: SupabaseSyncRepositoryImpl
    ): SupabaseSyncRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        impl: NewsRepositoryImpl
    ): NewsRepository
}
