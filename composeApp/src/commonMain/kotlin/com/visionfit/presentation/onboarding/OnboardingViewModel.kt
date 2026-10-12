package com.visionfit.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.ProfileStatus
import com.visionfit.domain.repository.AuthRepository
import com.visionfit.domain.repository.ProfileRepository
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val calculateTarget: CalculateNutritionTargetUseCase,
    isEditing: Boolean,
) : MviViewModel<OnboardingUiState, OnboardingEvent, OnboardingEffect>(
    initialState = profileRepository.profile.value.let { saved ->
        OnboardingUiState(
            profile = saved,
            target = calculateTarget(saved),
            isEditing = isEditing,
            loadStatus = if (isEditing) profileRepository.status.value else ProfileStatus.READY,
        )
    },
) {

    init {
        if (isEditing) {
            profileRepository.status
                .onEach { status ->
                    // The form was hidden until now, so nothing typed is lost by showing the saved profile.
                    if (status == ProfileStatus.READY && !currentState.isFormVisible) {
                        val saved = profileRepository.profile.value
                        updateState { copy(profile = saved, target = calculateTarget(saved), loadStatus = status) }
                    } else {
                        updateState { copy(loadStatus = status) }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.SexSelected -> edit { copy(sex = event.sex) }
            is OnboardingEvent.MetricStepped -> edit { step(event.metric, event.delta) }
            is OnboardingEvent.ActivitySelected -> edit { copy(activityLevel = event.level) }
            is OnboardingEvent.GoalSelected -> edit { copy(goal = event.goal) }
            OnboardingEvent.Submit -> submit()
            OnboardingEvent.Back -> sendEffect(OnboardingEffect.NavigateBack)
            OnboardingEvent.RetryLoadClicked -> viewModelScope.launch { profileRepository.refresh() }
            OnboardingEvent.LogoutClicked -> viewModelScope.launch {
                authRepository.logout()
                sendEffect(OnboardingEffect.NavigateToAuth)
            }
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
        if (currentState.isSaving || !currentState.isFormVisible) return
        updateState { copy(isSaving = true) }
        viewModelScope.launch {
            val saved = profileRepository.saveProfile(currentState.profile)
            updateState { copy(isSaving = false) }
            sendEffect(if (saved) OnboardingEffect.NavigateToGoal else OnboardingEffect.SaveFailed)
        }
    }
}
