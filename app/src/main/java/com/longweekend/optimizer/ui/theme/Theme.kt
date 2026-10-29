package com.longweekend.optimizer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = AmberOnSecondaryContainer,
    background = SurfaceBg,
    onBackground = TextOnSurface,
    surface = SurfaceLowest,
    onSurface = TextOnSurface,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = TextOnSurfaceVariant,
    outline = OutlineColor,
    outlineVariant = OutlineVariant,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun LongWeekendTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
