package com.visionfit.presentation.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/** Rounded label. [bordered] adds the 2dp ink outline used on colored backgrounds. */
@Composable
fun VfPill(
    text: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
    style: TextStyle = VisionFitTheme.type.chip,
    contentPadding: PaddingValues = PaddingValues(horizontal = 7.dp, vertical = 1.dp),
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(if (bordered) Modifier.border(VfDimens.Border, VisionFitTheme.colors.ink, shape) else Modifier)
            .padding(contentPadding),
    ) {
        Text(text = text, style = style, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

enum class Macro { PROTEIN, CARBS, FAT }

data class MacroPalette(val fill: Color, val container: Color, val text: Color, val track: Color)

@Composable
fun macroPalette(macro: Macro): MacroPalette {
    val c = VisionFitTheme.colors
    return when (macro) {
        Macro.PROTEIN -> MacroPalette(fill = c.violet, container = c.primaryContainer, text = c.primaryText, track = c.violetTrack)
        Macro.CARBS -> MacroPalette(fill = c.yellow, container = c.yellowContainer, text = c.yellowText, track = c.yellowTrack)
        Macro.FAT -> MacroPalette(fill = c.sky, container = c.skyPale, text = c.skyText, track = c.skyTrack)
    }
}

/** "P 4g · C 62g · F 14g". Values are pre-formatted so callers pick the precision. */
@Composable
fun MacroPills(
    protein: String,
    carbs: String,
    fat: String,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
    /** Macros above their daily target get a coral "vượt!" pill. */
    over: Set<Macro> = emptySet(),
    style: TextStyle = VisionFitTheme.type.chip,
    spacing: Dp = 4.dp,
) {
    val padding = if (bordered) PaddingValues(horizontal = 7.dp, vertical = 0.dp) else PaddingValues(horizontal = 7.dp, vertical = 1.dp)
    // Wraps onto a second line instead of truncating when space is tight.
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        listOf(Macro.PROTEIN to "P $protein", Macro.CARBS to "C $carbs", Macro.FAT to "F $fat").forEach { (macro, label) ->
            val palette = macroPalette(macro)
            val isOver = macro in over
            VfPill(
                text = if (isOver) "$label · vượt!" else label,
                background = when {
                    isOver -> VisionFitTheme.colors.coral
                    bordered -> VisionFitTheme.colors.surface
                    else -> palette.container
                },
                contentColor = if (isOver) VisionFitTheme.colors.ink else palette.text,
                bordered = bordered,
                style = style,
                contentPadding = padding,
            )
        }
    }
}

/** Yellow tick that pops in on the corner of a selected option. */
@Composable
fun CheckBadge(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(400, easing = OvershootEasing)) }
    Box(
        modifier = modifier
            .graphicsLayer {
                val p = progress.value
                scaleX = p
                scaleY = p
                rotationZ = -40f * (1f - p)
            }
            .size(size)
            .clip(CircleShape)
            .background(VisionFitTheme.colors.yellow)
            .border(VfDimens.Border, VisionFitTheme.colors.ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        VfIcon(VfIcons.Check, contentDescription = null, size = size / 2, tint = VisionFitTheme.colors.ink)
    }
}

/** Small numbered disc that labels a dish on the photo and in the edit list. */
@Composable
fun DishNumberBadge(
    number: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    shapeRadius: Dp? = null,
    style: TextStyle = VisionFitTheme.type.labelL,
) {
    val shape = shapeRadius?.let { RoundedCornerShape(it) } ?: CircleShape
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color)
            .border(VfDimens.Border, VisionFitTheme.colors.ink, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = number.toString(), style = style, color = VisionFitTheme.colors.ink)
    }
}
