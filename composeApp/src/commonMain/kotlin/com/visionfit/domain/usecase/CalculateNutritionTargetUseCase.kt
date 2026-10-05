package com.visionfit.domain.usecase

import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.MacroTargets
import com.visionfit.domain.model.NutritionTarget
import com.visionfit.domain.model.Sex
import kotlin.math.roundToInt

/**
 * Mifflin–St Jeor BMR × activity multiplier, then the goal adjustment.
 *
 * Macro split: protein 2 g per kg of body weight, fat 25 % of energy, carbs fill the rest.
 */
class CalculateNutritionTargetUseCase {

    operator fun invoke(profile: BodyProfile): NutritionTarget {
        val sexConstant = if (profile.sex == Sex.MALE) 5 else -161
        val bmr = 10.0 * profile.weightKg + 6.25 * profile.heightCm - 5.0 * profile.ageYears + sexConstant
        val tdee = bmr * profile.activityLevel.multiplier
        val adjustment = profile.goal.dailyAdjustmentKcal
        val dailyKcal = (tdee + adjustment).roundToInt()

        val proteinG = (PROTEIN_G_PER_KG * profile.weightKg).roundToInt()
        val fatG = (dailyKcal * FAT_ENERGY_SHARE / MacroTargets.KCAL_PER_G_FAT).toInt()
        val carbsKcal = dailyKcal - proteinG * MacroTargets.KCAL_PER_G_PROTEIN - fatG * MacroTargets.KCAL_PER_G_FAT
        val carbsG = (carbsKcal / MacroTargets.KCAL_PER_G_CARBS.toDouble()).toInt().coerceAtLeast(0)

        return NutritionTarget(
            bmrKcal = bmr.roundToInt(),
            tdeeKcal = tdee.roundToInt(),
            activityMultiplier = profile.activityLevel.multiplier,
            adjustmentKcal = adjustment,
            dailyKcal = dailyKcal,
            macros = MacroTargets(proteinG = proteinG, carbsG = carbsG, fatG = fatG),
            goal = profile.goal,
        )
    }

    private companion object {
        const val PROTEIN_G_PER_KG = 2.0
        const val FAT_ENERGY_SHARE = 0.25
    }
}
