package com.visionfit.domain.usecase

import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.DayStatus
import com.visionfit.domain.model.DaySummary
import com.visionfit.domain.model.MacroProgress
import com.visionfit.domain.model.NutritionTarget

/**
 * Sets what the day's diary says was eaten (meal-service's totals, confirmed meals only) against
 * the user's target. The target still comes from the profile on this device.
 */
class SummarizeDayUseCase {

    operator fun invoke(day: DayLog, target: NutritionTarget): DaySummary {
        val eatenMacros = day.eatenMacros
        return DaySummary(
            date = day.date,
            targetKcal = target.dailyKcal,
            eatenKcal = day.eatenKcal,
            eatenMacros = eatenMacros,
            protein = MacroProgress(eatenMacros.proteinG, target.macros.proteinG),
            carbs = MacroProgress(eatenMacros.carbsG, target.macros.carbsG),
            fat = MacroProgress(eatenMacros.fatG, target.macros.fatG),
            confirmedMealCount = day.confirmedMeals.size,
            pendingMealCount = day.pendingMeals.size,
        )
    }

    fun status(day: DayLog, target: NutritionTarget): DayStatus {
        val summary = invoke(day, target)
        return when {
            !summary.hasData -> DayStatus.NO_DATA
            summary.isOverTarget -> DayStatus.OVER_TARGET
            else -> DayStatus.WITHIN_TARGET
        }
    }
}
