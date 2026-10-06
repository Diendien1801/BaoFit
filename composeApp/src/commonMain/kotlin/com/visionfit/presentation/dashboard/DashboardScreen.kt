package com.visionfit.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionfit.core.mvi.CollectEffects
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DaySummary
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
import com.visionfit.presentation.designsystem.components.MainTabScaffold
import com.visionfit.presentation.designsystem.components.VfFullScreenLoading
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.layout.LocalWindowLayout
import com.visionfit.presentation.designsystem.layout.VfContentWidth
import com.visionfit.presentation.designsystem.layout.centeringPadding
import com.visionfit.presentation.designsystem.layout.expandedBy
import com.visionfit.presentation.designsystem.layout.maxContentWidth
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.preview.PreviewFixtures
import com.visionfit.presentation.preview.VisionFitPreview

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
    MainTabScaffold(
        selected = MainTab.TODAY,
        onTabSelected = { onEvent(DashboardEvent.TabSelected(it)) },
        onCaptureClick = { onEvent(DashboardEvent.CaptureClicked) },
        modifier = modifier.background(VisionFitTheme.colors.background),
    ) { contentPadding ->
        val summary = state.summary
        when {
            state.isLoading || summary == null -> VfFullScreenLoading()
            LocalWindowLayout.current.usesTwoPanes -> DashboardTwoPanes(state, summary, contentPadding, onEvent)
            else -> DashboardSingleColumn(state, summary, contentPadding, onEvent)
        }
    }
}

/** Phones and portrait tablets: one scrolling column, centered and capped in width. */
@Composable
private fun DashboardSingleColumn(
    state: DashboardUiState,
    summary: DaySummary,
    contentPadding: PaddingValues,
    onEvent: (DashboardEvent) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val side = centeringPadding(maxWidth, VfContentWidth.Column, LocalWindowLayout.current.gutter)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding.expandedBy(start = side, top = 18.dp, end = side),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            overviewItems(state, summary, onEvent)
            mealsSection(state, onEvent)
        }
    }
}

/** Wide and landscape windows: the numbers of the day on the left, its meals beside them. */
@Composable
private fun DashboardTwoPanes(
    state: DashboardUiState,
    summary: DaySummary,
    contentPadding: PaddingValues,
    onEvent: (DashboardEvent) -> Unit,
) {
    val gutter = LocalWindowLayout.current.gutter
    Row(
        modifier = Modifier.fillMaxSize().maxContentWidth(VfContentWidth.TwoPane),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            // Some room on the inner side for the hard shadows of the cards.
            contentPadding = contentPadding.expandedBy(start = gutter, top = 18.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            overviewItems(state, summary, onEvent)
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = contentPadding.expandedBy(top = 18.dp, end = gutter),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            mealsSection(state, onEvent, isOwnPane = true)
        }
    }
}

/** Greeting, offline notice, week, energy and macros. */
private fun LazyListScope.overviewItems(state: DashboardUiState, summary: DaySummary, onEvent: (DashboardEvent) -> Unit) {
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
}

/** "Bữa ăn hôm nay": pending and confirmed meals, or an invitation to log the first one. */
private fun LazyListScope.mealsSection(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    isOwnPane: Boolean = false,
) {
    item(key = "meals-header") {
        SectionHeaderLink(
            title = "Bữa ăn hôm nay",
            linkText = "Xem tất cả",
            onLinkClick = { onEvent(DashboardEvent.SeeAllMealsClicked) },
            modifier = if (isOwnPane) Modifier else Modifier.padding(top = 4.dp),
        )
    }
    if (state.meals.isEmpty()) {
        item(key = "no-meals") { NoMealsCard(onCaptureClick = { onEvent(DashboardEvent.CaptureClicked) }) }
    } else {
        itemsIndexed(state.meals, key = { _, meal -> meal.id }) { index, meal ->
            val entrance = Modifier.popIn(delayMillis = 250 + index * 100, durationMillis = 600)
            when (meal) {
                is PendingMeal -> PendingMealCard(
                    meal = meal,
                    isOffline = state.isOffline,
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
}

@Preview(widthDp = 390, heightDp = 1330)
@Composable
private fun DashboardPreview() {
    VisionFitPreview { DashboardScreen(state = PreviewFixtures.dashboard(), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 1330)
@Composable
private fun DashboardOfflineOverTargetPreview() {
    VisionFitPreview { DashboardScreen(state = PreviewFixtures.dashboard(offline = true, overTarget = true), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun DashboardEmptyDayPreview() {
    VisionFitPreview { DashboardScreen(state = PreviewFixtures.dashboard(emptyDay = true), onEvent = {}) }
}

@Preview(widthDp = 844, heightDp = 390)
@Composable
private fun DashboardLandscapePreview() {
    VisionFitPreview { DashboardScreen(state = PreviewFixtures.dashboard(), onEvent = {}) }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun DashboardDesktopPreview() {
    VisionFitPreview { DashboardScreen(state = PreviewFixtures.dashboard(), onEvent = {}) }
}
