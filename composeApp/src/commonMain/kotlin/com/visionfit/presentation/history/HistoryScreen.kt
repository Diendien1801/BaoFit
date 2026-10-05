package com.visionfit.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionfit.core.format.VnFormat
import com.visionfit.core.mvi.CollectEffects
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.designsystem.components.MainTab
import com.visionfit.presentation.designsystem.components.VfBottomNavBar
import com.visionfit.presentation.designsystem.components.VfIconTileButton
import com.visionfit.presentation.designsystem.components.VfLoadingIndicator
import com.visionfit.presentation.designsystem.components.VfMessageCard
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.history.components.DateChipsRow
import com.visionfit.presentation.history.components.DaySummaryCard
import com.visionfit.presentation.history.components.DiaryDatePickerDialog
import com.visionfit.presentation.history.components.OfflineCacheNote
import com.visionfit.presentation.history.components.TimelineEntry
import com.visionfit.presentation.preview.PreviewFixtures

@Composable
fun HistoryRoute(
    onNavigateToAnalysis: (jobId: String) -> Unit,
    onNavigateToReview: (jobId: String) -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToGoal: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HistoryViewModel = containerViewModel {
        HistoryViewModel(mealRepository, profileRepository, networkMonitor, timeProvider, calculateNutritionTarget, summarizeDay)
    },
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is HistoryEffect.NavigateToAnalysis -> onNavigateToAnalysis(effect.jobId)
            is HistoryEffect.NavigateToReview -> onNavigateToReview(effect.jobId)
            HistoryEffect.NavigateToCamera -> onNavigateToCamera()
            HistoryEffect.NavigateToDashboard -> onNavigateToDashboard()
            HistoryEffect.NavigateToGoal -> onNavigateToGoal()
            HistoryEffect.NavigateToProfile -> onNavigateToProfile()
        }
    }
    HistoryScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun HistoryScreen(state: HistoryUiState, onEvent: (HistoryEvent) -> Unit, modifier: Modifier = Modifier) {
    val status = WindowInsets.statusBars.asPaddingValues()
    val navigation = WindowInsets.navigationBars.asPaddingValues()
    Box(modifier = modifier.fillMaxSize().background(VisionFitTheme.colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 18.dp + status.calculateTopPadding(),
                bottom = VfDimens.BottomBarClearance + navigation.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "title") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = VfDimens.ScreenPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Nhật ký bữa ăn", style = VisionFitTheme.type.headlineL, modifier = Modifier.semantics { heading() })
                    VfIconTileButton(
                        icon = VfIcons.Calendar,
                        contentDescription = "Chọn ngày",
                        onClick = { onEvent(HistoryEvent.OpenDatePicker) },
                    )
                }
            }
            item(key = "dates") {
                DateChipsRow(
                    dates = state.dates,
                    selected = state.selectedDate,
                    today = state.today,
                    onDateSelected = { onEvent(HistoryEvent.DateSelected(it)) },
                )
            }
            dayContent(state, onEvent)
        }
        VfBottomNavBar(
            selected = MainTab.HISTORY,
            onTabSelected = { onEvent(HistoryEvent.TabSelected(it)) },
            onCaptureClick = { onEvent(HistoryEvent.CaptureClicked) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
    val today = state.today
    if (state.isDatePickerVisible && today != null) {
        DiaryDatePickerDialog(
            initialDate = state.selectedDate ?: today,
            today = today,
            onDatePicked = { onEvent(HistoryEvent.DatePicked(it)) },
            onDismiss = { onEvent(HistoryEvent.DismissDatePicker) },
        )
    }
}

private fun LazyListScope.dayContent(state: HistoryUiState, onEvent: (HistoryEvent) -> Unit) {
    val horizontal = Modifier.padding(horizontal = VfDimens.ScreenPadding)
    val summary = state.summary
    when {
        state.isLoading || summary == null -> item(key = "loading") {
            Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) { VfLoadingIndicator() }
        }
        state.meals.isEmpty() -> item(key = "empty-${state.selectedDate}") {
            VfMessageCard(
                title = if (state.isToday) "Hôm nay chưa có bữa nào" else "Chưa có bữa nào được ghi",
                message = if (state.isToday) {
                    "Chụp bữa ăn đầu tiên, AI sẽ tính calo giúp bạn."
                } else {
                    "Ngày ${state.selectedDate?.let(VnFormat::shortDate).orEmpty()} bạn chưa ghi lại bữa ăn nào."
                },
                icon = VfIcons.Calendar,
                actionLabel = if (state.isToday) "Chụp bữa ăn" else null,
                onAction = if (state.isToday) ({ onEvent(HistoryEvent.CaptureClicked) }) else null,
                modifier = horizontal.padding(top = 8.dp),
            )
        }
        else -> {
            item(key = "summary-${state.selectedDate}") { DaySummaryCard(summary = summary, isToday = state.isToday, modifier = horizontal) }
            itemsIndexed(state.meals, key = { _, meal -> meal.id }) { index, meal ->
                TimelineEntry(
                    meal = meal,
                    index = index,
                    isLast = index == state.meals.lastIndex,
                    isOffline = state.isOffline,
                    onClick = { onEvent(HistoryEvent.MealClicked(meal)) },
                    modifier = horizontal.padding(top = if (index == 0) 8.dp else 6.dp),
                )
            }
            item(key = "cache-note") { OfflineCacheNote(isOffline = state.isOffline, modifier = horizontal) }
        }
    }
}

@Preview(widthDp = 390, heightDp = 1320)
@Composable
private fun HistoryPreview() {
    VisionFitTheme { HistoryScreen(state = PreviewFixtures.history(), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HistoryEmptyDayPreview() {
    VisionFitTheme { HistoryScreen(state = PreviewFixtures.history(emptyPastDay = true), onEvent = {}) }
}
