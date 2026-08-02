package com.yourorg.systemcore.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SystemColorScheme = darkColorScheme(
    primary = CyanCore,
    onPrimary = NavyVoid,
    secondary = SkyBlue,
    onSecondary = NavyVoid,
    background = NavyBase,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceRaised,
    onSurfaceVariant = TextSecondary,
    error = StatusCritical,
    onError = TextPrimary,
    outline = GlassBorder
)

@Composable
fun SystemCoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SystemColorScheme,
        typography = SystemTypography,
        shapes = SystemShapes,
        content = content
    )
}
