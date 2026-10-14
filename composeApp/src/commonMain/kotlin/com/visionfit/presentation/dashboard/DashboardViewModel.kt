package com.visionfit.presentation.dashboard

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.core.time.TimeProvider
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.DiaryStatus
import com.visionfit.domain.model.NutritionTarget
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.ProfileStatus
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

class DashboardViewModel(
    authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    networkMonitor: NetworkMonitor,
    private val timeProvider: TimeProvider,
    private val calculateTarget: CalculateNutritionTargetUseCase,
    private val summarizeDay: SummarizeDayUseCase,
    private val calculateStreak: CalculateStreakUseCase,
) : MviViewModel<DashboardUiState, DashboardEvent, DashboardEffect>(DashboardUiState()) {

    init {
        val now = timeProvider.now()
        val today = now.date
        // null until the diary has loaded: the repository only emits days it really knows.
        val history = mealRepository.observeRange(from = today.minus(STREAK_LOOKBACK_DAYS, DateTimeUnit.DAY), to = today)
            .map<List<DayLog>, List<DayLog>?> { it }
            .onStart { emit(null) }
        val connectivity = combine(networkMonitor.isOnline, networkMonitor.lastSyncedAt) { online, synced -> online to synced }

        val profile = combine(profileRepository.status, profileRepository.profile) { status, profile ->
            status to calculateTarget(profile)
        }

        combine(
            authRepository.session,
            profile,
            history,
            connectivity,
            mealRepository.status,
        ) { session, (profileStatus, target), days, (online, syncedAt), diaryStatus ->
            buildState(now, session, profileStatus, target, days, diaryStatus, online, syncedAt)
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
                is ConfirmedMeal -> sendEffect(DashboardEffect.NavigateToMealDetail(meal.id))
            }
            is DashboardEvent.TabSelected -> when (event.tab) {
                MainTab.TODAY -> Unit
                MainTab.HISTORY -> sendEffect(DashboardEffect.NavigateToHistory)
                MainTab.GOAL -> sendEffect(DashboardEffect.NavigateToGoal)
                // A saved profile opens on its result (step 2); editing is one tap away there.
                // Without one (never created, still loading, failed) the editor handles those states.
                MainTab.PROFILE -> sendEffect(
                    if (profileRepository.status.value == ProfileStatus.READY) {
                        DashboardEffect.NavigateToGoal
                    } else {
                        DashboardEffect.NavigateToProfile
                    },
                )
            }
            DashboardEvent.RetryProfileClicked -> viewModelScope.launch { profileRepository.refresh() }
            DashboardEvent.RetryDiaryClicked -> viewModelScope.launch { mealRepository.refresh() }
        }
    }

    /** @param loadedDays `null` until the diary has loaded. */
    private fun buildState(
        now: LocalDateTime,
        session: UserSession?,
        profileStatus: ProfileStatus,
        target: NutritionTarget,
        loadedDays: List<DayLog>?,
        diaryStatus: DiaryStatus,
        isOnline: Boolean,
        lastSyncedAt: LocalDateTime?,
    ): DashboardUiState {
        val today = now.date
        val days = loadedDays.orEmpty()
        val todayLog = days.lastOrNull { it.date == today } ?: DayLog(today, emptyList())
        return DashboardUiState(
            isLoading = profileStatus == ProfileStatus.LOADING || (loadedDays == null && diaryStatus != DiaryStatus.FAILED),
            isProfileFailed = profileStatus == ProfileStatus.FAILED,
            isDiaryFailed = loadedDays == null && diaryStatus == DiaryStatus.FAILED,
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
