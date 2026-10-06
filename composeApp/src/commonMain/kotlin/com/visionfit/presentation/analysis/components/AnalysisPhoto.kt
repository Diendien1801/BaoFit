package com.visionfit.presentation.analysis.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.visionfit.core.format.VnFormat
import com.visionfit.domain.model.AnalysisFailure
import com.visionfit.domain.model.AnalysisJob
import com.visionfit.domain.model.AnalysisStage
import com.visionfit.domain.model.DetectionRegion
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.DishNumberBadge
import com.visionfit.presentation.designsystem.components.MealPhotoImage
import com.visionfit.presentation.designsystem.components.OvershootEasing
import com.visionfit.presentation.designsystem.components.PhotoCropMapping
import com.visionfit.presentation.designsystem.components.PhotoFocus
import com.visionfit.presentation.designsystem.components.hardShadow
import com.visionfit.presentation.designsystem.components.rememberMealPhotoPainter
import com.visionfit.presentation.designsystem.components.spinning
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/** Same crop as the design: centered horizontally, 38 % from the top. */
val AnalysisPhotoFocus = PhotoFocus(x = 0.5f, y = 0.38f)

/** Decorative "searching" circles shown before the AI has located any dish. */
private val ScanningRegions = listOf(
    DetectionRegion(0.688f, 0.505f, 0.162f),
    DetectionRegion(0.376f, 0.448f, 0.147f),
    DetectionRegion(0.312f, 0.693f, 0.176f),
    DetectionRegion(0.821f, 0.229f, 0.153f),
)

/**
 * The analyzed photo with a stage-specific overlay: scanning circles and a sweeping bar while
 * recognizing, numbered rings around each dish once found, and a warning when it failed.
 * The caller sizes it; overlays follow the crop at any size.
 */
@Composable
fun AnalysisPhoto(job: AnalysisJob, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val shape = RoundedCornerShape(VfRadius.Hero)
    val shake = remember { Animatable(0f) }
    val shakePx = with(LocalDensity.current) { 8.dp.toPx() }
    LaunchedEffect(job.stage) {
        if (job.stage == AnalysisStage.FAILED) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 500
                    0f at 0
                    -1f at 100
                    0.875f at 200
                    -0.625f at 300
                    0.375f at 400
                    0f at 500
                },
            )
        }
    }
    Box(
        modifier = modifier
            .graphicsLayer { translationX = shake.value * shakePx }
            .hardShadow(shape, VfDimens.ShadowXL, colors.primary)
            .clip(shape)
            .background(colors.inkSoft)
            .border(VfDimens.Border, colors.ink, shape),
    ) {
        MealPhotoImage(
            photo = job.photo,
            contentDescription = "Ảnh bữa ăn vừa chụp",
            focus = AnalysisPhotoFocus,
            modifier = Modifier.fillMaxSize(),
        )
        when (job.stage) {
            AnalysisStage.RECOGNIZING -> ScanningOverlay(job)
            AnalysisStage.CALCULATING, AnalysisStage.COMPLETED -> DetectionOverlay(job)
            AnalysisStage.FAILED -> FailureOverlay(job.failure)
        }
        StageChip(job, Modifier.padding(12.dp))
    }
}

@Composable
private fun ScanningOverlay(job: AnalysisJob) {
    val colors = VisionFitTheme.colors
    val painter = rememberMealPhotoPainter(job.photo)
    val transition = rememberInfiniteTransition(label = "scanning")
    val spin = transition.animateFloat(0f, 360f, infiniteRepeatable(tween(7_000, easing = LinearEasing)), label = "scanSpin")
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scanSweep",
    )
    Box(Modifier.fillMaxSize().background(colors.ink.copy(alpha = 0.22f)))
    Canvas(Modifier.fillMaxSize()) {
        val image = painter?.intrinsicSize?.takeIf { it.width > 0f } ?: size
        val mapping = PhotoCropMapping(size, image, AnalysisPhotoFocus)
        val stroke = 3.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()))
        ScanningRegions.forEachIndexed { index, region ->
            val center = mapping.center(region)
            val radius = mapping.radius(region) - stroke / 2
            val direction = if (index % 2 == 0) 1f else -1f
            rotate(spin.value * direction * (1f - index * 0.08f), pivot = center) {
                drawCircle(
                    color = if (index % 2 == 0) colors.yellow else colors.surface,
                    radius = radius,
                    center = center,
                    style = Stroke(width = stroke, pathEffect = dash),
                )
            }
        }
        val barHeight = 8.dp.toPx()
        val top = 4.dp.toPx() + sweep.value * (size.height - barHeight - 10.dp.toPx())
        drawRect(colors.ink, topLeft = Offset(0f, top - 2.dp.toPx()), size = Size(size.width, barHeight + 4.dp.toPx()))
        drawRect(colors.yellow, topLeft = Offset(0f, top), size = Size(size.width, barHeight))
    }
}

@Composable
private fun DetectionOverlay(job: AnalysisJob) {
    val colors = VisionFitTheme.colors
    val painter = rememberMealPhotoPainter(job.photo)
    val regions = job.detections.mapNotNull { it.region }
    val reveal = remember(regions.size) { List(regions.size) { Animatable(0f) } }
    LaunchedEffect(reveal) {
        reveal.forEachIndexed { index, animatable ->
            delay(if (index == 0) 0L else 120L)
            animatable.animateTo(1f, tween(500, easing = OvershootEasing))
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val container = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val image = painter?.intrinsicSize?.takeIf { it.width > 0f } ?: container
        val mapping = PhotoCropMapping(container, image, AnalysisPhotoFocus)
        Canvas(Modifier.fillMaxSize()) {
            regions.forEachIndexed { index, region ->
                val p = reveal.getOrNull(index)?.value ?: 1f
                if (p <= 0f) return@forEachIndexed
                val center = mapping.center(region)
                val outer = mapping.radius(region) * (0.7f + 0.3f * p)
                val color = colors.dishMarkers[index % colors.dishMarkers.size].copy(alpha = p.coerceIn(0f, 1f))
                val ink = colors.ink.copy(alpha = p.coerceIn(0f, 1f))
                val mid = outer - 2.dp.toPx()
                drawCircle(ink, radius = mid, center = center, style = Stroke(width = 8.dp.toPx()))
                drawCircle(color, radius = mid, center = center, style = Stroke(width = 4.dp.toPx()))
            }
        }
        val badgeHalf = with(density) { 14.dp.toPx() }
        regions.forEachIndexed { index, region ->
            val center = mapping.center(region)
            val offset = mapping.radius(region) / sqrt(2f)
            val top = center.y - offset
            // Top-left of the ring, or bottom-left when the ring is cut off by the photo's top edge.
            val anchor = if (top - badgeHalf < 0f) Offset(center.x - offset, center.y + offset) else Offset(center.x - offset, top)
            val p = reveal.getOrNull(index)?.value ?: 1f
            DishNumberBadge(
                number = index + 1,
                color = colors.dishMarkers[index % colors.dishMarkers.size],
                modifier = Modifier
                    .offset(
                        x = with(density) { (anchor.x - badgeHalf).toDp() },
                        y = with(density) { (anchor.y - badgeHalf).toDp() },
                    )
                    .graphicsLayer {
                        scaleX = p
                        scaleY = p
                        rotationZ = -30f * (1f - p)
                    },
            )
        }
    }
}

@Composable
private fun FailureOverlay(failure: AnalysisFailure?) {
    val colors = VisionFitTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().background(colors.ink.copy(alpha = 0.62f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        BrutalSurface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = colors.coral,
            shadowOffset = VfDimens.ShadowS,
            contentAlignment = Alignment.Center,
        ) {
            VfIcon(VfIcons.Alert, contentDescription = null, size = 28.dp, tint = colors.ink)
        }
        Text(
            text = if (failure == AnalysisFailure.NO_FOOD_DETECTED) "Không thấy món nào trong ảnh!" else "Ối, AI trả kết quả lạ quá!",
            style = VisionFitTheme.type.cardTitle,
            color = colors.surface,
        )
    }
}

@Composable
private fun StageChip(job: AnalysisJob, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val working = job.stage == AnalysisStage.RECOGNIZING || job.stage == AnalysisStage.CALCULATING
    val count = job.detections.size
    val text = when (job.stage) {
        AnalysisStage.RECOGNIZING -> "Đang nhận diện"
        AnalysisStage.CALCULATING -> "Đã thấy $count món"
        AnalysisStage.COMPLETED -> "$count món · ${VnFormat.thousands(job.totalKcal)} kcal"
        AnalysisStage.FAILED -> "Phân tích thất bại"
    }
    BrutalSurface(modifier = modifier, shape = CircleShape, shadowOffset = VfDimens.ShadowXs) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colors.yellow)
                    .border(VfDimens.Border, colors.ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                VfIcon(
                    VfIcons.Sparkle,
                    contentDescription = null,
                    size = 12.dp,
                    tint = colors.ink,
                    fill = colors.coral,
                    modifier = Modifier.spinning(1_600, enabled = working),
                )
            }
            Text(text = text, style = VisionFitTheme.type.labelM)
        }
    }
}
