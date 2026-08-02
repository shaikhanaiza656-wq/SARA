package com.yourorg.systemcore.core.ui.theme

import androidx.compose.ui.graphics.Color

// Base surfaces - deep navy, layered for depth
val NavyVoid = Color(0xFF05070D)          // deepest background
val NavyBase = Color(0xFF0A0E1A)          // primary window background
val NavySurface = Color(0xFF10162A)       // panel surface
val NavySurfaceRaised = Color(0xFF161D38) // raised / focused panel

// Glass overlay tints (used with alpha for glassmorphism)
val GlassTint = Color(0xFFB8D4FF)
val GlassBorder = Color(0x337FDBFF)

// Cyan / sky-blue accent system
val CyanCore = Color(0xFF39D6FF)      // primary accent - active states
val CyanBright = Color(0xFF7FE8FF)    // highlights, glow
val CyanDim = Color(0xFF1E8FA8)       // secondary / inactive accent
val SkyBlue = Color(0xFF5AC8FA)       // supporting accent

// Status colors (kept in the same cool palette so alerts don't clash)
val StatusOnline = Color(0xFF39D6FF)
val StatusWarning = Color(0xFFFFC857)
val StatusCritical = Color(0xFFFF5C7A)
val StatusOffline = Color(0xFF4A5470)

// Text
val TextPrimary = Color(0xFFE8F4FF)
val TextSecondary = Color(0xFF8FA3C4)
val TextDisabled = Color(0xFF4A5470)
