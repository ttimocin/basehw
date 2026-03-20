package com.taytek.basehw.data.remote.dto

@androidx.annotation.Keep
data class CurrencyResponse(
    @com.google.gson.annotations.SerializedName("amount") val amount: Double,
    @com.google.gson.annotations.SerializedName("base") val base: String,
    @com.google.gson.annotations.SerializedName("date") val date: String,
    @com.google.gson.annotations.SerializedName("rates") val rates: Map<String, Double>
)
