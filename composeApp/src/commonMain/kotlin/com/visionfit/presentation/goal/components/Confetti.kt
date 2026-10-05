package com.visionfit.presentation.goal.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import kotlinx.coroutines.delay

private data class Piece(
    val x: Float,
    val width: Dp,
    val height: Dp,
    val color: Color,
    val round: Boolean,
    val durationMillis: Int,
    val delayMillis: Int,
)

/** A short celebratory confetti burst: 12 pieces fall three times each, then stop. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier) {
    val c = VisionFitTheme.colors
    val pieces = remember(c) {
        listOf(
            Piece(24f, 10.dp, 16.dp, c.coral, false, 2_600, 100),
            Piece(62f, 12.dp, 12.dp, c.yellow, true, 3_100, 500),
            Piece(96f, 8.dp, 18.dp, c.mint, false, 2_400, 900),
            Piece(134f, 14.dp, 9.dp, c.sky, false, 2_900, 200),
            Piece(170f, 10.dp, 10.dp, c.pink, true, 2_700, 1_100),
            Piece(204f, 9.dp, 17.dp, c.yellow, false, 3_300, 350),
            Piece(238f, 13.dp, 13.dp, c.violet, false, 2_500, 750),
            Piece(272f, 10.dp, 16.dp, c.mint, false, 3_000, 1_300),
            Piece(306f, 12.dp, 12.dp, c.coral, true, 2_800, 50),
            Piece(340f, 9.dp, 15.dp, c.sky, false, 2_600, 600),
            Piece(362f, 11.dp, 11.dp, c.yellow, true, 3_200, 1_000),
            Piece(8f, 12.dp, 8.dp, c.violet, false, 3_000, 1_500),
        )
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(420.dp)) {
        val widthScale = maxWidth.value / DESIGN_WIDTH
        pieces.forEach { piece ->
            FallingPiece(piece, Modifier.offset(x = (piece.x * widthScale).dp))
        }
    }
}

@Composable
private fun FallingPiece(piece: Piece, modifier: Modifier) {
    val progress = remember { Animatable(0f) }
    val fallPx = with(LocalDensity.current) { 380.dp.toPx() }
    val risePx = with(LocalDensity.current) { 40.dp.toPx() }
    LaunchedEffect(piece) {
        delay(piece.delayMillis.toLong())
        repeat(3) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(piece.durationMillis, easing = EaseIn))
        }
    }
    val shape = if (piece.round) CircleShape else RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                val p = progress.value
                translationY = -risePx + p * fallPx
                rotationZ = 560f * p
                alpha = when {
                    p == 0f -> 0f
                    p < 0.1f -> p / 0.1f
                    p > 0.85f -> ((1f - p) / 0.15f).coerceIn(0f, 1f)
                    else -> 1f
                }
            }
            .size(width = piece.width, height = piece.height)
            .clip(shape)
            .background(piece.color)
            .border(1.5.dp, VisionFitTheme.colors.ink, shape),
    )
}

private const val DESIGN_WIDTH = 390f

/** CSS `ease-in`. */
private val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
