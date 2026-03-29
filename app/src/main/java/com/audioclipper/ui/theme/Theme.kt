package com.audioclipper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PurpleAccent,
    onPrimary = OnSurfaceDark,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = PurpleAccentLight,
    secondary = PurpleAccentLight,
    onSecondary = Charcoal900,
    secondaryContainer = PurpleContainer,
    onSecondaryContainer = PurpleAccentLight,
    tertiary = PurpleAccentLight,
    onTertiary = Charcoal900,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    outline = Charcoal400,
    outlineVariant = Charcoal600,
)

@Composable
fun AudioClipperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
