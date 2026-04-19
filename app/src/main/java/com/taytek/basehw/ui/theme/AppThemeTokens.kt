package com.taytek.basehw.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppThemeTokens(
    val primaryAccent: Color,
    val cardBorderStandard: Color,
    val cardBorderMuted: Color,
    val selectionOverlay: Color,
    val selectionIconTint: Color,
    val placeholderGradientStart: Color,
    val placeholderGradientEnd: Color
)

data class BrandTokens(
    val sthGold: Color,
    val sthGoldDeep: Color,
    val sthBlueSurface: Color,
    val sthBlueSurfaceAlt: Color,
    val sthTagBackground: Color,
    val sthTagText: Color,
    val sthTagGlow: Color,
    val thGray: Color,
    val thText: Color,
    val chaseBlack: Color,
    val chaseText: Color,
    val chaseBorder: Color
)

private val DefaultAppTokens = AppThemeTokens(
    primaryAccent = Color.Unspecified,
    cardBorderStandard = Color.Unspecified,
    cardBorderMuted = Color.Unspecified,
    selectionOverlay = Color.Unspecified,
    selectionIconTint = Color.Unspecified,
    placeholderGradientStart = Color.Unspecified,
    placeholderGradientEnd = Color.Unspecified
)

private val DefaultBrandTokens = BrandTokens(
    sthGold = Color.Unspecified,
    sthGoldDeep = Color.Unspecified,
    sthBlueSurface = Color.Unspecified,
    sthBlueSurfaceAlt = Color.Unspecified,
    sthTagBackground = Color.Unspecified,
    sthTagText = Color.Unspecified,
    sthTagGlow = Color.Unspecified,
    thGray = Color.Unspecified,
    thText = Color.Unspecified,
    chaseBlack = Color.Unspecified,
    chaseText = Color.Unspecified,
    chaseBorder = Color.Unspecified
)

val LocalAppThemeTokens = staticCompositionLocalOf { DefaultAppTokens }
val LocalBrandTokens = staticCompositionLocalOf { DefaultBrandTokens }

object AppTheme {
    val tokens: AppThemeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppThemeTokens.current

    val brand: BrandTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalBrandTokens.current
}
