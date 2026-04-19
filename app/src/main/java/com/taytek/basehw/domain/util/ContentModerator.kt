package com.taytek.basehw.domain.util

import com.taytek.basehw.data.remote.firebase.RemoteConfigDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Serializable
data class ModerationResult(
    val is_safe: Boolean,
    val reason: String? = null
)

@Singleton
class ContentModerator @Inject constructor(
    private val remoteConfig: RemoteConfigDataSource
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun validateContent(text: String): Result<ModerationResult> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success(ModerationResult(true))

        // Use Supabase Edge Function instead of direct Groq API call
        // API key is stored securely in Supabase secrets, not in the APK
        val supabaseUrl = remoteConfig.getPhotoBackupSupabaseUrl().trim().trimEnd('/')
        val supabaseAnonKey = remoteConfig.getPhotoBackupApiKey().trim()

        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            return@withContext Result.failure(Exception("Supabase yapılandırması eksik."))
        }

        val edgeFunctionUrl = "$supabaseUrl/functions/v1/moderate-content"

        try {
            val requestBody = JSONObject().apply {
                put("text", text)
                put("lang", java.util.Locale.getDefault().language)
            }

            val request = Request.Builder()
                .url(edgeFunctionUrl)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Moderasyon hizmeti hatası: ${response.code}"))
            }

            val result = json.decodeFromString<ModerationResult>(responseBody)
            Result.success(result)

        } catch (e: Exception) {
            // Fallback: Moderation service unavailable, allow content
            android.util.Log.w("ContentModerator", "Moderation service error, allowing content: ${e.localizedMessage}")
            Result.success(ModerationResult(is_safe = true, reason = "Moderation unavailable"))
        }
    }
}