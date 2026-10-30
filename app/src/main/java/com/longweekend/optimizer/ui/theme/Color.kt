package com.longweekend.optimizer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's palette. Screens use the named colours below (TealPrimary, SurfaceBg, …), which
 * resolve to the light or dark set provided by [LongWeekendTheme].
 */
@Immutable
data class AppColors(
    // Primary brand palette (Teal)
    val tealPrimary: Color,
    val tealPrimaryContainer: Color,
    val tealOnPrimary: Color,
    val tealOnPrimaryContainer: Color,
    val tealPrimaryFixed: Color,
    val tealPrimaryFixedDim: Color,
    val tealOnPrimaryFixed: Color,
    // Secondary accent palette (Amber / Gold)
    val amberSecondary: Color,
    val amberSecondaryContainer: Color,
    val amberOnSecondaryContainer: Color,
    val amberSecondaryFixed: Color,
    val amberOnSecondaryFixed: Color,
    val amberOnSecondary: Color,
    // Surfaces & Backgrounds
    val surfaceBg: Color,
    val surfaceLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    // Text & Outlines
    val textOnSurface: Color,
    val textOnSurfaceVariant: Color,
    val outlineVariant: Color,
    val outline: Color,
    // Error / Alert
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val isDark: Boolean
)

val LightAppColors = AppColors(
    tealPrimary = Color(0xFF003733),
    tealPrimaryContainer = Color(0xFF0B4F4A),
    tealOnPrimary = Color(0xFFFFFFFF),
    tealOnPrimaryContainer = Color(0xFF84BFB8),
    tealPrimaryFixed = Color(0xFFB2EEE7),
    tealPrimaryFixedDim = Color(0xFF96D2CB),
    tealOnPrimaryFixed = Color(0xFF00201D),
    amberSecondary = Color(0xFF855300),
    amberSecondaryContainer = Color(0xFFFEA619),
    amberOnSecondaryContainer = Color(0xFF684000),
    amberSecondaryFixed = Color(0xFFFFDDB8),
    amberOnSecondaryFixed = Color(0xFF2A1700),
    amberOnSecondary = Color(0xFFFFFFFF),
    surfaceBg = Color(0xFFF9F9FF),
    surfaceLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F3FF),
    surfaceContainer = Color(0xFFE7EEFF),
    surfaceContainerHigh = Color(0xFFDEE8FF),
    surfaceContainerHighest = Color(0xFFD8E3FB),
    textOnSurface = Color(0xFF111C2D),
    textOnSurfaceVariant = Color(0xFF3F4947),
    outlineVariant = Color(0xFFBFC8C6),
    outline = Color(0xFF707977),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    isDark = false
)

/** Same roles as [LightAppColors]: "Teal primary" is light teal on dark, amber blocks are toned down. */
val DarkAppColors = AppColors(
    tealPrimary = Color(0xFF96D2CB),
    tealPrimaryContainer = Color(0xFF6FB5AD),
    tealOnPrimary = Color(0xFF00201D),
    tealOnPrimaryContainer = Color(0xFF00201D),
    tealPrimaryFixed = Color(0xFF1D4A46),
    tealPrimaryFixedDim = Color(0xFF2B5F5A),
    tealOnPrimaryFixed = Color(0xFFB2EEE7),
    amberSecondary = Color(0xFFFFB95F),
    amberSecondaryContainer = Color(0xFF6B4500),
    amberOnSecondaryContainer = Color(0xFFFFDDB8),
    amberSecondaryFixed = Color(0xFF4A3000),
    amberOnSecondaryFixed = Color(0xFFFFDDB8),
    amberOnSecondary = Color(0xFF472A00),
    surfaceBg = Color(0xFF0E1420),
    surfaceLowest = Color(0xFF182030),
    surfaceContainerLow = Color(0xFF1C2535),
    surfaceContainer = Color(0xFF232D3F),
    surfaceContainerHigh = Color(0xFF2A3548),
    surfaceContainerHighest = Color(0xFF313D51),
    textOnSurface = Color(0xFFE2E8F4),
    textOnSurfaceVariant = Color(0xFFBEC8CC),
    outlineVariant = Color(0xFF3F4947),
    outline = Color(0xFF89938F),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    isDark = true
)

/**
 * Battery saver for OLED/AMOLED screens, where black pixels are switched off: a pure black
 * page, near-black cards, deep low-luminance fills for holiday/leave blocks and slightly dimmed
 * text (no pure white, which also smears on OLED). Designed in Stitch ("Long Weekend OLED").
 */
val OledAppColors = AppColors(
    tealPrimary = Color(0xFF80CBC4),
    tealPrimaryContainer = Color(0xFF5DA8A0),
    tealOnPrimary = Color(0xFF00201D),
    tealOnPrimaryContainer = Color(0xFF00201D),
    tealPrimaryFixed = Color(0xFF0F2F2C),
    tealPrimaryFixedDim = Color(0xFF173F3B),
    tealOnPrimaryFixed = Color(0xFFA6E4DC),
    amberSecondary = Color(0xFFFFB95F),
    amberSecondaryContainer = Color(0xFF3D2800),
    amberOnSecondaryContainer = Color(0xFFFFD9A8),
    amberSecondaryFixed = Color(0xFF2E1E00),
    amberOnSecondaryFixed = Color(0xFFFFD9A8),
    amberOnSecondary = Color(0xFF472A00),
    surfaceBg = Color(0xFF000000),
    surfaceLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF101010),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF242424),
    textOnSurface = Color(0xFFE6E6E6),
    textOnSurfaceVariant = Color(0xFFA6A6A6),
    outlineVariant = Color(0xFF262626),
    outline = Color(0xFF7A7A7A),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5C0006),
    onErrorContainer = Color(0xFFFFDAD6),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

// Named colours used across the UI; each resolves against the current theme
val TealPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.tealPrimary
val TealPrimaryContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.tealPrimaryContainer
val TealOnPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.tealOnPrimary
val TealOnPrimaryContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.tealOnPrimaryContainer
val TealPrimaryFixed: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.tealPrimaryFixed
val TealPrimaryFixedDim: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.tealPrimaryFixedDim
val TealOnPrimaryFixed: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.tealOnPrimaryFixed

val AmberSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.amberSecondary
val AmberSecondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.amberSecondaryContainer
val AmberOnSecondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.amberOnSecondaryContainer
val AmberSecondaryFixed: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.amberSecondaryFixed
val AmberOnSecondaryFixed: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.amberOnSecondaryFixed
val AmberOnSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.amberOnSecondary

val SurfaceBg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceBg
val SurfaceLowest: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceLowest
val SurfaceContainerLow: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceContainerLow
val SurfaceContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceContainer
val SurfaceContainerHigh: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceContainerHigh
val SurfaceContainerHighest: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceContainerHighest

val TextOnSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textOnSurface
val TextOnSurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textOnSurfaceVariant
val OutlineVariant: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.outlineVariant
val OutlineColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.outline

val ErrorRed: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.error
val ErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.errorContainer
val OnErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onErrorContainer
