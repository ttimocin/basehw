package com.taytek.basehw.domain.util

import com.taytek.basehw.BuildConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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
class ContentModerator @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
        
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun validateContent(text: String): Result<ModerationResult> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success(ModerationResult(true))

        val apiKey = BuildConfig.GROQ_API_KEY
        val url = "https://api.groq.com/openai/v1/chat/completions"
        
        try {
            // JSON'u manuel String olarak değil, JSONObject ile güvenli oluşturuyoruz
            val root = JSONObject()
            root.put("model", "llama-3.3-70b-versatile")
            
            val messages = JSONArray()
            
            // Sistem mesajı (Talimatlar)
            val systemMsg = JSONObject()
            systemMsg.put("role", "system")
            systemMsg.put("content", "Sen bir içerik moderatörüsün. Metni küfür ve nefret söylemi açısından incele. Yanıtı SADECE şu formatta bir JSON objesi olarak ver: {\"is_safe\": true} veya {\"is_safe\": false, \"reason\": \"...\"}")
            messages.put(systemMsg)
            
            // Kullanıcı mesajı (İncelenecek metin)
            val userMsg = JSONObject()
            userMsg.put("role", "user")
            userMsg.put("content", text)
            messages.put(userMsg)
            
            root.put("messages", messages)
            
            // JSON Modunu zorunlu kılıyoruz
            val responseFormat = JSONObject()
            responseFormat.put("type", "json_object")
            root.put("response_format", responseFormat)
            root.put("temperature", 0)

            val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Groq Hatası: ${response.code} - $responseBody"))
            }

            // Gelen yanıtın içindeki mesajı çekiyoruz
            val responseJson = JSONObject(responseBody)
            val choices = responseJson.getJSONArray("choices")
            val aiContent = choices.getJSONObject(0).getJSONObject("message").getString("content")
            
            val result = json.decodeFromString<ModerationResult>(aiContent)
            Result.success(result)

        } catch (e: Exception) {
            Result.failure(Exception("Hata: ${e.localizedMessage}"))
        }
    }
}