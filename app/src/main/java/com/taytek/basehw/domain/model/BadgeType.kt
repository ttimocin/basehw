package com.taytek.basehw.domain.model

import androidx.compose.ui.graphics.Color

enum class BadgeType(
    val titleRes: Int,
    val descRes: Int,
    val emoji: String,
    val color: Long,
    val iconRes: Int? = null
) {
    // Collection size badges
    FIRST_CAR(
        titleRes = com.taytek.basehw.R.string.badge_first_car_title,
        descRes = com.taytek.basehw.R.string.badge_first_car_desc,
        emoji = "🚗",
        color = 0xFF42A5F5,
        iconRes = com.taytek.basehw.R.drawable.first_car
    ),
    TEN_CARS(
        titleRes = com.taytek.basehw.R.string.badge_ten_cars_title,
        descRes = com.taytek.basehw.R.string.badge_ten_cars_desc,
        emoji = "🔟",
        color = 0xFF66BB6A,
        iconRes = com.taytek.basehw.R.drawable.ten_cars
    ),
    FIFTY_CARS(
        titleRes = com.taytek.basehw.R.string.badge_fifty_cars_title,
        descRes = com.taytek.basehw.R.string.badge_fifty_cars_desc,
        emoji = "🌟",
        color = 0xFFFFA726,
        iconRes = com.taytek.basehw.R.drawable.fifty_cars
    ),
    HUNDRED_CARS(
        titleRes = com.taytek.basehw.R.string.badge_hundred_cars_title,
        descRes = com.taytek.basehw.R.string.badge_hundred_cars_desc,
        emoji = "💯",
        color = 0xFFE53935,
        iconRes = com.taytek.basehw.R.drawable.hundred_cars
    ),

    // Premium / brand specific
    PREMIUM_HUNTER(
        titleRes = com.taytek.basehw.R.string.badge_premium_hunter_title,
        descRes = com.taytek.basehw.R.string.badge_premium_hunter_desc,
        emoji = "🏁",
        color = 0xFF7B1FA2,
        iconRes = com.taytek.basehw.R.drawable.premium_hunter
    ),
    MATCHBOX_FAN(
        titleRes = com.taytek.basehw.R.string.badge_matchbox_fan_title,
        descRes = com.taytek.basehw.R.string.badge_matchbox_fan_desc,
        emoji = "🟦",
        color = 0xFF1565C0,
        iconRes = com.taytek.basehw.R.drawable.matchbox_fan
    ),
    MULTI_BRAND(
        titleRes = com.taytek.basehw.R.string.badge_multi_brand_title,
        descRes = com.taytek.basehw.R.string.badge_multi_brand_desc,
        emoji = "🌈",
        color = 0xFF26C6DA,
        iconRes = com.taytek.basehw.R.drawable.multi_brand
    ),

    // Financial (USD based)
    SILVER_COLLECTOR(
        titleRes = com.taytek.basehw.R.string.badge_silver_collector_title,
        descRes = com.taytek.basehw.R.string.badge_silver_collector_desc,
        emoji = "🥈",
        color = 0xFF9E9E9E
    ),
    GOLDEN_COLLECTOR(
        titleRes = com.taytek.basehw.R.string.badge_golden_collector_title,
        descRes = com.taytek.basehw.R.string.badge_golden_collector_desc,
        emoji = "🥇",
        color = 0xFFFFD700
    ),

    // Custom
    CUSTOM_MAKER(
        titleRes = com.taytek.basehw.R.string.badge_custom_maker_title,
        descRes = com.taytek.basehw.R.string.badge_custom_maker_desc,
        emoji = "🎨",
        color = 0xFF4CAF50
    ),

    // Box status
    MOC_COLLECTOR(
        titleRes = com.taytek.basehw.R.string.badge_moc_collector_title,
        descRes = com.taytek.basehw.R.string.badge_moc_collector_desc,
        emoji = "📦",
        color = 0xFF00838F,
        iconRes = com.taytek.basehw.R.drawable.moc_collector
    )
}
