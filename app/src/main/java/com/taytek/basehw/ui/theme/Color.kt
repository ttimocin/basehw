package com.taytek.basehw.ui.theme

import androidx.compose.ui.graphics.Color

// === Legacy / still referenced externally ===
@Deprecated(
    message = "Use MaterialTheme.colorScheme or AppTheme.tokens from AppThemeTokens.kt."
)
val AppBackground = Color(0xFFF8F9FA)
@Deprecated(
    message = "Use MaterialTheme.colorScheme.surface."
)
val AppSurface = Color(0xFFFFFFFF)
@Deprecated(
    message = "Use AppTheme.tokens.primaryAccent."
)
val AppPrimary = Color(0xFFFB923C)          // Paler orange (Tailwind Orange 400 style)
@Deprecated(
    message = "Use MaterialTheme.colorScheme.onPrimary."
)
val AppOnPrimary = Color(0xFFFFFFFF)
@Deprecated(
    message = "Use MaterialTheme.colorScheme.onBackground."
)
val AppTextPrimary = Color(0xFF1E293B)
@Deprecated(
    message = "Use MaterialTheme.colorScheme.onSurfaceVariant."
)
val AppTextSecondary = Color(0xFF334155)
@Deprecated(
    message = "Use AppTheme.tokens.primaryAccent variants."
)
val AppAccentLight = Color(0xFFFFE8CC)
@Deprecated(
    message = "Use MaterialTheme color/elevation tokens where needed."
)
val AppShadow = Color(0x1A000000)

// === Dark Theme (Figma-based navy) ===
val DarkNavy        = Color(0xFF0F172A)   // Main background (same as FigmaBackground)
val DarkNavyDeep    = Color(0xFF080C14)   // Nav bar (slightly darker)
val DarkNavyCard    = Color(0xFF1E293B)   // Cards / surfaces (same as FigmaSurface)
val DarkNavyVariant = Color(0xFF263448)   // surfaceVariant, inputs
val DarkOrange      = Color(0xFFF57C00)   // Primary (same as FigmaAccent)
val DarkOrangeAlt   = Color(0xFFFF9A3C)   // Secondary / lighter orange
val DarkText        = Color(0xFFE2E8F0)   // Primary text
val DarkTextSecond  = Color(0xFF9CA3AF)   // Secondary text (same as FigmaSecondaryText)
val DarkGreen       = Color(0xFF10B981)   // Positive / tertiary (same as FigmaPositive)

// === Light Theme ===
val LightBackground   = Color(0xFFF8F9FA)
val LightSurface      = Color(0xFFFFFFFF)
val LightNavBar       = Color(0xFFFFFFFF)
val LightOrange       = Color(0xFFFB923C)   // Softer orange brand color
val LightOrangeOnP    = Color(0xFFFFFFFF)
val LightOrangeContainer  = Color(0xFFFFF7ED)
val LightOnOrangeContainer = Color(0xFF4A1800)
val LightText         = Color(0xFF1E293B)   // Dark navy for readability on white
val LightTextSecond   = Color(0xFF334155)
val LightVariant      = Color(0xFFF1F5F9)
val LightOutline      = Color(0xFFCBD5E1)

// Card Gradients
val LightCardGradientStart = Color(0xFFFFFDFB)
val LightCardGradientEnd   = Color(0xFFFFF7ED)
val DarkCardGradientStart  = Color(0xFF1E293B) // Matched to DarkNavyCard
val DarkCardGradientEnd    = Color(0xFF121416)

// === Cyber / Synthwave (third theme) — indigo → magenta → sunset ===
val CyberGradientTop = Color(0xFF1A0D3A)
val CyberGradientMid = Color(0xFF5C1F5C)
val CyberGradientBottom = Color(0xFFFF8A5C)
val CyberScreenBg = CyberGradientTop
val CyberNavBar = Color(0xFF0C0618)
val CyberCard = Color(0xFF12081F)
val CyberInputSurface = Color(0xFF221538)
val CyberCardGradientStart = Color(0xFF2A1548)
val CyberCardGradientEnd = Color(0xFF160C28)
val CyberNeonPink = Color(0xFFFF4FD8)
val CyberSunsetOrange = Color(0xFFFF8A4C)
val CyberText = Color(0xFFF8F0FF)
val CyberTextMuted = Color(0xFFC9B8E8)
/** Icon “cutout” on neon gradient buttons (very dark, not pure black). */
val CyberKnockoutIconTint = Color(0xFF0E0618)

// === Neon Cyan / Electric Blue (fourth theme) — deep navy shell + cyan accents ===
/** Primary neon accent — slightly toned down vs pure #00E5FF for less eye-searing brightness. */
val NeonElectricCyan = Color(0xFF00D2E8)
val NeonCyanGradientTop = Color(0xFF050A18)
val NeonCyanGradientMid = Color(0xFF0C1828)
val NeonCyanGradientBottom = Color(0xFF142438)
val NeonCyanScreenBg = NeonCyanGradientTop
val NeonCyanNavBar = Color(0xFF060D18)
val NeonCyanCard = Color(0xFF0A1520)
val NeonCyanInputSurface = Color(0xFF121E2E)
val NeonCyanCardGradientStart = Color(0xFF101C2E)
val NeonCyanCardGradientEnd = Color(0xFF0A121C)
val NeonCyanText = Color(0xFFFFFFFF)
val NeonCyanTextMuted = Color(0xFF8A9BA8)
val NeonCyanDeep = Color(0xFF00A3C4)
val NeonCyanKnockoutIconTint = Color(0xFF021018)

// Brand indicator colors
val HotWheelsRed = Color(0xFFE53935)
val MatchboxBlue = Color(0xFF1565C0)
val MiniGTSilver = Color(0xFF90A4AE)
val MajoretteYellow = Color(0xFFFDD835)
val JadaPurple = Color(0xFF7B1FA2)
val SikuBlue = Color(0xFF1976D2)
val KaidoHouseColor = Color(0xFFB71C1C)
val HwPopCultureOrange = Color(0xFFFF6D00)
val HwFastFuriousAmber = Color(0xFFFFC107)
val HwCarCultureGreen = Color(0xFF2E7D32)
val GreenlightGreen = Color(0xFF2E7D32)