package com.taytek.basehw.domain.model

enum class AppCurrency(val code: String, val symbol: String) {
    USD("USD", "$"),
    EUR("EUR", "€"),
    GBP("GBP", "£"),
    TRY("TRY", "₺"),
    JPY("JPY", "¥"),
    CHF("CHF", "CHF"),
    CAD("CAD", "$"),
    AUD("AUD", "$"),
    CNY("CNY", "¥");

    companion object {
        fun fromCode(code: String): AppCurrency {
            return entries.find { it.code == code } ?: USD
        }
    }
}
