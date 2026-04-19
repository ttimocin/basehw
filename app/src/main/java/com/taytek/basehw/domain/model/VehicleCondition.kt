package com.taytek.basehw.domain.model

import com.taytek.basehw.R

enum class VehicleCondition(
    val titleRes: Int,
    val hexColor: Long
) {
    LOOSE(R.string.condition_loose, 0xFF757575),        // Gri (Kutusuz)
    MINT(R.string.condition_mint, 0xFF4CAF50),         // Yeşil (Kusursuz)
    NEAR_MINT(R.string.condition_near_mint, 0xFF2196F3), // Mavi (Çok İyi)
    DAMAGED(R.string.condition_damaged, 0xFFF44336);    // Kırmızı (Hasarlı)

    companion object {
        fun fromString(value: String?): VehicleCondition {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MINT
        }
    }
}
