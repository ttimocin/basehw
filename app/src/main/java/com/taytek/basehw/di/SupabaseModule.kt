package com.taytek.basehw.di

import com.taytek.basehw.BuildConfig
import com.taytek.basehw.data.remote.firebase.RemoteConfigDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(
        remoteConfigDataSource: RemoteConfigDataSource
    ): SupabaseClient {
        val configuredUrl = BuildConfig.SUPABASE_URL
            .ifBlank { remoteConfigDataSource.getPhotoBackupSupabaseUrl() }
        val configuredAnon = BuildConfig.SUPABASE_ANON_KEY
            .ifBlank { remoteConfigDataSource.getPhotoBackupApiKey() }

        val supabaseUrl = if (
            configuredUrl.isBlank() ||
            configuredUrl.contains("example.supabase.co", ignoreCase = true)
        ) {
            throw IllegalStateException("Supabase URL is missing or placeholder. Configure SUPABASE_URL.")
        } else {
            configuredUrl
        }

        val supabaseAnonKey = if (
            configuredAnon.isBlank() ||
            configuredAnon.contains("public-anon-key-placeholder", ignoreCase = true)
        ) {
            throw IllegalStateException("Supabase anon key is missing or placeholder. Configure SUPABASE_ANON_KEY.")
        } else {
            configuredAnon
        }

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseAnonKey
        ) {
            install(Postgrest)
            install(Storage)
            install(Realtime)

            accessToken = {
                runCatching {
                    FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                }.getOrNull()
            }
        }
    }
}
