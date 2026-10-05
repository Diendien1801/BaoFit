package com.visionfit.presentation.history

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.core.time.TimeProvider
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.PendingStatus
import com.visionfit.domain.repository.MealRepository
import com.visionfit.domain.repository.NetworkMonitor
import com.visionfit.domain.repository.ProfileRepository
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.domain.usecase.SummarizeDayUseCase
import com.visionfit.presentation.designsystem.components.MainTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

class HistoryViewModel(
    mealRepository: MealRepository,
    profileRepository: ProfileRepository,
    networkMonitor: NetworkMonitor,
    timeProvider: TimeProvider,
    calculateTarget: CalculateNutritionTargetUseCase,
    summarizeDay: SummarizeDayUseCase,
) : MviViewModel<HistoryUiState, HistoryEvent, HistoryEffect>(HistoryUiState()) {

    private val today = timeProvider.today()
    private val quickDates = (0 until QUICK_DAYS).map { today.minus(it, DateTimeUnit.DAY) }
    private val selectedDateFlow = MutableStateFlow(today)

    init {
        updateState { copy(today = this@HistoryViewModel.today, dates = quickDates, selectedDate = this@HistoryViewModel.today) }

        @OptIn(ExperimentalCoroutinesApi::class)
        val day = selectedDateFlow.flatMapLatest { date -> mealRepository.observeDay(date) }
        combine(day, profileRepository.profile.map(calculateTarget::invoke), networkMonitor.isOnline) { log, target, online ->
            Triple(log, summarizeDay(log, target), online)
        }
            .onEach { (log, summary, online) ->
                updateState {
                    copy(
                        isLoading = false,
                        selectedDate = log.date,
                        summary = summary,
                        meals = log.meals,
                        isOffline = !online,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.DateSelected -> select(event.date)
            HistoryEvent.OpenDatePicker -> updateState { copy(isDatePickerVisible = true) }
            HistoryEvent.DismissDatePicker -> updateState { copy(isDatePickerVisible = false) }
            is HistoryEvent.DatePicked -> {
                updateState { copy(isDatePickerVisible = false) }
                select(event.date)
            }
            is HistoryEvent.MealClicked -> (event.meal as? PendingMeal)?.let { meal ->
                sendEffect(
                    if (meal.status == PendingStatus.READY_FOR_REVIEW) {
                        HistoryEffect.NavigateToReview(meal.jobId)
                    } else {
                        HistoryEffect.NavigateToAnalysis(meal.jobId)
                    },
                )
            }
            HistoryEvent.CaptureClicked -> sendEffect(HistoryEffect.NavigateToCamera)
            is HistoryEvent.TabSelected -> when (event.tab) {
                MainTab.TODAY -> sendEffect(HistoryEffect.NavigateToDashboard)
                MainTab.HISTORY -> select(today)
                MainTab.GOAL -> sendEffect(HistoryEffect.NavigateToGoal)
                MainTab.PROFILE -> sendEffect(HistoryEffect.NavigateToProfile)
            }
        }
    }

    private fun select(date: LocalDate) {
        val clamped = if (date > today) today else date
        updateState {
            copy(
                selectedDate = clamped,
                isLoading = clamped != selectedDate,
                // A date picked from the calendar gets its own chip at the end.
                dates = if (clamped in quickDates) quickDates else quickDates + clamped,
            )
        }
        selectedDateFlow.value = clamped
    }

    private companion object {
        const val QUICK_DAYS = 7
    }
}
