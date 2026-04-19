package com.taytek.basehw.domain.model

enum class HwCardType {
    SHORT,
    LONG;

    fun toStorageCode(): String = name

    companion object {
        fun fromStorage(s: String?): HwCardType? = when (s?.uppercase()) {
            "SHORT" -> SHORT
            "LONG" -> LONG
            else -> null
        }
    }
}

object HwCardTypeRules {
    /**
     * Katalogdan seçim: HW ana hat + TH/STH kart tipi. [MasterData.isPremium] Premium setleri (Car Culture, vb.)
     * ayırır; [MasterData.dataSource] metninde "premium" aramak uzaktan senkron kaynaklı yanlış negatiflere yol açıyordu.
     * `hotwheels/other` kaynağı hariç.
     */
    fun showForMaster(master: MasterData?): Boolean {
        if (master?.brand != Brand.HOT_WHEELS) return false
        val norm = master.dataSource.trim().replace('\\', '/').lowercase()
        if (norm == "hotwheels/other" || norm.endsWith("/other")) return false
        // Premium setleri genelde isPremium=true; ana hat/TH bazen yanlış işaretlenirse dataSource ile düzelt
        if (master.isPremium) {
            return norm.isEmpty() || norm == "hotwheels"
        }
        return true
    }

    fun showForManual(isManualMode: Boolean, manualBrand: Brand): Boolean =
        isManualMode && manualBrand == Brand.HOT_WHEELS

    fun showForUserCar(car: UserCar): Boolean {
        val master = car.masterData
        if (master != null) return showForMaster(master)
        return car.manualBrand == Brand.HOT_WHEELS
    }
}
