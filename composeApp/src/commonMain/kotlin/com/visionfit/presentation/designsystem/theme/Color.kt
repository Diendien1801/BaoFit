package com.visionfit.presentation.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * VisionFit "playful" palette: bold flat fills, a single ink color for every outline,
 * text and hard offset shadow, and soft tints for containers.
 */
@Immutable
data class VisionFitColors(
    // Ink & surfaces
    val ink: Color = Color(0xFF1B1530),
    val inkSoft: Color = Color(0xFF2A2340),
    val background: Color = Color(0xFFF4F0FF),
    val surface: Color = Color(0xFFFFFFFF),
    val photoPlaceholder: Color = Color(0xFFE6D8C4),

    // Primary purple
    val primary: Color = Color(0xFF5B3FE0),
    val primaryBlob: Color = Color(0xFF6D52F0),
    val primaryContainer: Color = Color(0xFFE7E0FF),
    val onPrimaryMuted: Color = Color(0xFFE4DCFF),
    val primaryText: Color = Color(0xFF4B2FCF),
    val violet: Color = Color(0xFF7B5CFF),
    val violetTrack: Color = Color(0xFFEEE9FF),

    // Yellow
    val yellow: Color = Color(0xFFFFC53D),
    val yellowContainer: Color = Color(0xFFFFF0C2),
    val yellowPale: Color = Color(0xFFFFE7A3),
    val yellowTrack: Color = Color(0xFFFFF3CC),
    val onYellow: Color = Color(0xFF4A3200),
    val onYellowContainer: Color = Color(0xFF5C3D00),
    val onYellowPale: Color = Color(0xFF4A3A00),
    val yellowText: Color = Color(0xFF6E4A00),

    // Coral
    val coral: Color = Color(0xFFFF5A36),
    val coralContainer: Color = Color(0xFFFFE1D9),
    val onCoralContainer: Color = Color(0xFF8A2410),
    val error: Color = Color(0xFFB4230E),
    val errorStrong: Color = Color(0xFFC42A12),

    // Mint
    val mint: Color = Color(0xFF2DD4A0),
    val mintContainer: Color = Color(0xFFC7F5E3),
    val mintPale: Color = Color(0xFFD8F7EA),
    val onMint: Color = Color(0xFF0B3D2C),
    val mintText: Color = Color(0xFF0B5E43),

    // Sky
    val sky: Color = Color(0xFF3AA8FF),
    val skyContainer: Color = Color(0xFFCDE8FF),
    val skyPale: Color = Color(0xFFD6ECFF),
    val skyTrack: Color = Color(0xFFDDEFFF),
    val skyText: Color = Color(0xFF0B4F86),

    // Pink
    val pink: Color = Color(0xFFFF7AB6),
    val pinkContainer: Color = Color(0xFFFFD6E8),

    // Text & neutral strokes
    val textSecondary: Color = Color(0xFF4A4363),
    val textMuted: Color = Color(0xFF6A6485),
    val placeholder: Color = Color(0xFF7C7696),
    val outlineMuted: Color = Color(0xFF8E87A8),
    val underline: Color = Color(0xFFB3ABD6),

    // Camera (dark surfaces)
    val cameraControl: Color = Color(0xFF2E2650),
    val cameraOutline: Color = Color(0xFF4A3F7A),
    val cameraText: Color = Color(0xFFCFC8EA),
    val cameraTextMuted: Color = Color(0xFFB3ABD6),
) {
    val scrim: Color get() = ink.copy(alpha = 0.45f)

    /** Colors that number the dishes found on a photo, in detection order. */
    val dishMarkers: List<Color> get() = listOf(yellow, mint, pink, sky, violet, coral)
}

internal val LocalVisionFitColors = staticCompositionLocalOf { VisionFitColors() }
