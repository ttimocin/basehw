package com.taytek.basehw.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Top-left → bottom-right neon fill for small action buttons. */
fun cyberActionGradientBrush(): Brush = Brush.linearGradient(
    colors = listOf(CyberNeonPink, CyberSunsetOrange),
    start = Offset(0f, 0f),
    end = Offset(140f, 140f)
)

fun neonCyanActionGradientBrush(): Brush = Brush.linearGradient(
    colors = listOf(NeonElectricCyan, NeonCyanDeep),
    start = Offset(0f, 0f),
    end = Offset(140f, 140f)
)

@Composable
fun CyberNeonSquareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    corner: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = CyberNeonPink.copy(alpha = 0.42f),
                ambientColor = CyberSunsetOrange.copy(alpha = 0.22f)
            )
            .clip(shape)
            .background(cyberActionGradientBrush(), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 22.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun NeonCyanNeonSquareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    corner: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = NeonElectricCyan.copy(alpha = 0.45f),
                ambientColor = NeonCyanDeep.copy(alpha = 0.25f)
            )
            .clip(shape)
            .background(neonCyanActionGradientBrush(), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 22.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun NeonCyanNeonIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    iconTint: Color = NeonCyanKnockoutIconTint
) {
    NeonCyanNeonSquareButton(onClick = onClick, modifier = modifier, size = size) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = iconTint
        )
    }
}

@Composable
fun CyberNeonIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    /** Default: dark “cutout”; use e.g. [androidx.compose.material3.MaterialTheme.colorScheme.error] for logout. */
    iconTint: Color = CyberKnockoutIconTint
) {
    CyberNeonSquareButton(onClick = onClick, modifier = modifier, size = size) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = iconTint
        )
    }
}
