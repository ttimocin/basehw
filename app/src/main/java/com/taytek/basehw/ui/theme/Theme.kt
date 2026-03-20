package com.taytek.basehw.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    surfaceContainerLow = DarkNavyDeep,
    surfaceContainerHigh= DarkNavyCard,
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
    surfaceContainerLow = LightNavBar,
    surfaceContainerHigh= LightVariant,
    outline             = LightOutline,
    outlineVariant      = Color(0xFFE2E8F0),
    surfaceTint         = LightOrange,
    scrim               = Color(0xFF000000)
)

@Composable
fun BaseHWTheme(
    themeState: Int = 2, // 0: System, 1: Light, 2: Dark  — default is Dark
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeState) {
        1    -> false
        2    -> true
        else -> isSystemDark
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}