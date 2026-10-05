package com.visionfit.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionfit.core.mvi.CollectEffects
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.MealEntry
import com.visionfit.domain.model.PendingMeal
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.dashboard.components.ConfirmedMealRow
import com.visionfit.presentation.dashboard.components.DashboardHeader
import com.visionfit.presentation.dashboard.components.EnergyCard
import com.visionfit.presentation.dashboard.components.MacroBarsSection
import com.visionfit.presentation.dashboard.components.NoMealsCard
import com.visionfit.presentation.dashboard.components.OfflineBanner
import com.visionfit.presentation.dashboard.components.PendingMealCard
import com.visionfit.presentation.dashboard.components.SectionHeaderLink
import com.visionfit.presentation.dashboard.components.WeekStrip
import com.visionfit.presentation.designsystem.components.MainTab
import com.visionfit.presentation.designsystem.components.VfBottomNavBar
import com.visionfit.presentation.designsystem.components.VfFullScreenLoading
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.preview.PreviewFixtures

@Composable
fun DashboardRoute(
    onNavigateToHistory: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToGoal: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalysis: (jobId: String) -> Unit,
    onNavigateToReview: (jobId: String) -> Unit,
    viewModel: DashboardViewModel = containerViewModel {
        DashboardViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
            mealRepository = mealRepository,
            networkMonitor = networkMonitor,
            timeProvider = timeProvider,
            calculateTarget = calculateNutritionTarget,
            summarizeDay = summarizeDay,
            calculateStreak = calculateStreak,
        )
    },
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            DashboardEffect.NavigateToHistory -> onNavigateToHistory()
            DashboardEffect.NavigateToCamera -> onNavigateToCamera()
            DashboardEffect.NavigateToGoal -> onNavigateToGoal()
            DashboardEffect.NavigateToProfile -> onNavigateToProfile()
            is DashboardEffect.NavigateToAnalysis -> onNavigateToAnalysis(effect.jobId)
            is DashboardEffect.NavigateToReview -> onNavigateToReview(effect.jobId)
        }
    }
    DashboardScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun DashboardScreen(state: DashboardUiState, onEvent: (DashboardEvent) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(VisionFitTheme.colors.background)) {
        val summary = state.summary
        if (state.isLoading || summary == null) {
            VfFullScreenLoading()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = screenPadding(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item(key = "header") {
                    DashboardHeader(
                        initials = state.initials,
                        userName = state.userName,
                        today = state.today,
                        greeting = state.greeting,
                        notificationCount = state.notificationCount,
                        onNotificationsClick = { onEvent(DashboardEvent.NotificationsClicked) },
                    )
                }
                if (state.isOffline) {
                    item(key = "offline") { OfflineBanner(lastSyncedAt = state.lastSyncedAt) }
                }
                item(key = "week") { WeekStrip(days = state.week, streakDays = state.streakDays) }
                item(key = "energy") { EnergyCard(summary = summary) }
                item(key = "macros") { MacroBarsSection(summary = summary, modifier = Modifier.padding(top = 4.dp)) }
                item(key = "meals-header") {
                    SectionHeaderLink(
                        title = "Bữa ăn hôm nay",
                        linkText = "Xem tất cả",
                        onLinkClick = { onEvent(DashboardEvent.SeeAllMealsClicked) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (state.meals.isEmpty()) {
                    item(key = "no-meals") { NoMealsCard(onCaptureClick = { onEvent(DashboardEvent.CaptureClicked) }) }
                } else {
                    mealItems(state.meals, isOffline = state.isOffline, onEvent = onEvent)
                }
            }
        }
        VfBottomNavBar(
            selected = MainTab.TODAY,
            onTabSelected = { onEvent(DashboardEvent.TabSelected(it)) },
            onCaptureClick = { onEvent(DashboardEvent.CaptureClicked) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun LazyListScope.mealItems(
    meals: List<MealEntry>,
    isOffline: Boolean,
    onEvent: (DashboardEvent) -> Unit,
) {
    itemsIndexed(meals, key = { _, meal -> meal.id }) { index, meal ->
        val entrance = Modifier.popIn(delayMillis = 250 + index * 100, durationMillis = 600)
        when (meal) {
            is PendingMeal -> PendingMealCard(
                meal = meal,
                isOffline = isOffline,
                onClick = { onEvent(DashboardEvent.MealClicked(meal)) },
                modifier = entrance,
            )
            is ConfirmedMeal -> ConfirmedMealRow(
                meal = meal,
                onClick = { onEvent(DashboardEvent.MealClicked(meal)) },
                modifier = entrance,
            )
        }
    }
}

@Composable
private fun screenPadding(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val status = WindowInsets.statusBars.asPaddingValues()
    val navigation = WindowInsets.navigationBars.asPaddingValues()
    return PaddingValues(
        start = VfDimens.ScreenPadding + status.calculateStartPadding(layoutDirection),
        end = VfDimens.ScreenPadding + status.calculateEndPadding(layoutDirection),
        top = 18.dp + status.calculateTopPadding(),
        bottom = VfDimens.BottomBarClearance + navigation.calculateBottomPadding(),
    )
}

@Preview(widthDp = 390, heightDp = 1330)
@Composable
private fun DashboardPreview() {
    VisionFitTheme { DashboardScreen(state = PreviewFixtures.dashboard(), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 1330)
@Composable
private fun DashboardOfflineOverTargetPreview() {
    VisionFitTheme { DashboardScreen(state = PreviewFixtures.dashboard(offline = true, overTarget = true), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun DashboardEmptyDayPreview() {
    VisionFitTheme { DashboardScreen(state = PreviewFixtures.dashboard(emptyDay = true), onEvent = {}) }
}
