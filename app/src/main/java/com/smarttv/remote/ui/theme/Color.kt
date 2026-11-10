package com.smarttv.remote.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF080A10)
val BgDarkElevated = Color(0xFF0E111C)
val GlassSurface = Color(0xFF141724)
val GlassSurfaceSelected = Color(0xFF1A1F36)
val GlassBorder = Color(0xFF22283D)
val GlassBorderSelected = Color(0xFF3D7BFF)

val NeonBlue = Color(0xFF3D7BFF)
val NeonCyan = Color(0xFF00F0FF)
val NeonPurple = Color(0xFF9D4EDD)
val NeonGreen = Color(0xFF00E676)
val PowerRed = Color(0xFFFF3B30)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8E95A5)
val TextTertiary = Color(0xFF5D6578)

val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF3D7BFF), Color(0xFF9D4EDD))
)

val GlowGradient = Brush.verticalGradient(
    colors = listOf(Color(0x333D7BFF), Color(0x00080A10))
)

val CardGlassGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1B1F31), Color(0xFF111422))
)
