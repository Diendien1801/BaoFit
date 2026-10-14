package com.visionfit.presentation.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
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
import com.visionfit.domain.model.MealType
import com.visionfit.presentation.common.LocalAppMessenger
import com.visionfit.presentation.common.colors
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.common.label
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.VfCloseButton
import com.visionfit.presentation.designsystem.components.VfErrorState
import com.visionfit.presentation.designsystem.components.VfFullScreenLoading
import com.visionfit.presentation.designsystem.components.VfPrimaryButton
import com.visionfit.presentation.designsystem.components.VfTextButton
import com.visionfit.presentation.designsystem.components.VfTopBar
import com.visionfit.presentation.designsystem.components.topBorder
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.layout.LocalWindowLayout
import com.visionfit.presentation.designsystem.layout.VfContentWidth
import com.visionfit.presentation.designsystem.layout.centeringPadding
import com.visionfit.presentation.designsystem.layout.maxContentWidth
import com.visionfit.presentation.designsystem.layout.safeHorizontal
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.preview.PreviewFixtures
import com.visionfit.presentation.preview.VisionFitPreview
import com.visionfit.presentation.review.components.AddFoodButton
import com.visionfit.presentation.review.components.EditHint
import com.visionfit.presentation.review.components.EmptyTrayCard
import com.visionfit.presentation.review.components.FoodItemEditor
import com.visionfit.presentation.review.components.ImpactBanner
import com.visionfit.presentation.review.components.ReviewSummaryCard

/** Review of a finished analysis, before the meal enters the diary. */
@Composable
fun ReviewRoute(
    jobId: String,
    manualEntry: Boolean,
    onNavigateToDashboard: () -> Unit,
    onNavigateToCamera: () -> Unit,
    viewModel: ReviewViewModel = containerViewModel(key = "review-$jobId-$manualEntry") {
        ReviewViewModel(
            source = ReviewSource.Analysis(jobId, manualEntry),
            analysisRepository = mealAnalysisRepository,
            mealRepository = mealRepository,
            profileRepository = profileRepository,
            calculateTarget = calculateNutritionTarget,
            summarizeDay = summarizeDay,
        )
    },
) {
    ReviewEffectsAndScreen(viewModel, onLeave = onNavigateToDashboard, onNavigateToCamera = onNavigateToCamera)
}

/** Detail of a meal already in the diary: change it or delete it, then go back where the user came from. */
@Composable
fun MealDetailRoute(
    mealId: String,
    onNavigateBack: () -> Unit,
    viewModel: ReviewViewModel = containerViewModel(key = "meal-detail-$mealId") {
        ReviewViewModel(
            source = ReviewSource.SavedMeal(mealId),
            analysisRepository = mealAnalysisRepository,
            mealRepository = mealRepository,
            profileRepository = profileRepository,
            calculateTarget = calculateNutritionTarget,
            summarizeDay = summarizeDay,
        )
    },
) {
    ReviewEffectsAndScreen(viewModel, onLeave = onNavigateBack, onNavigateToCamera = onNavigateBack)
}

@Composable
private fun ReviewEffectsAndScreen(viewModel: ReviewViewModel, onLeave: () -> Unit, onNavigateToCamera: () -> Unit) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val messenger = LocalAppMessenger.current
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ReviewEffect.MealSaved -> {
                messenger.show("Đã lưu ${effect.mealType.label.lowercase()} · ${VnFormat.thousands(effect.totalKcal)} kcal")
                onLeave()
            }
            is ReviewEffect.MealUpdated -> {
                messenger.show("Đã cập nhật ${effect.mealType.label.lowercase()} · ${VnFormat.thousands(effect.totalKcal)} kcal")
                onLeave()
            }
            ReviewEffect.MealDeleted -> {
                messenger.show("Đã xóa bữa ăn khỏi nhật ký")
                onLeave()
            }
            ReviewEffect.NavigateToCamera -> onNavigateToCamera()
            ReviewEffect.NavigateToDashboard, ReviewEffect.NavigateBack -> onLeave()
            is ReviewEffect.ItemRemoved -> messenger.show(
                message = "Đã xóa “${effect.name}”",
                actionLabel = "Hoàn tác",
                onAction = { viewModel.onEvent(ReviewEvent.UndoRemove) },
            )
            is ReviewEffect.ShowIssue -> messenger.show(effect.issue.message(effect.itemNumber))
            ReviewEffect.SaveFailed -> messenger.show("Chưa lưu được bữa ăn. Thử lại nhé!")
            ReviewEffect.DeleteFailed -> messenger.show("Chưa xóa được bữa ăn. Kiểm tra mạng rồi thử lại nhé!")
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
            action = {
                when {
                    !state.isSavedMeal -> RetakeButton(onClick = { onEvent(ReviewEvent.Retake) })
                    !state.isLoading && !state.isMissing -> DeleteButton(onClick = { onEvent(ReviewEvent.DeleteClicked) })
                }
            },
            title = {
                Text(
                    text = if (state.isSavedMeal) "Chi tiết bữa ăn" else "Kiểm tra kết quả",
                    style = VisionFitTheme.type.sectionTitleSmall,
                    maxLines = 2,
                    modifier = Modifier.semantics { heading() },
                )
            },
        )
        if (state.isConfirmingDelete) {
            DeleteMealDialog(
                onConfirm = { onEvent(ReviewEvent.DeleteConfirmed) },
                onDismiss = { onEvent(ReviewEvent.DeleteDismissed) },
            )
        }
        when {
            state.isLoading -> VfFullScreenLoading(Modifier.weight(1f))
            state.isMissing -> VfErrorState(
                title = "Không tìm thấy bữa ăn này",
                message = if (state.isSavedMeal) {
                    "Có thể bữa ăn đã bị xóa. Quay lại nhật ký để xem nhé."
                } else {
                    "Có thể ảnh đã bị xóa. Quay về trang chính để xem nhật ký nhé."
                },
                onRetry = { onEvent(ReviewEvent.Close) },
                modifier = Modifier.weight(1f),
            )
            else -> {
                if (LocalWindowLayout.current.usesTwoPanes) {
                    ReviewTwoPanes(state, onEvent, Modifier.weight(1f))
                } else {
                    ReviewSingleColumn(state, onEvent, Modifier.weight(1f))
                }
                ReviewBottomBar(state, onEvent)
            }
        }
    }
}

/** Phones and portrait tablets: summary, hints and dishes in one list, capped in width. */
@Composable
private fun ReviewSingleColumn(state: ReviewUiState, onEvent: (ReviewEvent) -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeHorizontal)) {
        val side = centeringPadding(maxWidth, VfContentWidth.Column, LocalWindowLayout.current.gutter)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = side, end = side, top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "summary") { ReviewSummaryCard(state) }
            if (state.isSavedMeal) {
                item(key = "meal-type") { MealTypePicker(state.mealType, onSelected = { onEvent(ReviewEvent.MealTypeSelected(it)) }) }
            }
            item(key = "impact") { ImpactBanner(state.remainingBeforeKcal, state.remainingAfterKcal) }
            item(key = "hint") { EditHint() }
            foodItems(state, onEvent)
        }
    }
}

/** Landscape and wide windows: the photo and totals stay in view while the dishes scroll beside them. */
@Composable
private fun ReviewTwoPanes(state: ReviewUiState, onEvent: (ReviewEvent) -> Unit, modifier: Modifier) {
    val gutter = LocalWindowLayout.current.gutter
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeHorizontal)
            .maxContentWidth(VfContentWidth.TwoPane),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = gutter, end = 12.dp, top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReviewSummaryCard(state)
            if (state.isSavedMeal) {
                MealTypePicker(state.mealType, onSelected = { onEvent(ReviewEvent.MealTypeSelected(it)) })
            }
            ImpactBanner(state.remainingBeforeKcal, state.remainingAfterKcal)
            EditHint()
        }
        LazyColumn(
            modifier = Modifier.weight(1.1f).fillMaxHeight(),
            contentPadding = PaddingValues(start = 12.dp, end = gutter, top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            foodItems(state, onEvent)
        }
    }
}

/** Editable dishes (or the empty tray) followed by "add a dish". */
private fun LazyListScope.foodItems(state: ReviewUiState, onEvent: (ReviewEvent) -> Unit) {
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
private fun DeleteButton(onClick: () -> Unit) {
    val colors = VisionFitTheme.colors
    BrutalSurface(
        modifier = Modifier.height(VfDimens.TopBarButton),
        shape = RoundedCornerShape(VfRadius.L),
        color = colors.coralContainer,
        shadowOffset = VfDimens.ShadowS,
        onClick = onClick,
        onClickLabel = "Xóa bữa ăn",
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VfIcon(VfIcons.Trash, contentDescription = null, size = 16.dp, tint = colors.ink)
            Text(text = "Xóa", style = VisionFitTheme.type.labelM)
        }
    }
}

/** Breakfast, lunch, snack or dinner: a saved meal can be moved to another one. */
@Composable
private fun MealTypePicker(selected: MealType, onSelected: (MealType) -> Unit) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MealType.entries.forEach { type ->
            val isSelected = type == selected
            BrutalSurface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .semantics { this.selected = isSelected },
                shape = RoundedCornerShape(VfRadius.L),
                color = if (isSelected) type.colors().badge else colors.surface,
                shadowOffset = if (isSelected) VfDimens.ShadowS else 0.dp,
                onClick = { onSelected(type) },
                role = Role.RadioButton,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = type.label.removePrefix("Bữa "),
                    style = VisionFitTheme.type.labelM,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DeleteMealDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xóa bữa ăn này?", style = VisionFitTheme.type.sectionTitleSmall) },
        text = { Text("Bữa ăn sẽ biến mất khỏi nhật ký và không còn tính vào tổng calo.", style = VisionFitTheme.type.body) },
        confirmButton = { VfTextButton(text = "Xóa", onClick = onConfirm, color = VisionFitTheme.colors.coral) },
        dismissButton = { VfTextButton(text = "Hủy", onClick = onDismiss, color = VisionFitTheme.colors.textSecondary) },
        containerColor = VisionFitTheme.colors.surface,
    )
}

@Composable
private fun ReviewBottomBar(state: ReviewUiState, onEvent: (ReviewEvent) -> Unit) {
    val colors = VisionFitTheme.colors
    val layout = LocalWindowLayout.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .topBorder(VfDimens.Border, colors.ink)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .windowInsetsPadding(WindowInsets.safeHorizontal),
    ) {
        Row(
            modifier = Modifier
                .maxContentWidth(VfContentWidth.Column)
                .padding(
                    start = layout.gutter,
                    end = layout.gutter,
                    top = if (layout.isShort) 8.dp else 12.dp,
                    bottom = if (layout.isShort) 10.dp else 22.dp,
                ),
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
                text = if (state.isSavedMeal) "Lưu thay đổi" else "Xác nhận & lưu",
                onClick = { onEvent(ReviewEvent.Save) },
                // The check icon is the first thing to go when a small phone runs out of room.
                leadingIcon = if (layout.isNarrow) null else VfIcons.Check,
                trailingIcon = null,
                enabled = state.canSave,
                loading = state.isSaving,
                modifier = Modifier.weight(1f),
            )
        }
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
    VisionFitPreview { ReviewScreen(state = PreviewFixtures.review(), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReviewManualEmptyPreview() {
    VisionFitPreview { ReviewScreen(state = PreviewFixtures.review(manual = true), onEvent = {}) }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun ReviewDesktopPreview() {
    VisionFitPreview { ReviewScreen(state = PreviewFixtures.review(), onEvent = {}) }
}
