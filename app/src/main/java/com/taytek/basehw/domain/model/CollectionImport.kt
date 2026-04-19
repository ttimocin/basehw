package com.taytek.basehw.domain.model

enum class CollectionImportMode {
    /** Mevcut kayıtlarla çakışanları atla, yeni olanları ekle. */
    MERGE,
    /** Yerel koleksiyonu ve özel klasör eşlemelerini sil, sonra içe aktar. */
    REPLACE
}

data class CollectionImportStats(
    val added: Int,
    val skippedDuplicates: Int,
    val skippedIncomplete: Int,
    val parseFailures: Int
)
