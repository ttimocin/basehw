package com.taytek.basehw.data.repository

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.taytek.basehw.domain.model.Brand
import com.taytek.basehw.domain.model.VehicleCondition
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Dışa aktarılmış JSON / CSV / PDF dosyalarından metin tabanlı satırlar üretir (foto yok).
 */
internal object CollectionImportHelper {

    fun parseRows(
        inputStream: InputStream,
        mimeTypeHint: String?,
        conversionRate: Double
    ): Pair<List<ImportRow>, Int> {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return emptyList<ImportRow>() to 1

        val mime = mimeTypeHint?.lowercase().orEmpty()
        val sniffPdf = looksLikePdf(bytes)
        val sniffJson = looksLikeJson(bytes)
        return when {
            sniffPdf || mime.contains("pdf") -> parsePdf(bytes, conversionRate)
            sniffJson || mime.contains("json") ->
                parseJson(String(bytes, StandardCharsets.UTF_8), conversionRate)
            else -> parseCsv(bytes, conversionRate)
        }
    }

    private fun looksLikePdf(bytes: ByteArray): Boolean =
        bytes.size >= 5 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'D'.code.toByte()

    private fun looksLikeJson(bytes: ByteArray): Boolean {
        val s = bytes.take(400).toByteArray().toString(StandardCharsets.UTF_8).trimStart()
        return s.startsWith("{") || s.startsWith("[")
    }

    // --- JSON ---

    private fun jsonValueToString(el: JsonElement?): String? {
        if (el == null || el.isJsonNull) return null
        return when {
            el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asJsonPrimitive.toString()
            el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asJsonPrimitive.asString
            el.isJsonPrimitive && el.asJsonPrimitive.isBoolean -> el.asJsonPrimitive.asBoolean.toString()
            else -> null
        }
    }

    private fun parseJson(text: String, rate: Double): Pair<List<ImportRow>, Int> {
        var parseFailures = 0
        val trimmed = text.trim()
        val root = runCatching { JsonParser.parseString(trimmed) }.getOrElse { return emptyList<ImportRow>() to 1 }
        val array: JsonArray = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> {
                val obj = root.asJsonObject
                when {
                    obj.has("cars") && obj.get("cars").isJsonArray -> obj.getAsJsonArray("cars")
                    obj.has("items") && obj.get("items").isJsonArray -> obj.getAsJsonArray("items")
                    else -> {
                        parseFailures++
                        return emptyList<ImportRow>() to parseFailures
                    }
                }
            }
            else -> return emptyList<ImportRow>() to 1
        }

        val rows = mutableListOf<ImportRow>()
        array.forEach { el ->
            if (!el.isJsonObject) {
                parseFailures++
                return@forEach
            }
            val o = el.asJsonObject
            val row = jsonObjectToRow(o, rate)
            if (row != null) rows.add(row) else parseFailures++
        }
        return rows to parseFailures
    }

    private fun jsonObjectToRow(o: JsonObject, rate: Double): ImportRow? {
        val markaKod = o.get("marka_kod")?.asString?.trim()?.takeIf { it.isNotEmpty() }
        val marka = o.get("marka")?.asString?.trim()?.takeIf { it.isNotEmpty() }
        val brandCode = resolveBrandCode(markaKod, marka) ?: return null

        val model = o.get("model")?.asString?.trim()?.takeIf { it.isNotEmpty() }
        val seri = o.get("seri")?.asString?.trim()?.takeIf { it.isNotEmpty() }
        val seriNo = o.get("seri_no")?.asString?.trim()?.takeIf { it.isNotEmpty() }
        val toy = o.get("toy_no")?.asString?.trim()?.takeIf { it.isNotEmpty() }
        val col = o.get("col_no")?.asString?.trim()?.takeIf { it.isNotEmpty() }
        val yilEl = o.get("yil")
        val yil = when {
            yilEl == null || yilEl.isJsonNull -> null
            yilEl.isJsonPrimitive && yilEl.asJsonPrimitive.isNumber -> yilEl.asInt
            else -> yilEl.asString.trim().toIntOrNull()
        }
        val durum = o.get("durum")?.asString?.trim()?.takeIf { it.isNotEmpty() } ?: "MINT"
        val not = o.get("not")?.asString?.trim() ?: ""
        val konum = o.get("konum")?.asString?.trim() ?: ""

        val fiyatStr = jsonValueToString(o.get("fiyat"))
        val degerStr = jsonValueToString(o.get("deger"))
        val purchaseDisplay = parseMoney(fiyatStr)
        val valueDisplay = parseMoney(degerStr)
        val purchaseBase = purchaseDisplay?.let { if (rate > 0) it / rate else it }
        val valueBase = valueDisplay?.let { if (rate > 0) it / rate else it }

        val adetEl = o.get("adet")
        val adet = when {
            adetEl == null || adetEl.isJsonNull -> 1
            adetEl.isJsonPrimitive && adetEl.asJsonPrimitive.isNumber -> adetEl.asInt.coerceAtLeast(1)
            adetEl.isJsonPrimitive && adetEl.asJsonPrimitive.isString ->
                adetEl.asString.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
            else -> 1
        }
        val favori = o.get("favori")?.takeIf { !it.isJsonNull }?.asBoolean == true
        val custom = o.get("custom")?.takeIf { !it.isJsonNull }?.asBoolean == true
        val wishlist = o.get("wishlist")?.takeIf { !it.isJsonNull }?.asBoolean == true

        return ImportRow(
            brandCode = brandCode,
            modelName = model,
            series = seri,
            seriesNum = seriNo,
            toyNum = toy,
            colNum = col,
            year = yil,
            conditionName = mapConditionName(durum),
            personalNote = not,
            storageLocation = konum,
            purchasePriceBase = purchaseBase,
            estimatedValueBase = valueBase,
            quantity = adet,
            isFavorite = favori,
            isCustom = custom,
            isWishlist = wishlist
        )
    }

    // --- CSV ---

    private fun parseCsv(bytes: ByteArray, rate: Double): Pair<List<ImportRow>, Int> {
        val text = String(bytes, StandardCharsets.UTF_8)
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList<ImportRow>() to 1

        val sep = detectSeparator(lines.first())
        val header = lines.first().split(sep).map { it.trim().lowercase() }
        val headerIndex = buildHeaderIndex(header)

        if (headerIndex["brand"] == null && headerIndex["marka"] == null) {
            return emptyList<ImportRow>() to 1
        }

        var failures = 0
        val rows = mutableListOf<ImportRow>()
        for (line in lines.drop(1)) {
            val cols = splitCsvLine(line, sep)
            if (cols.size < header.size / 2) {
                failures++
                continue
            }
            val r = csvLineToRow(cols, headerIndex, rate)
            if (r != null) rows.add(r) else failures++
        }
        return rows to failures
    }

    private fun detectSeparator(headerLine: String): String {
        val semi = headerLine.count { it == ';' }
        val comma = headerLine.count { it == ',' }
        val tab = headerLine.count { it == '\t' }
        return when {
            tab >= semi && tab >= comma -> "\t"
            semi >= comma -> ";"
            else -> ","
        }
    }

    private fun buildHeaderIndex(header: List<String>): Map<String, Int> {
        val m = mutableMapOf<String, Int>()
        header.forEachIndexed { i, h ->
            val key = when {
                h in listOf("brand", "marka") -> "brand"
                h in listOf("model", "modell", "modelo") -> "model"
                h in listOf("year", "yil", "jahr", "año", "ano") -> "year"
                h in listOf("series", "serie", "seri") -> "series"
                h.contains("toy") && h.contains("num") -> "toy"
                h.contains("col") && h.contains("num") -> "col"
                h in listOf("status", "durum", "estado") -> "status"
                h.contains("price") || h == "fiyat" || h.contains("preis") || h.contains("precio") -> "price"
                h.contains("value") || h == "deger" || h.contains("wert") || h.contains("valor") -> "value"
                h in listOf("note", "not", "notiz") -> "note"
                h in listOf("location", "konum", "standort", "ubicación", "ubicacion") -> "location"
                else -> h
            }
            m[key] = i
        }
        return m
    }

    private fun csvLineToRow(
        cols: List<String>,
        idx: Map<String, Int>,
        rate: Double
    ): ImportRow? {
        fun get(key: String): String? {
            val i = idx[key] ?: return null
            return cols.getOrNull(i)?.trim()?.takeIf { it.isNotEmpty() && it != "-" }
        }

        val brandRaw = get("brand") ?: return null
        val brandCode = resolveBrandCode(null, brandRaw) ?: return null
        val model = get("model")
        val year = get("year")?.toIntOrNull()
        val series = get("series")
        val toy = get("toy")
        val col = get("col")
        val statusRaw = get("status") ?: "MINT"
        val purchaseDisplay = parseMoney(get("price"))
        val valueDisplay = parseMoney(get("value"))
        val purchaseBase = purchaseDisplay?.let { if (rate > 0) it / rate else it }
        val valueBase = valueDisplay?.let { if (rate > 0) it / rate else it }

        return ImportRow(
            brandCode = brandCode,
            modelName = model,
            series = series,
            seriesNum = null,
            toyNum = toy,
            colNum = col,
            year = year,
            conditionName = mapConditionName(statusRaw),
            personalNote = get("note") ?: "",
            storageLocation = get("location") ?: "",
            purchasePriceBase = purchaseBase,
            estimatedValueBase = valueBase,
            quantity = 1,
            isFavorite = false,
            isCustom = false,
            isWishlist = false
        )
    }

    private fun splitCsvLine(line: String, sep: String): List<String> {
        if (sep != ",") return line.split(sep).map { it.trim() }
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> {
                    out.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString().trim())
        return out
    }

    // --- PDF (uygulama tarafından üretilmiş raporlar için kaba metin çıkarımı) ---

    private fun parsePdf(bytes: ByteArray, rate: Double): Pair<List<ImportRow>, Int> {
        val tokens = extractPdfTextLiterals(bytes)
        if (tokens.isEmpty()) return emptyList<ImportRow>() to 1

        val headerIdx = tokens.indexOfFirst {
            it == "Marka" || it == "Brand" || it == "Marke" || it == "Marca"
        }
        if (headerIdx < 0) return emptyList<ImportRow>() to 1

        var i = headerIdx + 7
        val rows = mutableListOf<ImportRow>()
        while (i + 6 < tokens.size) {
            if (tokens[i].startsWith("Sayfa")) break
            val chunk = tokens.subList(i, i + 7).map { it.trim() }
            val row = pdfChunkToRow(chunk, rate)
            if (row != null) rows.add(row) else break
            i += 7
        }
        val failures = if (rows.isEmpty()) 1 else 0
        return rows to failures
    }

    private fun pdfChunkToRow(chunk: List<String>, rate: Double): ImportRow? {
        if (chunk.size < 7) return null
        val brandDisplay = chunk[0].removeSuffix("..").trim()
        val model = chunk[1].removeSuffix("..").trim().takeIf { it.isNotEmpty() && it != "-" }
        val series = chunk[2].removeSuffix("..").trim().takeIf { it.isNotEmpty() && it != "-" }
        val yearStr = chunk[3].trim()
        val year = yearStr.toIntOrNull()
        val statusRaw = chunk[4].trim()
        val purchaseDisplay = parseMoney(chunk[5])
        val valueDisplay = parseMoney(chunk[6])
        val r = if (rate > 0) rate else 1.0
        val purchaseBase = purchaseDisplay?.let { it / r }
        val valueBase = valueDisplay?.let { it / r }

        val brandCode = resolveBrandCode(null, brandDisplay) ?: return null

        return ImportRow(
            brandCode = brandCode,
            modelName = model,
            series = series,
            seriesNum = null,
            toyNum = null,
            colNum = null,
            year = year,
            conditionName = mapConditionName(statusRaw),
            personalNote = "",
            storageLocation = "",
            purchasePriceBase = purchaseBase,
            estimatedValueBase = valueBase,
            quantity = 1,
            isFavorite = false,
            isCustom = false,
            isWishlist = false
        )
    }

    private fun extractPdfTextLiterals(bytes: ByteArray): List<String> {
        val s = bytes.toString(StandardCharsets.ISO_8859_1)
        val out = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            if (s[i] == '(') {
                val sb = StringBuilder()
                i++
                while (i < s.length) {
                    when (s[i]) {
                        '\\' -> {
                            i++
                            if (i < s.length) {
                                sb.append(s[i])
                                i++
                            }
                        }
                        ')' -> {
                            i++
                            out.add(sb.toString())
                            break
                        }
                        else -> {
                            sb.append(s[i])
                            i++
                        }
                    }
                }
            } else i++
        }
        return out
    }

    // --- Ortak ---

    data class ImportRow(
        val brandCode: String,
        val modelName: String?,
        val series: String?,
        val seriesNum: String?,
        val toyNum: String?,
        val colNum: String?,
        val year: Int?,
        val conditionName: String,
        val personalNote: String,
        val storageLocation: String,
        val purchasePriceBase: Double?,
        val estimatedValueBase: Double?,
        val quantity: Int,
        val isFavorite: Boolean,
        val isCustom: Boolean,
        val isWishlist: Boolean
    )

    fun resolveBrandCode(markaKod: String?, markaDisplay: String?): String? {
        if (!markaKod.isNullOrBlank()) {
            val t = markaKod.trim()
            return runCatching { Brand.valueOf(t) }.getOrNull()?.name
                ?: Brand.entries.firstOrNull { it.name.equals(t, ignoreCase = true) }?.name
        }
        if (markaDisplay.isNullOrBlank()) return null
        val t = markaDisplay.trim()
        Brand.entries.firstOrNull { it.displayName.equals(t, ignoreCase = true) }?.let { return it.name }
        Brand.entries.firstOrNull { it.name.equals(t, ignoreCase = true) }?.let { return it.name }
        return Brand.entries.firstOrNull { t.contains(it.displayName, ignoreCase = true) }?.name
    }

    fun parseMoney(s: String?): Double? {
        if (s.isNullOrBlank()) return null
        val t = s.trim()
        if (t == "-" || t == "0.00") return null
        val digits = buildString {
            for (ch in t) {
                when {
                    ch.isDigit() -> append(ch)
                    ch == '.' || ch == ',' -> append(ch)
                }
            }
        }
        if (digits.isEmpty()) return null
        val normalized = when {
            digits.count { it == ',' } == 1 && digits.count { it == '.' } == 0 ->
                digits.replace(',', '.')
            digits.count { it == '.' } > 1 -> digits.replace(".", "").replace(',', '.')
            else -> digits.replace(",", "")
        }
        return normalized.toDoubleOrNull()
    }

    private fun mapConditionName(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return VehicleCondition.MINT.name
        val upper = t.uppercase()
        runCatching { VehicleCondition.valueOf(upper) }.getOrNull()?.let { return it.name }
        return when {
            t.equals("Mint", true) || t.contains("Mint", true) && t.contains("Kapali", true) ||
                t.contains("OVP", true) && t.contains("Mint", true) ||
                t.contains("En caja", true) -> VehicleCondition.MINT.name
            t.contains("Near", true) || t.contains("Near Mint", true) -> VehicleCondition.NEAR_MINT.name
            t.contains("Damaged", true) || t.contains("Hasar", true) || t.contains("Beschädigt", true) ||
                t.contains("Danado", true) -> VehicleCondition.DAMAGED.name
            t.contains("Loose", true) || t.contains("Acilmis", true) || t.contains("Lose", true) ||
                t.contains("Abierto", true) -> VehicleCondition.LOOSE.name
            else -> VehicleCondition.MINT.name
        }
    }

    fun isRowImportable(row: ImportRow, masterResolved: Boolean): Boolean {
        if (masterResolved) return true
        return !row.modelName.isNullOrBlank() ||
            !row.toyNum.isNullOrBlank() ||
            row.year != null ||
            !row.series.isNullOrBlank()
    }
}
