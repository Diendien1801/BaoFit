package com.visionfit.domain.model

enum class Sex { MALE, FEMALE }

/** Activity multipliers used to turn BMR into TDEE. */
enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
}

enum class FitnessGoal(val dailyAdjustmentKcal: Int) {
    FAT_LOSS(-500),
    MUSCLE_GAIN(300),
}

data class BodyProfile(
    val sex: Sex,
    val ageYears: Int,
    val heightCm: Int,
    val weightKg: Int,
    val activityLevel: ActivityLevel,
    val goal: FitnessGoal,
) {
    init {
        require(ageYears in AgeRange) { "ageYears must be in $AgeRange" }
        require(heightCm in HeightRangeCm) { "heightCm must be in $HeightRangeCm" }
        require(weightKg in WeightRangeKg) { "weightKg must be in $WeightRangeKg" }
    }

    companion object {
        val AgeRange = 14..90
        val HeightRangeCm = 120..220
        val WeightRangeKg = 30..200

        val Default = BodyProfile(
            sex = Sex.MALE,
            ageYears = 25,
            heightCm = 172,
            weightKg = 68,
            activityLevel = ActivityLevel.MODERATE,
            goal = FitnessGoal.FAT_LOSS,
        )
    }
}
