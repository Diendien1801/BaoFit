package com.visionfit.presentation.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** cubic-bezier(.34, 1.56, .64, 1): the springy overshoot used across the design. */
val OvershootEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** cubic-bezier(.22, 1, .36, 1): fast start, long settle (rings and progress bars). */
val SettleEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** Entrance: fades in while rising 18dp and scaling from 94 %, with a small overshoot. */
fun Modifier.popIn(delayMillis: Int = 0, durationMillis: Int = 550, rise: Dp = 18.dp): Modifier = composed {
    val progress = remember { Animatable(0f) }
    val risePx = with(LocalDensity.current) { rise.toPx() }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        progress.animateTo(1f, tween(durationMillis, easing = OvershootEasing))
    }
    graphicsLayer {
        val p = progress.value
        alpha = (p * 1.7f).coerceIn(0f, 1f)
        translationY = (1f - p) * risePx
        val scale = 0.94f + 0.06f * p
        scaleX = scale
        scaleY = scale
    }
}

/** Gentle vertical bob, used on stickers. */
fun Modifier.floating(amplitude: Dp = 7.dp, periodMillis: Int = 3_000, delayMillis: Int = 0): Modifier = composed {
    val amplitudePx = with(LocalDensity.current) { amplitude.toPx() }
    val transition = rememberInfiniteTransition(label = "floating")
    val offset = transition.animateFloat(
        initialValue = 0f,
        targetValue = -amplitudePx,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis),
        ),
        label = "floatingOffset",
    )
    graphicsLayer { translationY = offset.value }
}

/** Rocks between two angles (wiggle / tilt keyframes). */
fun Modifier.wiggle(fromDegrees: Float, toDegrees: Float, periodMillis: Int): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "wiggle")
    val angle = transition.animateFloat(
        initialValue = fromDegrees,
        targetValue = toDegrees,
        animationSpec = infiniteRepeatable(tween(periodMillis / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wiggleAngle",
    )
    graphicsLayer { rotationZ = angle.value }
}

fun Modifier.spinning(periodMillis: Int, enabled: Boolean = true): Modifier =
    if (!enabled) this else composed {
        val transition = rememberInfiniteTransition(label = "spin")
        val angle = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
            label = "spinAngle",
        )
        graphicsLayer { rotationZ = angle.value }
    }

/** Hop up and settle, e.g. the notification counter. Runs [iterations] times (forever by default). */
fun Modifier.bouncing(
    periodMillis: Int = 1_600,
    height: Dp = 8.dp,
    delayMillis: Int = 0,
    iterations: Int = Int.MAX_VALUE,
): Modifier = composed {
    val heightPx = with(LocalDensity.current) { height.toPx() }
    val offset = remember { Animatable(0f) }
    LaunchedEffect(periodMillis, heightPx, iterations) {
        delay(delayMillis.toLong())
        repeat(iterations) {
            offset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = periodMillis
                    0f at 0
                    -heightPx at (periodMillis * 0.4f).toInt()
                    -heightPx * 0.375f at (periodMillis * 0.6f).toInt()
                    0f at periodMillis
                },
            )
        }
    }
    graphicsLayer { translationY = offset.value }
}

/** Expanding, fading halo behind a call-to-action. */
fun Modifier.pinging(periodMillis: Int = 1_800, maxScale: Float = 1.65f, startAlpha: Float = 0.6f): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "ping")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))),
        label = "pingProgress",
    )
    graphicsLayer {
        val p = progress.value
        val scale = 1f + (maxScale - 1f) * p
        scaleX = scale
        scaleY = scale
        alpha = startAlpha * (1f - p)
    }
}

/** Scales from a corner and back (camera frame corners). */
fun Modifier.breathing(origin: TransformOrigin, periodMillis: Int = 1_600, minScale: Float = 0.92f): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale = transition.animateFloat(
        initialValue = 1f,
        targetValue = minScale,
        animationSpec = infiniteRepeatable(tween(periodMillis / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathingScale",
    )
    graphicsLayer {
        transformOrigin = origin
        scaleX = scale.value
        scaleY = scale.value
    }
}
