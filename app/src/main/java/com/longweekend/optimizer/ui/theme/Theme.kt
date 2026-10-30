package com.longweekend.optimizer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** Material components (dialogs, text fields, switches…) take the same colours as our own UI. */
private fun AppColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = tealPrimary,
        onPrimary = tealOnPrimary,
        primaryContainer = tealPrimaryContainer,
        onPrimaryContainer = tealOnPrimaryContainer,
        secondary = amberSecondary,
        onSecondary = amberOnSecondary,
        secondaryContainer = amberSecondaryContainer,
        onSecondaryContainer = amberOnSecondaryContainer,
        background = surfaceBg,
        onBackground = textOnSurface,
        surface = surfaceLowest,
        onSurface = textOnSurface,
        surfaceVariant = surfaceContainerHighest,
        onSurfaceVariant = textOnSurfaceVariant,
        surfaceContainerLowest = surfaceLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        outline = outline,
        outlineVariant = outlineVariant,
        error = error,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer
    )
}

private val LightColorScheme = LightAppColors.toColorScheme()
private val DarkColorScheme = DarkAppColors.toColorScheme()
// Material lightens elevated surfaces (cards, bars) with a tint; on OLED that lights pixels, so it is off
private val OledColorScheme = OledAppColors.toColorScheme().copy(surfaceTint = Color.Transparent)

/** [oled] picks the pure-black palette; it only applies while the app is dark. */
@Composable
fun LongWeekendTheme(
    darkTheme: Boolean,
    oled: Boolean = false,
    content: @Composable () -> Unit
) {
    val (colors, scheme) = when {
        darkTheme && oled -> OledAppColors to OledColorScheme
        darkTheme -> DarkAppColors to DarkColorScheme
        else -> LightAppColors to LightColorScheme
    }
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content
        )
    }
}
