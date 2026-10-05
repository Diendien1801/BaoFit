package com.visionfit.presentation.designsystem.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

object VisionFitTheme {
    val colors: VisionFitColors
        @Composable @ReadOnlyComposable
        get() = LocalVisionFitColors.current

    val type: VisionFitTypography
        @Composable @ReadOnlyComposable
        get() = LocalVisionFitTypography.current
}

/**
 * Provides the VisionFit tokens and maps them onto Material 3, so stock components
 * (date picker, snackbar, progress indicator) match the brand.
 */
@Composable
fun VisionFitTheme(content: @Composable () -> Unit) {
    val colors = remember { VisionFitColors() }
    val type = rememberVisionFitTypography()
    val colorScheme = remember(colors) {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.surface,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.ink,
            secondary = colors.yellow,
            onSecondary = colors.ink,
            secondaryContainer = colors.yellowContainer,
            onSecondaryContainer = colors.ink,
            tertiary = colors.mint,
            onTertiary = colors.ink,
            background = colors.background,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.background,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainerHigh = colors.surface,
            surfaceContainerHighest = colors.surface,
            inverseSurface = colors.ink,
            inverseOnSurface = colors.surface,
            inversePrimary = colors.yellow,
            outline = colors.ink,
            outlineVariant = colors.underline,
            error = colors.error,
            onError = colors.surface,
            errorContainer = colors.coralContainer,
            onErrorContainer = colors.onCoralContainer,
        )
    }
    val materialTypography = remember(type) {
        Typography(
            displayLarge = type.numberXL,
            headlineLarge = type.headlineL,
            headlineMedium = type.headlineM,
            headlineSmall = type.headlineS,
            titleLarge = type.sectionTitle,
            titleMedium = type.cardTitle,
            titleSmall = type.labelM,
            bodyLarge = type.input,
            bodyMedium = type.body,
            bodySmall = type.caption,
            labelLarge = type.labelL,
            labelMedium = type.label,
            labelSmall = type.chip,
        )
    }
    CompositionLocalProvider(
        LocalVisionFitColors provides colors,
        LocalVisionFitTypography provides type,
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = materialTypography) {
            // Ink is the default color for text and icons everywhere.
            CompositionLocalProvider(LocalContentColor provides colors.ink, content = content)
        }
    }
}
