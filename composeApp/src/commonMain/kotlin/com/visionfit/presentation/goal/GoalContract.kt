package com.visionfit.presentation.goal

import com.visionfit.domain.model.NutritionTarget

data class GoalUiState(
    val target: NutritionTarget? = null,
) {
    val isLoading: Boolean get() = target == null
}

sealed interface GoalEvent {
    data object StartTracking : GoalEvent
    data object EditProfile : GoalEvent
    data object Back : GoalEvent
}

sealed interface GoalEffect {
    data object NavigateToDashboard : GoalEffect
    data object NavigateToProfile : GoalEffect
    data object NavigateBack : GoalEffect
}
