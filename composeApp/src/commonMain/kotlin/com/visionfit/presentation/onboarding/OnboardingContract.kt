package com.visionfit.presentation.onboarding

import com.visionfit.domain.model.ActivityLevel
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.FitnessGoal
import com.visionfit.domain.model.NutritionTarget
import com.visionfit.domain.model.Sex

enum class BodyMetric { AGE, HEIGHT, WEIGHT }

data class OnboardingUiState(
    val profile: BodyProfile = BodyProfile.Default,
    /** Live estimate shown in the footer while the user adjusts the inputs. */
    val target: NutritionTarget? = null,
    /** Opened from the "Hồ sơ" tab rather than right after sign-up. */
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
) {
    fun canDecrease(metric: BodyMetric): Boolean = value(metric) > range(metric).first
    fun canIncrease(metric: BodyMetric): Boolean = value(metric) < range(metric).last

    fun value(metric: BodyMetric): Int = when (metric) {
        BodyMetric.AGE -> profile.ageYears
        BodyMetric.HEIGHT -> profile.heightCm
        BodyMetric.WEIGHT -> profile.weightKg
    }

    companion object {
        fun range(metric: BodyMetric): IntRange = when (metric) {
            BodyMetric.AGE -> BodyProfile.AgeRange
            BodyMetric.HEIGHT -> BodyProfile.HeightRangeCm
            BodyMetric.WEIGHT -> BodyProfile.WeightRangeKg
        }
    }
}

sealed interface OnboardingEvent {
    data class SexSelected(val sex: Sex) : OnboardingEvent
    data class MetricStepped(val metric: BodyMetric, val delta: Int) : OnboardingEvent
    data class ActivitySelected(val level: ActivityLevel) : OnboardingEvent
    data class GoalSelected(val goal: FitnessGoal) : OnboardingEvent
    data object Submit : OnboardingEvent
    data object Back : OnboardingEvent
}

sealed interface OnboardingEffect {
    data object NavigateToGoal : OnboardingEffect
    data object NavigateBack : OnboardingEffect
}
