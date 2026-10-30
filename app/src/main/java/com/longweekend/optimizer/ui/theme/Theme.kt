package com.longweekend.optimizer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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

@Composable
fun LongWeekendTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
