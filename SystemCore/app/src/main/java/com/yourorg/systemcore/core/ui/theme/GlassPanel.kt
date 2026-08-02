package com.yourorg.systemcore.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies the frosted-glass panel look used across the System Window: a subtle vertical
 * gradient surface, a soft cyan-tinted border, and rounded corners. Real Compose graphics
 * primitives only - no external glass/blur library, since Android's runtime blur
 * (RenderEffect) requires API 31+ and is not universally available down to minSdk 26,
 * so we approximate the glass look with translucency + gradient + border instead of a
 * true backdrop blur.
 */
fun Modifier.glassPanel(cornerRadius: Dp = 20.dp): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                NavySurfaceRaised.copy(alpha = 0.72f),
                NavySurface.copy(alpha = 0.55f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                CyanCore.copy(alpha = 0.35f),
                GlassBorder
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
