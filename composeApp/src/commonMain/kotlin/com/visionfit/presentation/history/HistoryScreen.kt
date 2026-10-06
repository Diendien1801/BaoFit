package com.visionfit.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionfit.core.format.VnFormat
import com.visionfit.core.mvi.CollectEffects
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.designsystem.components.MainTab
import com.visionfit.presentation.designsystem.components.MainTabScaffold
import com.visionfit.presentation.designsystem.components.VfIconTileButton
import com.visionfit.presentation.designsystem.components.VfLoadingIndicator
import com.visionfit.presentation.designsystem.components.VfMessageCard
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.layout.LocalWindowLayout
import com.visionfit.presentation.designsystem.layout.VfContentWidth
import com.visionfit.presentation.designsystem.layout.centeringPadding
import com.visionfit.presentation.designsystem.layout.expandedBy
import com.visionfit.presentation.designsystem.layout.maxContentWidth
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.history.components.DateChipsRow
import com.visionfit.presentation.history.components.DaySummaryCard
import com.visionfit.presentation.history.components.DiaryDatePickerDialog
import com.visionfit.presentation.history.components.OfflineCacheNote
import com.visionfit.presentation.history.components.TimelineEntry
import com.visionfit.presentation.preview.PreviewFixtures
import com.visionfit.presentation.preview.VisionFitPreview

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
    MainTabScaffold(
        selected = MainTab.HISTORY,
        onTabSelected = { onEvent(HistoryEvent.TabSelected(it)) },
        onCaptureClick = { onEvent(HistoryEvent.CaptureClicked) },
        modifier = modifier.background(VisionFitTheme.colors.background),
    ) { contentPadding ->
        if (LocalWindowLayout.current.usesTwoPanes) {
            HistoryTwoPanes(state, contentPadding, onEvent)
        } else {
            HistorySingleColumn(state, contentPadding, onEvent)
        }
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

/** Space between the items of a list and the edges of its pane. */
private class Gutters(val start: Dp, val end: Dp) {
    val modifier: Modifier get() = Modifier.padding(start = start, end = end)
}

/** Phones and portrait tablets: title, day chips, totals and timeline in one column. */
@Composable
private fun HistorySingleColumn(state: HistoryUiState, contentPadding: PaddingValues, onEvent: (HistoryEvent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val side = centeringPadding(maxWidth, VfContentWidth.Column, LocalWindowLayout.current.gutter)
        val gutters = Gutters(side, side)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding.expandedBy(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            headerItems(state, onEvent, gutters)
            if (hasEntries(state)) summaryItems(state, gutters, withCacheNote = false)
            timelineItems(state, onEvent, gutters, withCacheNote = true)
        }
    }
}

/** Wide and landscape windows: day chips and totals on the left, the timeline beside them. */
@Composable
private fun HistoryTwoPanes(state: HistoryUiState, contentPadding: PaddingValues, onEvent: (HistoryEvent) -> Unit) {
    val gutter = LocalWindowLayout.current.gutter
    Row(Modifier.fillMaxSize().maxContentWidth(VfContentWidth.TwoPane)) {
        val panePadding = contentPadding.expandedBy(top = 18.dp)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = panePadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val gutters = Gutters(gutter, 12.dp)
            headerItems(state, onEvent, gutters)
            if (hasEntries(state)) summaryItems(state, gutters, withCacheNote = true)
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = panePadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            timelineItems(state, onEvent, Gutters(12.dp, gutter), withCacheNote = false)
        }
    }
}

private fun hasEntries(state: HistoryUiState): Boolean =
    !state.isLoading && state.summary != null && state.meals.isNotEmpty()

private fun LazyListScope.headerItems(state: HistoryUiState, onEvent: (HistoryEvent) -> Unit, gutters: Gutters) {
    item(key = "title") {
        Row(
            modifier = Modifier.fillMaxWidth().then(gutters.modifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Nhật ký bữa ăn",
                style = VisionFitTheme.type.headlineL,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
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
            horizontalPadding = gutters.start,
        )
    }
}

/** Totals of the selected day, optionally followed by the offline / cached-data note. */
private fun LazyListScope.summaryItems(state: HistoryUiState, gutters: Gutters, withCacheNote: Boolean) {
    val summary = state.summary ?: return
    item(key = "summary-${state.selectedDate}") {
        DaySummaryCard(summary = summary, isToday = state.isToday, modifier = gutters.modifier)
    }
    if (withCacheNote) cacheNoteItem(state, gutters)
}

private fun LazyListScope.cacheNoteItem(state: HistoryUiState, gutters: Gutters) {
    item(key = "cache-note") { OfflineCacheNote(isOffline = state.isOffline, modifier = gutters.modifier) }
}

/** The meals of the selected day as a timeline, or loading / empty states in their place. */
private fun LazyListScope.timelineItems(
    state: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit,
    gutters: Gutters,
    withCacheNote: Boolean,
) {
    when {
        state.isLoading || state.summary == null -> item(key = "loading") {
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
                modifier = gutters.modifier.padding(top = 8.dp),
            )
        }
        else -> {
            itemsIndexed(state.meals, key = { _, meal -> meal.id }) { index, meal ->
                TimelineEntry(
                    meal = meal,
                    index = index,
                    isLast = index == state.meals.lastIndex,
                    isOffline = state.isOffline,
                    onClick = { onEvent(HistoryEvent.MealClicked(meal)) },
                    modifier = gutters.modifier.padding(top = if (index == 0) 8.dp else 6.dp),
                )
            }
            if (withCacheNote) cacheNoteItem(state, gutters)
        }
    }
}

@Preview(widthDp = 390, heightDp = 1320)
@Composable
private fun HistoryPreview() {
    VisionFitPreview { HistoryScreen(state = PreviewFixtures.history(), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HistoryEmptyDayPreview() {
    VisionFitPreview { HistoryScreen(state = PreviewFixtures.history(emptyPastDay = true), onEvent = {}) }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun HistoryDesktopPreview() {
    VisionFitPreview { HistoryScreen(state = PreviewFixtures.history(), onEvent = {}) }
}
