package com.yourorg.systemcore.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Uses the system default font family; a licensed display/monospace font can be
// swapped in later via a bundled font resource - not fabricated here.
val SystemFontFamily = FontFamily.SansSerif
val SystemMonoFamily = FontFamily.Monospace

val SystemTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        letterSpacing = 1.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        letterSpacing = 0.8.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 1.2.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SystemMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp
    )
)
