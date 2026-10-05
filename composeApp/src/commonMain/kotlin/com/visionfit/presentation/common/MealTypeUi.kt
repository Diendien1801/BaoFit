package com.visionfit.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.visionfit.domain.model.MealType
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

val MealType.label: String
    get() = when (this) {
        MealType.BREAKFAST -> "Bữa sáng"
        MealType.LUNCH -> "Bữa trưa"
        MealType.SNACK -> "Bữa xế"
        MealType.DINNER -> "Bữa tối"
    }

/** Per-meal accents used by the diary timeline (time badge, washi tape, label). */
data class MealTypeColors(val badge: Color, val tape: Color, val text: Color)

@Composable
@ReadOnlyComposable
fun MealType.colors(): MealTypeColors {
    val c = VisionFitTheme.colors
    return when (this) {
        MealType.BREAKFAST -> MealTypeColors(badge = c.yellow, tape = c.mint.copy(alpha = 0.75f), text = c.yellowText)
        MealType.LUNCH -> MealTypeColors(badge = c.mint, tape = c.yellow.copy(alpha = 0.8f), text = c.mintText)
        MealType.SNACK -> MealTypeColors(badge = c.sky, tape = c.sky.copy(alpha = 0.75f), text = c.skyText)
        MealType.DINNER -> MealTypeColors(badge = c.coral, tape = c.pink.copy(alpha = 0.8f), text = c.onCoralContainer)
    }
}
