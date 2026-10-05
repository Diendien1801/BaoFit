package com.visionfit.presentation.analysis.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.visionfit.core.format.VnFormat
import com.visionfit.domain.model.AnalysisFailure
import com.visionfit.presentation.analysis.AnalysisStep
import com.visionfit.presentation.analysis.StepKind
import com.visionfit.presentation.analysis.StepStatus
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.dashedBorder
import com.visionfit.presentation.designsystem.components.spinning
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/** The pipeline as a checklist: upload → queue → recognize → calculate. */
@Composable
fun AnalysisStepsCard(
    steps: List<AnalysisStep>,
    photoSizeBytes: Long,
    failure: AnalysisFailure?,
    modifier: Modifier = Modifier,
) {
    BrutalSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowOffset = VfDimens.ShadowM,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            steps.forEachIndexed { index, step ->
                StepRow(step, photoSizeBytes, failure, showDivider = index < steps.lastIndex)
            }
        }
    }
}

@Composable
private fun StepRow(step: AnalysisStep, photoSizeBytes: Long, failure: AnalysisFailure?, showDivider: Boolean) {
    val colors = VisionFitTheme.colors
    val title = step.kind.title
    val subtitle = step.subtitle(photoSizeBytes, failure)
    val dividerColor = colors.onPrimaryMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .drawBehind {
                if (showDivider) {
                    val y = size.height - 1.dp.toPx()
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    )
                }
            }
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title: ${step.status.spoken}. $subtitle" },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { StatusTile(step.status) }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = VisionFitTheme.type.bodyStrong,
                color = when (step.status) {
                    StepStatus.PENDING -> colors.textMuted
                    StepStatus.FAILED -> colors.error
                    else -> colors.ink
                },
            )
            Text(
                text = subtitle,
                style = VisionFitTheme.type.caption.copy(fontWeight = FontWeight.Normal),
                color = if (step.status == StepStatus.FAILED) colors.error else colors.textMuted,
            )
        }
        step.duration?.let {
            Text(text = VnFormat.seconds(it), style = VisionFitTheme.type.labelS.copy(fontWeight = FontWeight.Bold), color = colors.textSecondary)
        }
    }
}

@Composable
private fun StatusTile(status: StepStatus) {
    val colors = VisionFitTheme.colors
    val shape = RoundedCornerShape(VfRadius.Xs)
    val tile = Modifier.size(30.dp).clip(shape)
    when (status) {
        StepStatus.DONE -> Box(tile.background(colors.mint).border(VfDimens.Border, colors.ink, shape), contentAlignment = Alignment.Center) {
            VfIcon(VfIcons.Check, contentDescription = null, size = 14.dp, tint = colors.ink)
        }
        StepStatus.ACTIVE -> Box(tile.background(colors.yellow).border(VfDimens.Border, colors.ink, shape), contentAlignment = Alignment.Center) {
            VfIcon(
                VfIcons.Sparkle,
                contentDescription = null,
                size = 16.dp,
                tint = colors.ink,
                fill = colors.surface,
                modifier = Modifier.spinning(1_400),
            )
        }
        StepStatus.PENDING -> Box(
            tile.background(colors.background).dashedBorder(VfDimens.Border, colors.outlineMuted, shape, dash = 4.dp, gap = 3.dp),
        )
        StepStatus.FAILED -> Box(tile.background(colors.coral).border(VfDimens.Border, colors.ink, shape), contentAlignment = Alignment.Center) {
            VfIcon(VfIcons.Close, contentDescription = null, size = 14.dp, tint = colors.ink, strokeWidth = 3.4f)
        }
    }
}

private val StepKind.title: String
    get() = when (this) {
        StepKind.UPLOAD -> "Tải ảnh lên"
        StepKind.QUEUE -> "Xếp hàng chờ phân tích"
        StepKind.RECOGNIZE -> "Nhận diện món ăn"
        StepKind.CALCULATE -> "Tính calo & macros"
    }

private fun AnalysisStep.subtitle(photoSizeBytes: Long, failure: AnalysisFailure?): String = when (kind) {
    StepKind.UPLOAD -> "${VnFormat.megabytes(photoSizeBytes)} · đã nén trên máy"
    StepKind.QUEUE -> "Bạn có thể rời màn hình này"
    StepKind.RECOGNIZE -> when {
        status != StepStatus.FAILED -> "AI Vision đang soi từng đĩa"
        failure == AnalysisFailure.NO_FOOD_DETECTED -> "Không tìm thấy món ăn trong ảnh"
        else -> "Phản hồi từ AI không hợp lệ"
    }
    StepKind.CALCULATE -> "Ước lượng khối lượng từng món"
}

private val StepStatus.spoken: String
    get() = when (this) {
        StepStatus.DONE -> "xong"
        StepStatus.ACTIVE -> "đang chạy"
        StepStatus.PENDING -> "đang chờ"
        StepStatus.FAILED -> "thất bại"
    }

/** "…" whose dots blink one after another while the AI is working. */
@Composable
fun WorkingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row {
        repeat(3) { index ->
            val alpha = transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1_200
                        0.2f at 0
                        0.2f at 240
                        1f at 600
                        0.2f at 1_200
                    },
                    initialStartOffset = StartOffset(index * 200),
                ),
                label = "dot$index",
            )
            Text(text = ".", style = VisionFitTheme.type.headlineM, modifier = Modifier.graphicsLayer { this.alpha = alpha.value })
        }
    }
}

/** Small green dot that pulses next to "auto refresh" copy. */
@Composable
fun PulseDot(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale = transition.animateFloat(1f, 1.25f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pulseScale")
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .size(10.dp)
            .clip(CircleShape)
            .background(VisionFitTheme.colors.mint)
            .border(VfDimens.Border, VisionFitTheme.colors.ink, CircleShape),
    )
}
