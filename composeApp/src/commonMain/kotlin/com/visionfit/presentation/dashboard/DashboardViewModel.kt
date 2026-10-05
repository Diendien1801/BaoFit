package com.visionfit.presentation.dashboard

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.core.time.TimeProvider
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.NutritionTarget
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.UserSession
import com.visionfit.domain.repository.AuthRepository
import com.visionfit.domain.repository.MealRepository
import com.visionfit.domain.repository.NetworkMonitor
import com.visionfit.domain.repository.ProfileRepository
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.domain.usecase.CalculateStreakUseCase
import com.visionfit.domain.usecase.SummarizeDayUseCase
import com.visionfit.presentation.designsystem.components.MainTab
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

class DashboardViewModel(
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    mealRepository: MealRepository,
    networkMonitor: NetworkMonitor,
    private val timeProvider: TimeProvider,
    private val calculateTarget: CalculateNutritionTargetUseCase,
    private val summarizeDay: SummarizeDayUseCase,
    private val calculateStreak: CalculateStreakUseCase,
) : MviViewModel<DashboardUiState, DashboardEvent, DashboardEffect>(DashboardUiState()) {

    init {
        val now = timeProvider.now()
        val today = now.date
        val history = mealRepository.observeRange(from = today.minus(STREAK_LOOKBACK_DAYS, DateTimeUnit.DAY), to = today)
        val connectivity = combine(networkMonitor.isOnline, networkMonitor.lastSyncedAt) { online, synced -> online to synced }

        combine(
            authRepository.session,
            profileRepository.profile.map(calculateTarget::invoke),
            history,
            connectivity,
        ) { session, target, days, (online, syncedAt) ->
            buildState(now, session, target, days, online, syncedAt)
        }
            .onEach { state -> updateState { state } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.NotificationsClicked,
            DashboardEvent.SeeAllMealsClicked,
            -> sendEffect(DashboardEffect.NavigateToHistory)
            DashboardEvent.CaptureClicked -> sendEffect(DashboardEffect.NavigateToCamera)
            is DashboardEvent.MealClicked -> when (val meal = event.meal) {
                is PendingMeal -> sendEffect(meal.destination())
                is ConfirmedMeal -> sendEffect(DashboardEffect.NavigateToHistory)
            }
            is DashboardEvent.TabSelected -> when (event.tab) {
                MainTab.TODAY -> Unit
                MainTab.HISTORY -> sendEffect(DashboardEffect.NavigateToHistory)
                MainTab.GOAL -> sendEffect(DashboardEffect.NavigateToGoal)
                MainTab.PROFILE -> sendEffect(DashboardEffect.NavigateToProfile)
            }
        }
    }

    private fun buildState(
        now: LocalDateTime,
        session: UserSession?,
        target: NutritionTarget,
        days: List<DayLog>,
        isOnline: Boolean,
        lastSyncedAt: LocalDateTime?,
    ): DashboardUiState {
        val today = now.date
        val todayLog = days.lastOrNull { it.date == today } ?: DayLog(today, emptyList())
        return DashboardUiState(
            isLoading = false,
            userName = session?.displayName.orEmpty(),
            initials = session?.initials ?: "?",
            today = today,
            greeting = greetingFor(now.hour),
            isOffline = !isOnline,
            lastSyncedAt = lastSyncedAt?.time,
            week = days.takeLast(WEEK_DAYS).map { day ->
                WeekDay(date = day.date, status = summarizeDay.status(day, target), isToday = day.date == today)
            },
            streakDays = calculateStreak(days, today),
            summary = summarizeDay(todayLog, target),
            meals = todayLog.meals,
            notificationCount = todayLog.pendingMeals.size,
        )
    }

    private fun greetingFor(hour: Int): Greeting = when (hour) {
        in 4..10 -> Greeting.MORNING
        in 11..13 -> Greeting.NOON
        in 14..17 -> Greeting.AFTERNOON
        else -> Greeting.EVENING
    }

    private companion object {
        const val WEEK_DAYS = 7
        const val STREAK_LOOKBACK_DAYS = 60
    }
}
