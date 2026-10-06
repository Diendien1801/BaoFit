package com.visionfit.presentation.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionfit.core.format.VnFormat
import com.visionfit.core.mvi.CollectEffects
import com.visionfit.domain.model.AnalysisFailure
import com.visionfit.domain.model.AnalysisJob
import com.visionfit.domain.model.AnalysisStage
import com.visionfit.presentation.analysis.components.AnalysisPhoto
import com.visionfit.presentation.analysis.components.AnalysisStepsCard
import com.visionfit.presentation.analysis.components.PulseDot
import com.visionfit.presentation.analysis.components.WorkingDots
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.common.label
import com.visionfit.presentation.designsystem.components.VfCloseButton
import com.visionfit.presentation.designsystem.components.VfErrorState
import com.visionfit.presentation.designsystem.components.VfFullScreenLoading
import com.visionfit.presentation.designsystem.components.VfPill
import com.visionfit.presentation.designsystem.components.VfPrimaryButton
import com.visionfit.presentation.designsystem.components.VfSecondaryLink
import com.visionfit.presentation.designsystem.components.VfTopBar
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.layout.LocalWindowLayout
import com.visionfit.presentation.designsystem.layout.VfContentWidth
import com.visionfit.presentation.designsystem.layout.maxContentWidth
import com.visionfit.presentation.designsystem.layout.proportionalHeight
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.preview.PreviewFixtures
import com.visionfit.presentation.preview.VisionFitPreview

@Composable
fun AnalysisRoute(
    jobId: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToReview: (jobId: String, manualEntry: Boolean) -> Unit,
    viewModel: AnalysisViewModel = containerViewModel(key = "analysis-$jobId") { AnalysisViewModel(jobId, mealAnalysisRepository) },
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            AnalysisEffect.NavigateToDashboard -> onNavigateToDashboard()
            is AnalysisEffect.NavigateToReview -> onNavigateToReview(effect.jobId, effect.manualEntry)
        }
    }
    AnalysisScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun AnalysisScreen(state: AnalysisUiState, onEvent: (AnalysisEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val job = state.job
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        VfTopBar(
            navigation = { VfCloseButton(onClick = { onEvent(AnalysisEvent.Close) }) },
            title = {
                if (job != null) {
                    VfPill(
                        text = "${job.mealType.label} · ${VnFormat.time(job.capturedAt.time)}",
                        background = colors.coralContainer,
                        contentColor = colors.ink,
                        bordered = true,
                        style = VisionFitTheme.type.labelL,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            },
        )
        when {
            state.isLoading -> VfFullScreenLoading(Modifier.weight(1f))
            state.isMissing || job == null -> VfErrorState(
                title = "Không tìm thấy bữa ăn này",
                message = "Có thể ảnh đã bị xóa. Quay về trang chính để xem nhật ký nhé.",
                onRetry = { onEvent(AnalysisEvent.BackToDashboard) },
                modifier = Modifier.weight(1f),
            )
            LocalWindowLayout.current.usesTwoPanes -> AnalysisTwoPanes(state, job, onEvent, Modifier.weight(1f))
            else -> AnalysisSingleColumn(state, job, onEvent, Modifier.weight(1f))
        }
    }
}

/** Phones and portrait tablets: photo, progress and steps scroll; the actions stay at the bottom. */
@Composable
private fun AnalysisSingleColumn(state: AnalysisUiState, job: AnalysisJob, onEvent: (AnalysisEvent) -> Unit, modifier: Modifier) {
    val gutter = LocalWindowLayout.current.gutter
    Column(modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .maxContentWidth(VfContentWidth.Column)
                .padding(start = gutter, end = gutter, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 350 × 300 on the design phone; keeps that shape up to tablet width.
            AnalysisPhoto(job = job, modifier = Modifier.proportionalHeight(ratio = 350f / 300f, minHeight = 240.dp, maxHeight = 460.dp))
            StageHeadline(state, job)
            AnalysisStepsCard(steps = state.steps, photoSizeBytes = job.photoSizeBytes, failure = job.failure)
        }
        AnalysisActions(state, onEvent, Modifier.maxContentWidth(VfContentWidth.Column).padding(horizontal = gutter))
    }
}

/** Landscape and wide windows: the photo fills the left half, progress and actions the right. */
@Composable
private fun AnalysisTwoPanes(state: AnalysisUiState, job: AnalysisJob, onEvent: (AnalysisEvent) -> Unit, modifier: Modifier) {
    val gutter = LocalWindowLayout.current.gutter
    Row(
        modifier = modifier.maxContentWidth(VfContentWidth.TwoPane).padding(horizontal = gutter),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AnalysisPhoto(
            job = job,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 12.dp, bottom = 22.dp),
        )
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    // Room for the hard shadow of the steps card.
                    .padding(top = 12.dp, end = VfDimens.ShadowM),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StageHeadline(state, job)
                AnalysisStepsCard(steps = state.steps, photoSizeBytes = job.photoSizeBytes, failure = job.failure)
            }
            AnalysisActions(state, onEvent)
        }
    }
}

@Composable
private fun StageHeadline(state: AnalysisUiState, job: AnalysisJob) {
    val count = job.detections.size
    val (headline, subtitle) = when (job.stage) {
        AnalysisStage.RECOGNIZING -> "AI đang “nếm thử”" to "Thường mất 5–10 giây. Cứ đi làm việc khác, xong mình báo ngay!"
        AnalysisStage.CALCULATING -> "Thấy $count món rồi" to "Đang cân đo calo và macros cho từng món."
        AnalysisStage.COMPLETED -> "Xong rồi nè!" to
            "$count món · khoảng ${VnFormat.thousands(job.totalKcal)} kcal. Kiểm tra lại một chút trước khi lưu nhé."
        AnalysisStage.FAILED -> "Ơ, có gì đó sai sai" to when (job.failure) {
            AnalysisFailure.NO_FOOD_DETECTED -> "AI không tìm thấy món ăn nào. Thử chụp lại rõ hơn hoặc tự nhập món nha."
            else -> "Ảnh vẫn được lưu an toàn. Thử phân tích lại hoặc tự nhập món nha."
        }
    }
    Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics { heading() }) {
            Text(text = headline, style = VisionFitTheme.type.headlineM)
            if (state.isWorking) WorkingDots()
        }
        Text(
            text = subtitle,
            style = VisionFitTheme.type.body,
            color = VisionFitTheme.colors.textSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AnalysisActions(state: AnalysisUiState, onEvent: (AnalysisEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = if (LocalWindowLayout.current.isShort) 12.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state.stage) {
            AnalysisStage.COMPLETED -> {
                VfPrimaryButton(
                    text = "Xem & xác nhận kết quả",
                    onClick = { onEvent(AnalysisEvent.ReviewResult) },
                    height = 58.dp,
                    modifier = Modifier.popIn(durationMillis = 500),
                )
                VfSecondaryLink(text = "Để sau", onClick = { onEvent(AnalysisEvent.Later) })
            }
            AnalysisStage.FAILED -> {
                VfPrimaryButton(
                    text = "Thử phân tích lại",
                    onClick = { onEvent(AnalysisEvent.Retry) },
                    containerColor = colors.primary,
                    contentColor = colors.surface,
                    leadingIcon = VfIcons.Retry,
                    trailingIcon = null,
                    iconTint = colors.yellow,
                    loading = state.isRetrying,
                )
                VfSecondaryLink(text = "Tự nhập món ăn", onClick = { onEvent(AnalysisEvent.ManualEntry) })
            }
            else -> {
                VfPrimaryButton(
                    text = "Về Dashboard, xong mình báo!",
                    onClick = { onEvent(AnalysisEvent.BackToDashboard) },
                    containerColor = colors.surface,
                    trailingIcon = null,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PulseDot()
                    Text(text = "Tự động kiểm tra mỗi 3 giây", style = VisionFitTheme.type.caption, color = colors.textSecondary)
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AnalysisRecognizingPreview() {
    VisionFitPreview { AnalysisScreen(state = PreviewFixtures.analysis(AnalysisStage.RECOGNIZING), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AnalysisCompletedPreview() {
    VisionFitPreview { AnalysisScreen(state = PreviewFixtures.analysis(AnalysisStage.COMPLETED), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AnalysisFailedPreview() {
    VisionFitPreview { AnalysisScreen(state = PreviewFixtures.analysis(AnalysisStage.FAILED), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AnalysisMissingPreview() {
    VisionFitPreview { AnalysisScreen(state = AnalysisUiState(isLoading = false, isMissing = true), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 400)
@Composable
private fun AnalysisLoadingPreview() {
    VisionFitPreview { Box(Modifier.fillMaxSize()) { AnalysisScreen(state = AnalysisUiState(), onEvent = {}) } }
}

@Preview(widthDp = 844, heightDp = 390)
@Composable
private fun AnalysisLandscapePreview() {
    VisionFitPreview { AnalysisScreen(state = PreviewFixtures.analysis(AnalysisStage.COMPLETED), onEvent = {}) }
}
