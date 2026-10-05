package com.visionfit.domain.model

import kotlin.math.roundToInt

/** Grams of each macronutrient. */
data class Macros(
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
) {
    operator fun plus(other: Macros) = Macros(
        proteinG = proteinG + other.proteinG,
        carbsG = carbsG + other.carbsG,
        fatG = fatG + other.fatG,
    )

    companion object {
        val Zero = Macros(0.0, 0.0, 0.0)
    }
}

/** Daily macro targets in whole grams. Percentages are energy shares and always sum to 100. */
data class MacroTargets(
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
) {
    private val totalKcal: Int get() = proteinG * KCAL_PER_G_PROTEIN + carbsG * KCAL_PER_G_CARBS + fatG * KCAL_PER_G_FAT

    val proteinPercent: Int get() = share(proteinG * KCAL_PER_G_PROTEIN)
    val fatPercent: Int get() = share(fatG * KCAL_PER_G_FAT)
    val carbsPercent: Int get() = if (totalKcal == 0) 0 else 100 - proteinPercent - fatPercent

    private fun share(kcal: Int): Int = if (totalKcal == 0) 0 else (kcal * 100.0 / totalKcal).roundToInt()

    companion object {
        const val KCAL_PER_G_PROTEIN = 4
        const val KCAL_PER_G_CARBS = 4
        const val KCAL_PER_G_FAT = 9
    }
}

data class NutritionTarget(
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val activityMultiplier: Double,
    val adjustmentKcal: Int,
    val dailyKcal: Int,
    val macros: MacroTargets,
    val goal: FitnessGoal,
)
