package com.taytek.basehw.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  // Offline-first caching
                .build()
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): com.google.firebase.remoteconfig.FirebaseRemoteConfig {
        return com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): com.google.firebase.storage.FirebaseStorage {
        return com.google.firebase.storage.FirebaseStorage.getInstance()
    }
}
