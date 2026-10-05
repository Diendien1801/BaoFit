package com.visionfit.presentation.designsystem.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws an [IconSpec]. Unlike `Icon`, this keeps multi-color icons intact: [tint] strokes the
 * outline, [fill] fills body paths and [accent] fills accent paths (e.g. the camera lens).
 */
@Composable
fun VfIcon(
    icon: IconSpec,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current,
    fill: Color = Color.Transparent,
    accent: Color = fill,
    strokeWidth: Float = icon.strokeWidth,
) {
    val vector = remember(icon, tint, fill, accent, strokeWidth) {
        icon.toImageVector(tint, fill, accent, strokeWidth)
    }
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}

private fun IconSpec.toImageVector(tint: Color, fill: Color, accent: Color, strokeWidth: Float): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    paths.forEach { path ->
        val fillColor = when (path.fill) {
            FillRole.None -> null
            FillRole.Body -> fill
            FillRole.Accent -> accent
        }
        builder.addPath(
            pathData = addPathNodes(path.data),
            fill = fillColor?.takeIf { it.alpha > 0f }?.let(::SolidColor),
            stroke = SolidColor(tint),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}
