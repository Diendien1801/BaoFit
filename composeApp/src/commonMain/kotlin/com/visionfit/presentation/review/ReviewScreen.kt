package com.visionfit.presentation.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionfit.core.format.VnFormat
import com.visionfit.core.mvi.CollectEffects
import com.visionfit.presentation.common.LocalAppMessenger
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.common.label
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.VfCloseButton
import com.visionfit.presentation.designsystem.components.VfErrorState
import com.visionfit.presentation.designsystem.components.VfFullScreenLoading
import com.visionfit.presentation.designsystem.components.VfPrimaryButton
import com.visionfit.presentation.designsystem.components.VfTopBar
import com.visionfit.presentation.designsystem.components.topBorder
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.preview.PreviewFixtures
import com.visionfit.presentation.review.components.AddFoodButton
import com.visionfit.presentation.review.components.EditHint
import com.visionfit.presentation.review.components.EmptyTrayCard
import com.visionfit.presentation.review.components.FoodItemEditor
import com.visionfit.presentation.review.components.ImpactBanner
import com.visionfit.presentation.review.components.ReviewSummaryCard

@Composable
fun ReviewRoute(
    jobId: String,
    manualEntry: Boolean,
    onNavigateToDashboard: () -> Unit,
    onNavigateToCamera: () -> Unit,
    viewModel: ReviewViewModel = containerViewModel(key = "review-$jobId-$manualEntry") {
        ReviewViewModel(
            jobId = jobId,
            isManualEntry = manualEntry,
            analysisRepository = mealAnalysisRepository,
            mealRepository = mealRepository,
            profileRepository = profileRepository,
            calculateTarget = calculateNutritionTarget,
            summarizeDay = summarizeDay,
        )
    },
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val messenger = LocalAppMessenger.current
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ReviewEffect.MealSaved -> {
                messenger.show("Đã lưu ${effect.mealType.label.lowercase()} · ${VnFormat.thousands(effect.totalKcal)} kcal")
                onNavigateToDashboard()
            }
            ReviewEffect.NavigateToCamera -> onNavigateToCamera()
            ReviewEffect.NavigateToDashboard -> onNavigateToDashboard()
            is ReviewEffect.ItemRemoved -> messenger.show(
                message = "Đã xóa “${effect.name}”",
                actionLabel = "Hoàn tác",
                onAction = { viewModel.onEvent(ReviewEvent.UndoRemove) },
            )
            is ReviewEffect.ShowIssue -> messenger.show(effect.issue.message(effect.itemNumber))
            ReviewEffect.SaveFailed -> messenger.show("Chưa lưu được bữa ăn. Thử lại nhé!")
        }
    }
    ReviewScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun ReviewScreen(state: ReviewUiState, onEvent: (ReviewEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VisionFitTheme.colors.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        VfTopBar(
            navigation = { VfCloseButton(onClick = { onEvent(ReviewEvent.Close) }) },
            action = { RetakeButton(onClick = { onEvent(ReviewEvent.Retake) }) },
            title = {
                Text(text = "Kiểm tra kết quả", style = VisionFitTheme.type.sectionTitleSmall, modifier = Modifier.semantics { heading() })
            },
        )
        when {
            state.isLoading -> VfFullScreenLoading(Modifier.weight(1f))
            state.isMissing -> VfErrorState(
                title = "Không tìm thấy bữa ăn này",
                message = "Có thể ảnh đã bị xóa. Quay về trang chính để xem nhật ký nhé.",
                onRetry = { onEvent(ReviewEvent.Close) },
                modifier = Modifier.weight(1f),
            )
            else -> {
                ReviewList(state, onEvent, Modifier.weight(1f))
                ReviewBottomBar(state, onEvent)
            }
        }
    }
}

@Composable
private fun ReviewList(state: ReviewUiState, onEvent: (ReviewEvent) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = VfDimens.ScreenPadding, end = VfDimens.ScreenPadding, top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "summary") { ReviewSummaryCard(state) }
        item(key = "impact") { ImpactBanner(state.remainingBeforeKcal, state.remainingAfterKcal) }
        item(key = "hint") { EditHint() }
        if (state.items.isEmpty()) {
            item(key = "empty") { EmptyTrayCard() }
        }
        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
            FoodItemEditor(
                item = item,
                number = index + 1,
                isInvalid = item.id in state.invalidItemIds,
                onNameChange = { onEvent(ReviewEvent.NameChanged(item.id, it)) },
                onGramsChange = { onEvent(ReviewEvent.GramsChanged(item.id, it)) },
                onKcalChange = { onEvent(ReviewEvent.KcalChanged(item.id, it)) },
                onRemove = { onEvent(ReviewEvent.RemoveItem(item.id)) },
                modifier = Modifier.animateItem(),
            )
        }
        item(key = "add") { AddFoodButton(onClick = { onEvent(ReviewEvent.AddItem) }, modifier = Modifier.animateItem()) }
    }
}

@Composable
private fun RetakeButton(onClick: () -> Unit) {
    val colors = VisionFitTheme.colors
    BrutalSurface(
        modifier = Modifier.height(VfDimens.TopBarButton),
        shape = RoundedCornerShape(VfRadius.L),
        color = colors.yellow,
        shadowOffset = VfDimens.ShadowS,
        onClick = onClick,
        onClickLabel = "Chụp lại",
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VfIcon(VfIcons.Retry, contentDescription = null, size = 16.dp, tint = colors.ink)
            Text(text = "Chụp lại", style = VisionFitTheme.type.labelM)
        }
    }
}

@Composable
private fun ReviewBottomBar(state: ReviewUiState, onEvent: (ReviewEvent) -> Unit) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .topBorder(VfDimens.Border, colors.ink)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(start = VfDimens.ScreenPadding, end = VfDimens.ScreenPadding, top = 12.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = "Tổng bữa ăn", style = VisionFitTheme.type.caption, color = colors.textSecondary)
            Text(
                text = buildAnnotatedString {
                    append(VnFormat.thousands(state.totalKcal))
                    withStyle(SpanStyle(fontSize = 14.sp)) { append(" kcal") }
                },
                style = VisionFitTheme.type.titleXL,
                maxLines = 1,
            )
        }
        VfPrimaryButton(
            text = "Xác nhận & lưu",
            onClick = { onEvent(ReviewEvent.Save) },
            leadingIcon = VfIcons.Check,
            trailingIcon = null,
            enabled = state.canSave,
            loading = state.isSaving,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun ReviewIssue.message(itemNumber: Int?): String = when (this) {
    ReviewIssue.NO_ITEMS -> "Mâm đang trống, thêm ít nhất một món nhé!"
    ReviewIssue.MISSING_NAME -> "Đặt tên cho món số ${itemNumber ?: 1} nhé!"
    ReviewIssue.MISSING_WEIGHT -> "Món số ${itemNumber ?: 1} cần khối lượng lớn hơn 0 g."
}

@Preview(widthDp = 390, heightDp = 1310)
@Composable
private fun ReviewPreview() {
    VisionFitTheme { ReviewScreen(state = PreviewFixtures.review(), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReviewManualEmptyPreview() {
    VisionFitTheme { ReviewScreen(state = PreviewFixtures.review(manual = true), onEvent = {}) }
}
