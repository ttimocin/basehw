package com.taytek.basehw.di

import com.taytek.basehw.data.repository.MasterDataRepositoryImpl
import com.taytek.basehw.data.repository.UserCarRepositoryImpl
import com.taytek.basehw.domain.repository.MasterDataRepository
import com.taytek.basehw.domain.repository.UserCarRepository
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
}
