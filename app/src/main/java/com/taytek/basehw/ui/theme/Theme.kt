package com.taytek.basehw.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    /** Neon / synthwave palette (always dark UI). */
    CYBER,
    /** Deep navy + electric cyan (always dark UI). */
    NEON_CYAN;

    companion object {
        fun fromPreference(themeState: Int): AppThemeMode = when (themeState) {
            1 -> LIGHT
            2 -> DARK
            3 -> CYBER
            4 -> NEON_CYAN
            else -> SYSTEM
        }
    }
}

// ── Dark colour scheme ────────────────────────────────────────────────────────
// Background = 0xFF0F172A  (matches FigmaBackground / HomeScreen)
// Surface     = 0xFF1E293B (matches FigmaSurface / HomeScreen cards)
// Primary     = 0xFFF57C00 (matches FigmaAccent / orange)
private val DarkColorScheme = darkColorScheme(
    primary             = DarkOrange,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFF4A2000),
    onPrimaryContainer  = Color(0xFFFFCC99),
    secondary           = DarkOrangeAlt,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFF3A1800),
    onSecondaryContainer= Color(0xFFFFCC99),
    tertiary            = DarkGreen,
    onTertiary          = Color.White,
    background          = DarkNavy,
    onBackground        = DarkText,
    surface             = DarkNavyCard,
    onSurface           = DarkText,
    surfaceVariant      = DarkNavyVariant,
    onSurfaceVariant    = DarkTextSecond,
    surfaceContainer    = DarkNavyDeep,      // used for nav bar
    surfaceContainerLow = DarkCardGradientStart,
    surfaceContainerHigh= DarkCardGradientEnd,
    outline             = DarkTextSecond.copy(alpha = 0.3f),
    outlineVariant      = DarkTextSecond.copy(alpha = 0.15f),
    surfaceTint         = DarkOrange,
    scrim               = Color(0xFF000000)
)

// ── Light colour scheme ───────────────────────────────────────────────────────
// Clean white + orange accent; text uses dark navy for contrast
private val LightColorScheme = lightColorScheme(
    primary             = LightOrange,
    onPrimary           = LightOrangeOnP,
    primaryContainer    = LightOrangeContainer,
    onPrimaryContainer  = LightOnOrangeContainer,
    secondary           = Color(0xFFE65100),
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFFFECB3),
    onSecondaryContainer= Color(0xFF3E2000),
    tertiary            = Color(0xFF0D9488),
    onTertiary          = Color.White,
    background          = LightBackground,
    onBackground        = LightText,
    surface             = LightSurface,
    onSurface           = LightText,
    surfaceVariant      = LightVariant,
    onSurfaceVariant    = LightTextSecond,
    surfaceContainer    = LightNavBar,
    surfaceContainerLow = LightCardGradientStart,
    surfaceContainerHigh= LightCardGradientEnd,
    outline             = LightOrange.copy(alpha = 0.6f),
    outlineVariant      = Color(0xFFE2E8F0),
    surfaceTint         = LightOrange,
    scrim               = Color(0xFF000000)
)

private val DarkAppTokens = AppThemeTokens(
    primaryAccent = DarkOrange,
    cardBorderStandard = LightOrange.copy(alpha = 0.6f),
    cardBorderMuted = LightOrange.copy(alpha = 0.24f),
    selectionOverlay = DarkOrange.copy(alpha = 0.45f),
    selectionIconTint = DarkOrange,
    placeholderGradientStart = DarkOrange.copy(alpha = 0.15f),
    placeholderGradientEnd = DarkOrange.copy(alpha = 0.05f)
)

private val LightAppTokens = AppThemeTokens(
    primaryAccent = LightOrange,
    cardBorderStandard = LightOrange.copy(alpha = 0.6f),
    cardBorderMuted = LightOrange.copy(alpha = 0.24f),
    selectionOverlay = LightOrange.copy(alpha = 0.45f),
    selectionIconTint = LightOrange,
    placeholderGradientStart = LightOrange.copy(alpha = 0.15f),
    placeholderGradientEnd = LightOrange.copy(alpha = 0.05f)
)

private val CyberColorScheme = darkColorScheme(
    primary = CyberNeonPink,
    onPrimary = Color(0xFF1A0514),
    primaryContainer = Color(0xFF5C1045),
    onPrimaryContainer = Color(0xFFFFC2EA),
    secondary = CyberSunsetOrange,
    onSecondary = Color(0xFF1A0A00),
    secondaryContainer = Color(0xFF5C2A00),
    onSecondaryContainer = Color(0xFFFFE0B2),
    tertiary = Color(0xFF00E5FF),
    onTertiary = Color(0xFF001A1E),
    background = CyberScreenBg,
    onBackground = CyberText,
    surface = CyberCard,
    onSurface = CyberText,
    surfaceVariant = CyberInputSurface,
    onSurfaceVariant = CyberTextMuted,
    surfaceContainer = CyberNavBar,
    surfaceContainerLow = CyberCardGradientStart,
    surfaceContainerHigh = CyberCardGradientEnd,
    outline = CyberNeonPink.copy(alpha = 0.45f),
    outlineVariant = Color(0xFF3D2A55),
    surfaceTint = CyberNeonPink,
    scrim = Color(0xFF050010)
)

private val CyberAppTokens = AppThemeTokens(
    primaryAccent = CyberSunsetOrange,
    cardBorderStandard = CyberNeonPink.copy(alpha = 0.9f),
    cardBorderMuted = CyberNeonPink.copy(alpha = 0.38f),
    selectionOverlay = CyberNeonPink.copy(alpha = 0.35f),
    selectionIconTint = CyberNeonPink,
    placeholderGradientStart = CyberNeonPink.copy(alpha = 0.22f),
    placeholderGradientEnd = CyberSunsetOrange.copy(alpha = 0.1f)
)

private val NeonCyanColorScheme = darkColorScheme(
    primary = NeonElectricCyan,
    onPrimary = NeonCyanKnockoutIconTint,
    primaryContainer = Color(0xFF003D48),
    onPrimaryContainer = Color(0xFFB8F9FF),
    secondary = NeonCyanDeep,
    onSecondary = Color(0xFF001018),
    secondaryContainer = Color(0xFF004A5C),
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFF90CAF9),
    onTertiary = Color(0xFF0D1B2A),
    background = NeonCyanScreenBg,
    onBackground = NeonCyanText,
    surface = NeonCyanCard,
    onSurface = NeonCyanText,
    surfaceVariant = NeonCyanInputSurface,
    onSurfaceVariant = NeonCyanTextMuted,
    surfaceContainer = NeonCyanNavBar,
    surfaceContainerLow = NeonCyanCardGradientStart,
    surfaceContainerHigh = NeonCyanCardGradientEnd,
    outline = NeonElectricCyan.copy(alpha = 0.5f),
    outlineVariant = Color(0xFF2A3F55),
    surfaceTint = NeonElectricCyan,
    scrim = Color(0xFF000510)
)

private val NeonCyanAppTokens = AppThemeTokens(
    primaryAccent = NeonElectricCyan,
    cardBorderStandard = NeonElectricCyan.copy(alpha = 0.85f),
    cardBorderMuted = NeonElectricCyan.copy(alpha = 0.35f),
    selectionOverlay = NeonElectricCyan.copy(alpha = 0.28f),
    selectionIconTint = NeonElectricCyan,
    placeholderGradientStart = NeonElectricCyan.copy(alpha = 0.2f),
    placeholderGradientEnd = NeonCyanDeep.copy(alpha = 0.12f)
)

// Brand/rarity visuals are intentionally fixed and do not follow selected app theme.
private val FixedBrandTokens = BrandTokens(
    sthGold = Color(0xFFE0B94C),
    sthGoldDeep = Color(0xFFB68A2E),
    sthBlueSurface = Color(0xFF162947),
    sthBlueSurfaceAlt = Color(0xFF0F1F38),
    sthTagBackground = Color(0xFF1A1300),
    sthTagText = Color(0xFFFFD54F),
    sthTagGlow = Color(0xFFFFD700),
    thGray = Color(0xFFE5E4E2),
    thText = Color.DarkGray,
    chaseBlack = Color.Black,
    chaseText = Color.White,
    chaseBorder = Color.White.copy(alpha = 0.5f)
)

@Composable
fun BaseHWTheme(
    themeState: Int = 2, // 0: System, 1: Light, 2: Dark, 3: Cyber, 4: Neon Cyan — default Dark
    fontFamilyState: Int = 0, // 0: Space Grotesk, 1: Inter
    content: @Composable () -> Unit
) {
    val themeMode = AppThemeMode.fromPreference(themeState)
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.CYBER, AppThemeMode.NEON_CYAN -> true
        AppThemeMode.SYSTEM -> isSystemDark
    }
    val themeVariant: ThemeVariant = when (themeMode) {
        AppThemeMode.LIGHT -> ThemeVariant.Light
        AppThemeMode.CYBER -> ThemeVariant.Cyber
        AppThemeMode.NEON_CYAN -> ThemeVariant.NeonCyan
        AppThemeMode.DARK -> ThemeVariant.Dark
        AppThemeMode.SYSTEM -> if (isSystemDark) ThemeVariant.Dark else ThemeVariant.Light
    }
    val colorScheme = when (themeMode) {
        AppThemeMode.CYBER -> CyberColorScheme
        AppThemeMode.NEON_CYAN -> NeonCyanColorScheme
        else -> if (isDark) DarkColorScheme else LightColorScheme
    }
    val appTokens = when (themeMode) {
        AppThemeMode.CYBER -> CyberAppTokens
        AppThemeMode.NEON_CYAN -> NeonCyanAppTokens
        else -> if (isDark) DarkAppTokens else LightAppTokens
    }
    val typography = if (fontFamilyState == 1) InterTypography else SpaceGroteskTypography

    CompositionLocalProvider(
        LocalAppThemeTokens provides appTokens,
        LocalBrandTokens provides FixedBrandTokens,
        LocalThemeVariant provides themeVariant
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
