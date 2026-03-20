package com.taytek.basehw.domain.model

data class CurrencyRates(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
