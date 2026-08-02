package com.termuxai.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Sara is designed dark-only for now (always-on assistant dashboard, worn on
 * screen for long stretches) — no dynamic/light color scheme branching yet.
 * Revisit if a Settings screen adds a theme toggle.
 */
private val SaraColorScheme = darkColorScheme(
    primary = SaraCyan,
    onPrimary = BackgroundDark,
    secondary = SaraCyanDim,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceVariantDark,
    error = StatusError
)

@Composable
fun TermuxAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SaraColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
