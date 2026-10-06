package com.visionfit.presentation.designsystem.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** Upper bounds for content width, so cards and lines stay readable on tablets and desktops. */
object VfContentWidth {
    /** Sign-in sheet and other short forms. */
    val Form = 480.dp

    /** Single-column flows and lists. */
    val Column = 640.dp

    /** Both panes of a two-pane screen together. */
    val TwoPane = 1180.dp
}

/**
 * This width grown with the font size the user picked in system settings. For breakpoints that
 * exist because some text has to fit, so large text switches layouts earlier instead of wrapping.
 */
@Composable
@ReadOnlyComposable
fun Dp.withFontScale(): Dp = this * LocalDensity.current.fontScale.coerceAtLeast(1f)

/** Fills the available width up to [max] and centers the result. */
fun Modifier.maxContentWidth(max: Dp): Modifier =
    fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = max)
        .fillMaxWidth()

/**
 * Side padding that centers a column at most [maxContentWidth] wide inside [containerWidth],
 * never smaller than [minPadding]. For lazy lists, whose scrollable area should stay full width.
 */
fun centeringPadding(containerWidth: Dp, maxContentWidth: Dp, minPadding: Dp): Dp =
    maxOf(minPadding, (containerWidth - maxContentWidth) / 2)

/** These paddings (usually insets from a scaffold) plus extra space on each side. */
@Composable
fun PaddingValues.expandedBy(start: Dp = 0.dp, top: Dp = 0.dp, end: Dp = 0.dp, bottom: Dp = 0.dp): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction) + start,
        top = calculateTopPadding() + top,
        end = calculateEndPadding(direction) + end,
        bottom = calculateBottomPadding() + bottom,
    )
}

/**
 * Full available width and a height that follows it ([ratio] = width / height), kept within
 * [minHeight]..[maxHeight]. Photos keep their shape instead of turning into strips on wide cards.
 */
fun Modifier.proportionalHeight(ratio: Float, minHeight: Dp, maxHeight: Dp): Modifier = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = (width / ratio).roundToInt()
        .coerceIn(minHeight.roundToPx(), maxHeight.roundToPx())
        .coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(width, height) { placeable.place(0, 0) }
}

/**
 * Lays [content] out at its designed size and scales it down uniformly when less room is
 * available. For fixed compositions, such as a sticker collage, that must not reflow.
 *
 * Below [minScale] the content would be too small to read, so it is left out (zero size) and
 * the space goes to its neighbours. Its minimum intrinsic height is then zero, which lets a
 * parent measure how small the surrounding layout can get.
 */
@Composable
fun ScaleDownToFit(
    designWidth: Dp,
    designHeight: Dp,
    modifier: Modifier = Modifier,
    minScale: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    val measurePolicy = remember(designWidth, designHeight, minScale) {
        ScaleDownToFitPolicy(designWidth, designHeight, minScale)
    }
    Layout(
        content = { Box(Modifier.size(designWidth, designHeight), content = content) },
        modifier = modifier,
        measurePolicy = measurePolicy,
    )
}

private class ScaleDownToFitPolicy(
    private val designWidth: Dp,
    private val designHeight: Dp,
    private val minScale: Float,
) : MeasurePolicy {

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val width = designWidth.roundToPx()
        val height = designHeight.roundToPx()
        var scale = 1f
        if (constraints.hasBoundedWidth) scale = min(scale, constraints.maxWidth / width.toFloat())
        if (constraints.hasBoundedHeight) scale = min(scale, constraints.maxHeight / height.toFloat())
        if (scale < minScale) return layout(0, 0) {}
        val placeable = measurables.single().measure(Constraints.fixed(width, height))
        val scaledWidth = (width * scale).roundToInt()
        val scaledHeight = (height * scale).roundToInt()
        return layout(scaledWidth, scaledHeight) {
            // Layers scale around their center, so center the full-size box on the scaled slot.
            placeable.placeWithLayer(x = (scaledWidth - width) / 2, y = (scaledHeight - height) / 2) {
                scaleX = scale
                scaleY = scale
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int {
        val designWidthPx = designWidth.roundToPx()
        val scale = if (width == Constraints.Infinity) 1f else min(1f, width / designWidthPx.toFloat())
        return (designHeight.roundToPx() * scale).roundToInt()
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        if (minScale > 0f) 0 else maxIntrinsicHeight(measurables, width)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        designWidth.roundToPx()

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int = 0
}

/**
 * Equal-width cells in as many columns as fit (each at least [minCellWidth], at most
 * [maxColumns]). Cells in a row share the tallest one's height; a short last row keeps empty
 * slots so the columns stay aligned.
 */
@Composable
fun <T> AdaptiveGrid(
    items: List<T>,
    minCellWidth: Dp,
    modifier: Modifier = Modifier,
    maxColumns: Int = items.size,
    spacing: Dp = 12.dp,
    cell: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = floor((maxWidth + spacing) / (minCellWidth + spacing)).toInt()
            .coerceIn(1, maxColumns.coerceAtLeast(1))
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    row.forEach { item -> cell(item, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
