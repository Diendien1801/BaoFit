package com.visionfit.presentation.history

import com.visionfit.domain.model.DaySummary
import com.visionfit.domain.model.MealEntry
import com.visionfit.presentation.designsystem.components.MainTab
import kotlinx.datetime.LocalDate

data class HistoryUiState(
    val isLoading: Boolean = true,
    val today: LocalDate? = null,
    /** Quick-pick chips: today first, then the previous days (plus a date picked from the calendar). */
    val dates: List<LocalDate> = emptyList(),
    val selectedDate: LocalDate? = null,
    val summary: DaySummary? = null,
    /** Newest first. */
    val meals: List<MealEntry> = emptyList(),
    val isOffline: Boolean = false,
    val isDatePickerVisible: Boolean = false,
) {
    val isToday: Boolean get() = selectedDate != null && selectedDate == today
}

sealed interface HistoryEvent {
    data class DateSelected(val date: LocalDate) : HistoryEvent
    data object OpenDatePicker : HistoryEvent
    data object DismissDatePicker : HistoryEvent
    data class DatePicked(val date: LocalDate) : HistoryEvent
    data class MealClicked(val meal: MealEntry) : HistoryEvent
    data object CaptureClicked : HistoryEvent
    data class TabSelected(val tab: MainTab) : HistoryEvent
}

sealed interface HistoryEffect {
    data class NavigateToAnalysis(val jobId: String) : HistoryEffect
    data class NavigateToReview(val jobId: String) : HistoryEffect
    data object NavigateToCamera : HistoryEffect
    data object NavigateToDashboard : HistoryEffect
    data object NavigateToGoal : HistoryEffect
    data object NavigateToProfile : HistoryEffect
}
