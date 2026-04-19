package com.taytek.basehw.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Resolved visual theme (system preference already applied for [Light] / [Dark]). */
enum class ThemeVariant {
    Light,
    Dark,
    /** Magenta / sunset neon (Synthwave). */
    Cyber,
    /** Deep navy + electric cyan (#00E5FF). */
    NeonCyan
}

val LocalThemeVariant = staticCompositionLocalOf { ThemeVariant.Dark }

/** Use instead of comparing [MaterialTheme.colorScheme.background] to [DarkNavy] so Cyber stays on dark UI paths. */
@Composable
@ReadOnlyComposable
fun isDarkThemeUi(): Boolean = LocalThemeVariant.current != ThemeVariant.Light

/** Synthwave Cyber or Neon Cyan — full-screen gradient, transparent roots, neon chrome. */
@Composable
@ReadOnlyComposable
fun isNeonShellTheme(): Boolean = when (LocalThemeVariant.current) {
    ThemeVariant.Cyber, ThemeVariant.NeonCyan -> true
    else -> false
}

/** @deprecated Misleading name: true for both [ThemeVariant.Cyber] and [ThemeVariant.NeonCyan]. Prefer [isNeonShellTheme]. */
@Composable
@ReadOnlyComposable
fun isCyberTheme(): Boolean = isNeonShellTheme()

/**
 * Full-screen root fill: solid [MaterialTheme.colorScheme.background] normally; **transparent** in Cyber
 * so [MainScreen] (or another parent) can paint the vertical neon gradient underneath.
 */
@Composable
@ReadOnlyComposable
fun cyberRootBackgroundColor(): Color =
    if (isNeonShellTheme()) Color.Transparent else MaterialTheme.colorScheme.background

/** Same idea when a screen root uses [MaterialTheme.colorScheme.surface]. */
@Composable
@ReadOnlyComposable
fun cyberRootSurfaceColor(): Color =
    if (isNeonShellTheme()) Color.Transparent else MaterialTheme.colorScheme.surface

/**
 * İkon vurgusu: neon shell temalarında [AppTheme.tokens.primaryAccent] yerine
 * pembe/cyan [AppTheme.tokens.selectionIconTint] (filtre, istatistik pasta ikonu vb.).
 */
@Composable
@ReadOnlyComposable
fun neonShellChromeIconTint(): Color =
    when (LocalThemeVariant.current) {
        ThemeVariant.Cyber, ThemeVariant.NeonCyan -> AppTheme.tokens.selectionIconTint
        else -> AppTheme.tokens.primaryAccent
    }
