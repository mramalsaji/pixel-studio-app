package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PhotoForgeColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = StudioDarkBg,
    primaryContainer = CyanAccentMuted,
    onPrimaryContainer = TextPrimary,
    secondary = VioletAccent,
    onSecondary = StudioDarkBg,
    secondaryContainer = VioletAccentMuted,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberAccent,
    onTertiary = StudioDarkBg,
    background = StudioDarkBg,
    onBackground = TextPrimary,
    surface = StudioDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioDarkSurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = StudioDarkBg,
    surfaceContainerLow = StudioDarkSurface,
    surfaceContainer = StudioDarkSurfaceCard,
    surfaceContainerHigh = StudioDarkSurfaceElevated,
    surfaceContainerHighest = StudioDarkSurfaceHigh,
    outline = BorderSubtle,
    outlineVariant = BorderMedium,
    error = RedAccent,
    onError = StudioDarkBg
)

@Composable
fun PhotoForgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PhotoForgeColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PhotoForgeTheme(content = content)
}
