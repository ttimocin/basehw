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
        "warning", "age", "ages", "made in", "metal", "die-cast",
        "treasure hunt", "super treasure", "factory sealed", "new for",
        "first edition", "team transport", "car culture", "boulevard",
        "real riders", "international", "long card", "short card",
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
            "minigt" in fullTextLower || "mini gt" in fullTextLower -> Brand.MINI_GT
            "majorette" in fullTextLower -> Brand.MAJORETTE
            "jada" in fullTextLower -> Brand.JADA
            "siku" in fullTextLower -> Brand.SIKU
            "kaido" in fullTextLower -> Brand.KAIDO_HOUSE
            else -> null
        }
    }

    private fun pickBestCandidate(lines: List<String>): String? {
        if (lines.isEmpty()) return null

        val scored = lines.distinct()
            .map { line -> line to scoreLine(line) }
            .sortedByDescending { it.second }

        val minScore = 20
        val qualified = scored.filter { it.second >= minScore }
        if (qualified.isEmpty()) return null

        val (topLine, _) = qualified.first()
        if (looksLikeSkuOrCode(topLine)) {
            val alt = qualified.drop(1).firstOrNull { (line, s) ->
                !looksLikeSkuOrCode(line) && s >= minScore - 6
            }
            if (alt != null) return alt.first.take(40)
        }
        return topLine.take(40)
    }

    /**
     * Product / assortment style codes (e.g. FYG83, L259) — not multi-word model names.
     * Avoids matching performance trims like GT350 (2 letters + 3 digits).
     */
    private fun looksLikeSkuOrCode(line: String): Boolean {
        val t = line.trim()
        if (t.length < 3) return false

        val letters = t.count { it.isLetter() }
        val digits = t.count { it.isDigit() }
        if (letters + digits < 3) return false

        val words = t.split(" ").filter { it.isNotBlank() }

        if (words.size == 1) {
            val w = words[0]
            if (threeLettersTwoDigitsRegex.matches(w)) return true
            if (singleLetterManyDigitsRegex.matches(w)) return true
            if (longNumericWithOptionalLettersRegex.matches(w)) return true
        }

        if (letters <= 3 && digits >= 5) return true
        if (digits >= 6 && letters <= 4) return true

        return false
    }

    /** e.g. FYG83, HCT04 */
    private val threeLettersTwoDigitsRegex = Regex("^[A-Za-z]{3}\\d{2}$")

    /** e.g. L259 */
    private val singleLetterManyDigitsRegex = Regex("^[A-Za-z]\\d{3,}$")

    /** e.g. 1234, 12345A */
    private val longNumericWithOptionalLettersRegex = Regex("^\\d{4,}[A-Za-z]{0,2}$")

    private fun scoreLine(line: String): Int {
        val lower = line.lowercase()
        val words = line.split(" ").filter { it.isNotBlank() }

        var score = 0

        if (line.length in 4..40) score += 8
        if (words.size in 2..5) score += 10
        if (line.any { it.isLetter() }) score += 4

        val hasLettersAndDigits = line.any { it.isLetter() } && line.any { it.isDigit() }
        if (hasLettersAndDigits) {
            if (looksLikeSkuOrCode(line)) score -= 24
            else score += 10
        }

        if (words.size >= 2) {
            val letterCount = line.count { it.isLetter() }
            val digitCount = line.count { it.isDigit() }
            if (letterCount >= digitCount * 2 && letterCount >= 6) score += 6
        }

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
