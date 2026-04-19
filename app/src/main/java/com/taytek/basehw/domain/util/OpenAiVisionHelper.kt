package com.taytek.basehw.domain.util

import com.taytek.basehw.data.remote.firebase.RemoteConfigDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class VisionAnalysisResult(
    val brand: String? = null,
    val model: String? = null,
    val series: String? = null,
    val year: String? = null,
    val condition: String? = null,
    val conditionNote: String? = null,
    val estimatedValue: Double? = null,
    val currency: String = "$"
)

@Singleton
class OpenAiVisionHelper @Inject constructor(
    private val remoteConfig: RemoteConfigDataSource
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        /**
         * Converts a Uri image to a Base64 encoded string with optimization.
         */
        fun convertUriToBase64(context: Context, imageUri: Uri): String? {
            return try {
                val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
                val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                
                // Resize if too large (max 1024px width/height for efficiency)
                val maxSize = 1024
                val width = originalBitmap.width
                val height = originalBitmap.height
                val scale = (maxSize.toFloat() / maxOf(width, height)).coerceAtMost(1f)
                
                val scaledBitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (width * scale).toInt(),
                        (height * scale).toInt(),
                        true
                    )
                } else {
                    originalBitmap
                }

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val byteArray = outputStream.toByteArray()
                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun analyzeVehicleImage(base64Image: String): Result<VisionAnalysisResult> = withContext(Dispatchers.IO) {
        // Use Supabase Edge Function instead of direct OpenAI API call
        // API key is stored securely in Supabase secrets, not in the APK
        val supabaseUrl = remoteConfig.getPhotoBackupSupabaseUrl().trim().trimEnd('/')
        val supabaseAnonKey = remoteConfig.getPhotoBackupApiKey().trim()

        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            return@withContext Result.failure(Exception("Supabase yapılandırması eksik."))
        }

        val edgeFunctionUrl = "$supabaseUrl/functions/v1/analyze-vehicle"

        try {
            val requestBody = JSONObject().apply {
                put("base64_image", base64Image)
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
                return@withContext Result.failure(Exception("Görsel analiz hizmeti hatası: ${response.code}"))
            }

            // Parse result
            val resultJson = JSONObject(responseBody)
            val result = VisionAnalysisResult(
                brand = resultJson.optString("brand"),
                model = resultJson.optString("model"),
                series = resultJson.optString("series"),
                year = resultJson.optString("year"),
                condition = resultJson.optString("condition"),
                conditionNote = resultJson.optString("conditionNote"),
                estimatedValue = resultJson.optDouble("estimatedValue", 0.0)
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}