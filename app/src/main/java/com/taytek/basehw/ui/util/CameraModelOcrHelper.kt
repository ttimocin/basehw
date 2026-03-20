package com.taytek.basehw.ui.util

import android.content.Context
import android.net.Uri
import com.taytek.basehw.domain.model.Brand
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object CameraModelOcrHelper {

    data class OcrDetectionResult(
        val query: String?,
        val detectedBrand: Brand?
    )

    private val ignoredKeywords = setOf(
        "hot wheels", "matchbox", "mini gt", "majorette", "jada", "siku",
        "mattel", "premium", "collector", "scale", "series", "assortment",
        "warning", "age", "ages", "made in", "metal", "die-cast"
    )

    suspend fun detectFromImage(context: Context, imageUri: Uri): OcrDetectionResult {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val result = recognizer.process(image).await()
            val rawLines = result.textBlocks
                .flatMap { block -> block.lines.map { it.text } }

            val candidates = rawLines
                .map { normalize(it) }
                .filter { it.isNotBlank() }

            val query = pickBestCandidate(candidates)
            val fullTextLower = rawLines.joinToString(" ").lowercase()
            val detectedBrand = detectBrandHint(fullTextLower)

            OcrDetectionResult(query = query, detectedBrand = detectedBrand)
        } finally {
            recognizer.close()
        }
    }

    private fun detectBrandHint(fullTextLower: String): Brand? {
        return when {
            "hot wheels" in fullTextLower || "hotwheels" in fullTextLower -> Brand.HOT_WHEELS
            "matchbox" in fullTextLower -> Brand.MATCHBOX
            "mini gt" in fullTextLower || "minigt" in fullTextLower -> Brand.MINI_GT
            "majorette" in fullTextLower -> Brand.MAJORETTE
            "jada" in fullTextLower -> Brand.JADA
            "siku" in fullTextLower -> Brand.SIKU
            else -> null
        }
    }

    private fun pickBestCandidate(lines: List<String>): String? {
        if (lines.isEmpty()) return null

        val scored = lines
            .map { line -> line to scoreLine(line) }
            .sortedByDescending { it.second }

        val best = scored.firstOrNull { it.second >= 20 }?.first ?: return null
        return best.take(40)
    }

    private fun scoreLine(line: String): Int {
        val lower = line.lowercase()
        val words = line.split(" ").filter { it.isNotBlank() }

        var score = 0

        if (line.length in 4..40) score += 8
        if (words.size in 2..5) score += 10
        if (line.any { it.isLetter() } && line.any { it.isDigit() }) score += 12
        if (line.any { it.isLetter() }) score += 4

        if (ignoredKeywords.any { lower.contains(it) }) score -= 12
        if (words.size <= 1 && line.length < 5) score -= 10

        return score
    }

    private fun normalize(raw: String): String {
        return raw
            .replace(Regex("[^A-Za-z0-9 +\\-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
