package com.taytek.basehw.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NatureColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = OnGreenText,
    primaryContainer = ForestGreenLight,
    onPrimaryContainer = OnGreenText,
    secondary = DarkOlive,
    onSecondary = OnGreenText,
    secondaryContainer = CreamSurfaceVariant,
    onSecondaryContainer = DarkOlive,
    background = CreamBackground,
    onBackground = OnCreamText,
    surface = CreamSurface,
    onSurface = OnCreamText,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = DarkOliveVariant,
    outline = DarkOliveVariant,
    surfaceTint = ForestGreen,
    error = ErrorRed,
    onError = OnGreenText
)

@Composable
fun BaseHWTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Ignoring dark theme to force nature style
    content: @Composable () -> Unit
) {
    // We enforce the NatureColorScheme universally for now to match the strict visual style
    val colorScheme = NatureColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CreamBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}