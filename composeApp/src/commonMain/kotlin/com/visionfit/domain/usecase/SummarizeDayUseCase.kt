package com.visionfit.domain.usecase

import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.DayStatus
import com.visionfit.domain.model.DaySummary
import com.visionfit.domain.model.MacroProgress
import com.visionfit.domain.model.Macros
import com.visionfit.domain.model.NutritionTarget

/** Totals only confirmed meals: dishes still being analyzed have no trusted numbers yet. */
class SummarizeDayUseCase {

    operator fun invoke(day: DayLog, target: NutritionTarget): DaySummary {
        val confirmed = day.confirmedMeals
        val eatenMacros = confirmed.fold(Macros.Zero) { total, meal -> total + meal.macros }
        return DaySummary(
            date = day.date,
            targetKcal = target.dailyKcal,
            eatenKcal = confirmed.sumOf { it.kcal },
            eatenMacros = eatenMacros,
            protein = MacroProgress(eatenMacros.proteinG, target.macros.proteinG),
            carbs = MacroProgress(eatenMacros.carbsG, target.macros.carbsG),
            fat = MacroProgress(eatenMacros.fatG, target.macros.fatG),
            confirmedMealCount = confirmed.size,
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
