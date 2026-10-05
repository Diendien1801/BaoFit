package com.visionfit.presentation.goal

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.domain.repository.ProfileRepository
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class GoalViewModel(
    profileRepository: ProfileRepository,
    calculateTarget: CalculateNutritionTargetUseCase,
) : MviViewModel<GoalUiState, GoalEvent, GoalEffect>(GoalUiState()) {

    init {
        profileRepository.profile
            .map(calculateTarget::invoke)
            .onEach { target -> updateState { copy(target = target) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: GoalEvent) {
        when (event) {
            GoalEvent.StartTracking -> sendEffect(GoalEffect.NavigateToDashboard)
            GoalEvent.EditProfile -> sendEffect(GoalEffect.NavigateToProfile)
            GoalEvent.Back -> sendEffect(GoalEffect.NavigateBack)
        }
    }
}
