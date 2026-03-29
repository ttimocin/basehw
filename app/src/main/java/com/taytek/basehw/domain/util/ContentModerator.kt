package com.taytek.basehw.domain.util

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.taytek.basehw.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ModerationResult(
    val is_safe: Boolean,
    val reason: String? = null
)

@Singleton
class ContentModerator @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }
    
    // Safety settings to block harmful content at the model level
    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
    )

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        safetySettings = safetySettings,
        systemInstruction = content { 
            text("Sen bir topluluk moderatörüsün. Sana gelen metni ırkçılık, saldırganlık, küfür ve nefret söylemi açısından incele. " +
                 "Eğer metin uygunsuzsa {\"is_safe\": false, \"reason\": \"Kısa Türkçe sebep\"} şeklinde, " +
                 "uygunsa {\"is_safe\": true} şeklinde SADECE geçerli bir JSON olarak yanıt ver. Başka açıklama yapma.")
        }
    )

    suspend fun validateContent(text: String): Result<ModerationResult> {
        if (text.isBlank()) return Result.success(ModerationResult(true))
        
        return try {
            // Anonymized: only the text is sent
            val response = model.generateContent(text)
            val responseText = response.text?.trim() ?: return Result.failure(Exception("AI'dan boş cevap geldi."))
            
            // Extract JSON if AI surrounds it with markdown code blocks
            val cleanJson = responseText.removePrefix("```json").removeSuffix("```").trim()
            val result = json.decodeFromString<ModerationResult>(cleanJson)
            Result.success(result)
        } catch (e: Exception) {
            // If AI blocks it due to safety filters, it throws an exception or returns null text
            if (e.message?.contains("SAFETY") == true) {
                Result.success(ModerationResult(false, "İçerik güvenlik filtrelerine takıldı (saldırganlık veya nefret söylemi)."))
            } else {
                Result.failure(e)
            }
        }
    }
}
