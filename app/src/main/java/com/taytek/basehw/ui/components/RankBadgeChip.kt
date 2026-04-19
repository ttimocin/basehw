package com.taytek.basehw.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taytek.basehw.domain.model.BadgeType

@Composable
fun RankBadgeChip(
    badge: BadgeType,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val rankColor = Color(badge.color)
    val glowProfile = badgeGlowProfile(badge)
    val shape = RoundedCornerShape(if (compact) 8.dp else 10.dp)
    val infiniteTransition = rememberInfiniteTransition(label = "rankGlow")
    val glowStrength = infiniteTransition.animateFloat(
        initialValue = glowProfile.minAlpha,
        targetValue = glowProfile.maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = glowProfile.durationMs),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rankGlowStrength"
    )
    val backgroundBrush = Brush.horizontalGradient(
        colors = listOf(
            rankColor.copy(alpha = 0.22f),
            rankColor.copy(alpha = 0.10f)
        )
    )

    Row(
        modifier = modifier
            .softGlow(
                color = rankColor,
                alpha = glowStrength.value,
                radius = if (compact) 5.dp + glowProfile.radiusBoost else 7.dp + glowProfile.radiusBoost
            )
            .background(backgroundBrush, shape)
            .border(1.dp, rankColor.copy(alpha = 0.55f), shape)
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "◆",
            color = rankColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black
        )
        Text(
            text = badge.title.uppercase(),
            color = rankColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp
        )
    }
}

private data class GlowProfile(
    val minAlpha: Float,
    val maxAlpha: Float,
    val durationMs: Int,
    val radiusBoost: Dp
)

private fun badgeGlowProfile(badge: BadgeType): GlowProfile {
    return when (badge.ordinal) {
        in 0..4 -> GlowProfile(
            minAlpha = 0.14f,
            maxAlpha = 0.24f,
            durationMs = 2100,
            radiusBoost = 0.dp
        )
        in 5..9 -> GlowProfile(
            minAlpha = 0.18f,
            maxAlpha = 0.32f,
            durationMs = 1950,
            radiusBoost = 0.5.dp
        )
        in 10..14 -> GlowProfile(
            minAlpha = 0.22f,
            maxAlpha = 0.42f,
            durationMs = 1750,
            radiusBoost = 1.dp
        )
        in 15..17 -> GlowProfile(
            minAlpha = 0.28f,
            maxAlpha = 0.54f,
            durationMs = 1600,
            radiusBoost = 1.5.dp
        )
        else -> GlowProfile(
            minAlpha = 0.34f,
            maxAlpha = 0.70f,
            durationMs = 1450,
            radiusBoost = 2.dp
        )
    }
}

private fun Modifier.softGlow(
    color: Color,
    alpha: Float,
    radius: Dp
): Modifier = drawBehind {
    val inset = radius.toPx()
    val topLeft = androidx.compose.ui.geometry.Offset(-inset * 0.25f, -inset * 0.2f)
    val size = androidx.compose.ui.geometry.Size(
        width = this.size.width + inset * 0.5f,
        height = this.size.height + inset * 0.4f
    )
    drawRoundRect(
        color = color.copy(alpha = alpha.coerceIn(0f, 1f) * 0.35f),
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(x = inset, y = inset)
    )
}
