package com.taytek.basehw.domain.model

enum class BadgeType(
    val title: String,
    val minScoreInclusive: Double,
    val maxScoreExclusive: Double?,
    val color: Long
) {
    ROOKIE(
        title = "Rookie",
        minScoreInclusive = 0.0,
        maxScoreExclusive = 10.0,
        color = 0xFF9E9E9E
    ),
    BEGINNER(
        title = "Beginner",
        minScoreInclusive = 10.0,
        maxScoreExclusive = 20.0,
        color = 0xFF90A4AE
    ),
    FIRST_RIDE(
        title = "First Ride",
        minScoreInclusive = 20.0,
        maxScoreExclusive = 30.0,
        color = 0xFF64B5F6
    ),
    STREET_SCOUT(
        title = "Street Scout",
        minScoreInclusive = 30.0,
        maxScoreExclusive = 45.0,
        color = 0xFF4FC3F7
    ),
    GARAGE_BUILDER(
        title = "Garage Builder",
        minScoreInclusive = 45.0,
        maxScoreExclusive = 60.0,
        color = 0xFF26A69A
    ),
    LOT_HUNTER(
        title = "Lot Hunter",
        minScoreInclusive = 60.0,
        maxScoreExclusive = 80.0,
        color = 0xFF26C6DA
    ),
    COLLECTION_DRIVER(
        title = "Collection Driver",
        minScoreInclusive = 80.0,
        maxScoreExclusive = 100.0,
        color = 0xFF66BB6A
    ),
    COLLECTION_RIDER(
        title = "Collection Rider",
        minScoreInclusive = 100.0,
        maxScoreExclusive = 125.0,
        color = 0xFF7CB342
    ),
    COLLECTOR(
        title = "Collector",
        minScoreInclusive = 125.0,
        maxScoreExclusive = 150.0,
        color = 0xFF9CCC65
    ),
    SKILLED_COLLECTOR(
        title = "Skilled Collector",
        minScoreInclusive = 150.0,
        maxScoreExclusive = 180.0,
        color = 0xFFD4E157
    ),
    ADVANCED_COLLECTOR(
        title = "Advanced Collector",
        minScoreInclusive = 180.0,
        maxScoreExclusive = 220.0,
        color = 0xFFFFEE58
    ),
    SERIOUS_COLLECTOR(
        title = "Serious Collector",
        minScoreInclusive = 220.0,
        maxScoreExclusive = 270.0,
        color = 0xFFFFCA28
    ),
    ELITE_COLLECTOR(
        title = "Elite Collector",
        minScoreInclusive = 270.0,
        maxScoreExclusive = 330.0,
        color = 0xFFFFA726
    ),
    PREMIUM_SEEKER(
        title = "Premium Seeker",
        minScoreInclusive = 330.0,
        maxScoreExclusive = 400.0,
        color = 0xFFFF9800
    ),
    VAULT_KEEPER(
        title = "Vault Keeper",
        minScoreInclusive = 400.0,
        maxScoreExclusive = 480.0,
        color = 0xFFFF7043
    ),
    TREASURE_HUNTER(
        title = "Treasure Hunter",
        minScoreInclusive = 480.0,
        maxScoreExclusive = 570.0,
        color = 0xFFEF5350
    ),
    MASTER_COLLECTOR(
        title = "Master Collector",
        minScoreInclusive = 570.0,
        maxScoreExclusive = 680.0,
        color = 0xFFAB47BC
    ),
    GRAND_COLLECTOR(
        title = "Grand Collector",
        minScoreInclusive = 680.0,
        maxScoreExclusive = 820.0,
        color = 0xFF7E57C2
    ),
    HALL_OF_FAME(
        title = "Hall of Fame",
        minScoreInclusive = 820.0,
        maxScoreExclusive = 1000.0,
        color = 0xFF5C6BC0
    ),
    LEGEND(
        title = "Legend",
        minScoreInclusive = 1000.0,
        maxScoreExclusive = null,
        color = 0xFFFFD54F
    );

    companion object
}

fun BadgeType.matchesScore(score: Double): Boolean {
    val upper = maxScoreExclusive
    return if (upper == null) {
        score >= minScoreInclusive
    } else {
        score >= minScoreInclusive && score < upper
    }
}

fun BadgeType.Companion.fromScore(score: Double): BadgeType {
    return BadgeType.values().firstOrNull { it.matchesScore(score) } ?: BadgeType.ROOKIE
}

fun BadgeType.Companion.fromInputs(inputs: CollectionScoreInputs): BadgeType {
    return fromScore(inputs.totalScore)
}
