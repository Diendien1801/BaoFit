package com.visionfit.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.repository.ProfileRepository
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val profileRepository: ProfileRepository,
    private val calculateTarget: CalculateNutritionTargetUseCase,
    isEditing: Boolean,
) : MviViewModel<OnboardingUiState, OnboardingEvent, OnboardingEffect>(
    initialState = profileRepository.profile.value.let { saved ->
        OnboardingUiState(profile = saved, target = calculateTarget(saved), isEditing = isEditing)
    },
) {

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.SexSelected -> edit { copy(sex = event.sex) }
            is OnboardingEvent.MetricStepped -> edit { step(event.metric, event.delta) }
            is OnboardingEvent.ActivitySelected -> edit { copy(activityLevel = event.level) }
            is OnboardingEvent.GoalSelected -> edit { copy(goal = event.goal) }
            OnboardingEvent.Submit -> submit()
            OnboardingEvent.Back -> sendEffect(OnboardingEffect.NavigateBack)
        }
    }

    private fun edit(transform: BodyProfile.() -> BodyProfile) {
        updateState {
            val profile = profile.transform()
            copy(profile = profile, target = calculateTarget(profile))
        }
    }

    private fun BodyProfile.step(metric: BodyMetric, delta: Int): BodyProfile {
        val value = (currentState.value(metric) + delta).coerceIn(OnboardingUiState.range(metric))
        return when (metric) {
            BodyMetric.AGE -> copy(ageYears = value)
            BodyMetric.HEIGHT -> copy(heightCm = value)
            BodyMetric.WEIGHT -> copy(weightKg = value)
        }
    }

    private fun submit() {
        if (currentState.isSaving) return
        updateState { copy(isSaving = true) }
        viewModelScope.launch {
            profileRepository.saveProfile(currentState.profile)
            updateState { copy(isSaving = false) }
            sendEffect(OnboardingEffect.NavigateToGoal)
        }
    }
}
