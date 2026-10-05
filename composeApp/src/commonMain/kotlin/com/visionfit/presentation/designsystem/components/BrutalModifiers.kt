package com.visionfit.presentation.designsystem.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

/**
 * The signature "hard" shadow: a solid copy of [shape] offset down-right, no blur.
 * [offsetPx] is read at draw time so press animations do not recompose.
 */
fun Modifier.hardShadow(shape: Shape, color: Color, offsetPx: () -> Float): Modifier = drawBehind {
    val offset = offsetPx()
    if (offset <= 0f || color.alpha == 0f) return@drawBehind
    val outline = shape.createOutline(size, layoutDirection, this)
    translate(offset, offset) { drawOutline(outline, color) }
}

fun Modifier.hardShadow(shape: Shape, offset: Dp, color: Color): Modifier =
    if (offset.value <= 0f) this else drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        val px = offset.toPx()
        translate(px, px) { drawOutline(outline, color) }
    }

/** CSS-like `border: 2px dashed`, drawn inside the bounds on top of the content. */
fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    shape: Shape,
    dash: Dp,
    gap: Dp,
): Modifier = drawWithContent {
    drawContent()
    val stroke = width.toPx()
    val inset = Size(size.width - stroke, size.height - stroke)
    val outline = shape.createOutline(inset, layoutDirection, this)
    translate(stroke / 2, stroke / 2) {
        drawOutline(
            outline = outline,
            color = color,
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
            ),
        )
    }
}

/**
 * Only the top edge of a sheet with rounded top corners, like CSS
 * `border-top: 2px; border-radius: 32px 32px 0 0`.
 */
fun Modifier.topRoundedBorder(width: Dp, color: Color, radius: Dp): Modifier = drawWithContent {
    drawContent()
    val stroke = width.toPx()
    val r = radius.toPx()
    val half = stroke / 2
    val path = Path().apply {
        moveTo(half, r)
        arcTo(Rect(half, half, 2 * r - half, 2 * r - half), 180f, 90f, forceMoveTo = false)
        lineTo(size.width - r, half)
        arcTo(Rect(size.width - 2 * r + half, half, size.width - half, 2 * r - half), 270f, 90f, forceMoveTo = false)
    }
    drawPath(path, color, style = Stroke(width = stroke))
}

/** A 2dp line along the top edge (fixed footers). */
fun Modifier.topBorder(width: Dp, color: Color): Modifier = drawWithContent {
    drawContent()
    val stroke = width.toPx()
    drawRect(color, size = Size(size.width, stroke))
}
