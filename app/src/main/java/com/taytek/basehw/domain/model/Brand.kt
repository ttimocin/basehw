package com.taytek.basehw.domain.model

import androidx.compose.ui.graphics.Color

enum class Brand(val displayName: String, val shortCode: String) {
    HOT_WHEELS("Hot Wheels", "HW"),
    MATCHBOX("Matchbox", "MBX"),
    MINI_GT("MiniGT", "MiniGT"),
    MAJORETTE("Majorette", "MAJ"),
    JADA("Jada", "Jada"),
    SIKU("Siku", "SIKU"),
    KAIDO_HOUSE("Kaido House", "KH"),
    GREENLIGHT("Greenlight", "GL")
}

fun Brand.toColor(): Color {
    return when (this) {
        Brand.HOT_WHEELS -> com.taytek.basehw.ui.theme.HotWheelsRed
        Brand.MATCHBOX   -> com.taytek.basehw.ui.theme.MatchboxBlue
        Brand.MINI_GT    -> com.taytek.basehw.ui.theme.MiniGTSilver
        Brand.MAJORETTE  -> com.taytek.basehw.ui.theme.MajoretteYellow
        Brand.JADA       -> com.taytek.basehw.ui.theme.JadaPurple
        Brand.SIKU       -> com.taytek.basehw.ui.theme.SikuBlue
        Brand.KAIDO_HOUSE -> com.taytek.basehw.ui.theme.KaidoHouseColor
        Brand.GREENLIGHT -> com.taytek.basehw.ui.theme.GreenlightGreen
    }
}

fun Brand.toIcon(): Int {
    return when (this) {
        Brand.HOT_WHEELS -> com.taytek.basehw.R.drawable.hotwheels
        Brand.MATCHBOX   -> com.taytek.basehw.R.drawable.matchbox
        Brand.MINI_GT    -> com.taytek.basehw.R.drawable.minigt
        Brand.MAJORETTE  -> com.taytek.basehw.R.drawable.majorette
        Brand.JADA       -> com.taytek.basehw.R.drawable.jada
        Brand.SIKU       -> com.taytek.basehw.R.drawable.siku
        Brand.KAIDO_HOUSE -> com.taytek.basehw.R.drawable.kaido
        Brand.GREENLIGHT -> com.taytek.basehw.R.drawable.greenlight
    }
}

