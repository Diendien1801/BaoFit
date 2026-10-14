package com.visionfit.presentation.dashboard

import com.visionfit.domain.model.DayStatus
import com.visionfit.domain.model.DaySummary
import com.visionfit.domain.model.MealEntry
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.PendingStatus
import com.visionfit.presentation.designsystem.components.MainTab
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

enum class Greeting { MORNING, NOON, AFTERNOON, EVENING }

data class WeekDay(
    val date: LocalDate,
    val status: DayStatus,
    val isToday: Boolean,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    /** The profile could not be downloaded, so today's targets would be someone else's. */
    val isProfileFailed: Boolean = false,
    /** The meal diary could not be downloaded, so "eaten today" would read as zero. */
    val isDiaryFailed: Boolean = false,
    val userName: String = "",
    val initials: String = "",
    val today: LocalDate? = null,
    val greeting: Greeting = Greeting.EVENING,
    val isOffline: Boolean = false,
    val lastSyncedAt: LocalTime? = null,
    val week: List<WeekDay> = emptyList(),
    val streakDays: Int = 0,
    val summary: DaySummary? = null,
    /** Today's diary, newest first. */
    val meals: List<MealEntry> = emptyList(),
    /** Analysis results waiting for the user (shown on the bell). */
    val notificationCount: Int = 0,
)

sealed interface DashboardEvent {
    data object NotificationsClicked : DashboardEvent
    data object SeeAllMealsClicked : DashboardEvent
    data class MealClicked(val meal: MealEntry) : DashboardEvent
    data object CaptureClicked : DashboardEvent
    data class TabSelected(val tab: MainTab) : DashboardEvent
    data object RetryProfileClicked : DashboardEvent
    data object RetryDiaryClicked : DashboardEvent
}

sealed interface DashboardEffect {
    data object NavigateToHistory : DashboardEffect
    data object NavigateToCamera : DashboardEffect
    data object NavigateToGoal : DashboardEffect
    data object NavigateToProfile : DashboardEffect
    data class NavigateToAnalysis(val jobId: String) : DashboardEffect
    data class NavigateToReview(val jobId: String) : DashboardEffect
    data class NavigateToMealDetail(val mealId: String) : DashboardEffect
}

internal fun PendingMeal.destination(): DashboardEffect = when (status) {
    PendingStatus.READY_FOR_REVIEW -> DashboardEffect.NavigateToReview(jobId)
    else -> DashboardEffect.NavigateToAnalysis(jobId)
}
