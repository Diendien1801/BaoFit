package com.visionfit.domain.model

import kotlinx.datetime.LocalDate
import kotlin.math.max

data class DayLog(
    val date: LocalDate,
    val meals: List<MealEntry>,
) {
    val confirmedMeals: List<ConfirmedMeal> get() = meals.filterIsInstance<ConfirmedMeal>()
    val pendingMeals: List<PendingMeal> get() = meals.filterIsInstance<PendingMeal>()
}

data class MacroProgress(
    val eatenG: Double,
    val targetG: Int,
) {
    /** Fraction of the target, not capped (1.125 = 112.5 %). */
    val fraction: Double get() = if (targetG <= 0) 0.0 else eatenG / targetG
    val isOver: Boolean get() = eatenG > targetG
    val overByG: Double get() = max(0.0, eatenG - targetG)
}

data class DaySummary(
    val date: LocalDate,
    val targetKcal: Int,
    val eatenKcal: Int,
    val eatenMacros: Macros,
    val protein: MacroProgress,
    val carbs: MacroProgress,
    val fat: MacroProgress,
    val confirmedMealCount: Int,
    val pendingMealCount: Int,
) {
    /** Negative when the day went over target. */
    val remainingKcal: Int get() = targetKcal - eatenKcal
    val isOverTarget: Boolean get() = eatenKcal > targetKcal
    val fraction: Double get() = if (targetKcal <= 0) 0.0 else eatenKcal.toDouble() / targetKcal
    val hasData: Boolean get() = confirmedMealCount > 0
}

enum class DayStatus { WITHIN_TARGET, OVER_TARGET, NO_DATA }
