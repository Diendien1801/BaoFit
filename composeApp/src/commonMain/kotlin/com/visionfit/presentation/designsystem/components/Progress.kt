package com.visionfit.presentation.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Animates from 0 to [target] once, when first shown and whenever [target] changes —
 * the "count up" used by rings, bars and big numbers.
 */
@Composable
fun animatedProgress(target: Float, durationMillis: Int = 1_400, delayMillis: Int = 200): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target) {
        delay(delayMillis.toLong())
        progress.animateTo(target, tween(durationMillis, easing = SettleEasing))
    }
    return progress.value
}

/**
 * Circular progress drawn like the SVG rings in the design: a track, an optional wider
 * ink arc under the progress arc (outline effect) and butt caps starting at 12 o'clock.
 */
@Composable
fun ProgressRing(
    progress: Float,
    radius: Dp,
    strokeWidth: Dp,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
    trackWidth: Dp = strokeWidth,
    outlineColor: Color? = null,
    outlineWidth: Dp = strokeWidth + 6.dp,
) {
    Canvas(modifier = modifier) {
        val r = radius.toPx()
        val center = Offset(size.width / 2, size.height / 2)
        val topLeft = Offset(center.x - r, center.y - r)
        val arcSize = Size(2 * r, 2 * r)
        val sweep = 360f * progress.coerceIn(0f, 1f)
        drawCircle(trackColor, radius = r, center = center, style = Stroke(trackWidth.toPx()))
        if (sweep <= 0f) return@Canvas
        if (outlineColor != null) {
            drawArc(outlineColor, -90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(outlineWidth.toPx(), cap = StrokeCap.Butt))
        }
        drawArc(progressColor, -90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Butt))
    }
}

/**
 * Diagonal "barber pole" stripes over [base] (CSS 45° repeating gradient, 28px tile),
 * scrolling sideways when [animated].
 */
fun Modifier.stripes(
    base: Color,
    stripe: Color = Color.White.copy(alpha = 0.35f),
    tile: Dp = 28.dp,
    animated: Boolean = true,
    periodMillis: Int = 800,
): Modifier = composed {
    val tilePx = with(LocalDensity.current) { tile.toPx() }
    val phaseState: State<Float> = if (animated) {
        rememberInfiniteTransition(label = "stripes").animateFloat(
            initialValue = 0f,
            targetValue = tilePx,
            animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
            label = "stripesPhase",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    // The phase is read while drawing, so the scrolling stripes never recompose.
    drawBehind {
        val phase = phaseState.value
        drawRect(base)
        val brush = Brush.linearGradient(
            0f to stripe, 0.25f to stripe,
            0.25f to Color.Transparent, 0.5f to Color.Transparent,
            0.5f to stripe, 0.75f to stripe,
            0.75f to Color.Transparent, 1f to Color.Transparent,
            start = Offset(phase, tilePx),
            end = Offset(phase + tilePx, 0f),
            tileMode = TileMode.Repeated,
        )
        drawRect(brush)
    }
}
