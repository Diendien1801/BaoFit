package com.visionfit.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.visionfit.domain.model.DaySummary
import com.visionfit.domain.model.MacroProgress
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.Macro
import com.visionfit.presentation.designsystem.components.animatedProgress
import com.visionfit.presentation.designsystem.components.macroPalette
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.components.stripes
import com.visionfit.presentation.designsystem.components.topBorder
import com.visionfit.presentation.designsystem.components.wiggle
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import kotlin.math.round
import kotlin.math.roundToInt

/** "Macros hôm nay": three fill-up jars, eaten vs target. Over-target jars turn into a coral stripe. */
@Composable
fun MacroBarsSection(summary: DaySummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = "Macros hôm nay", style = VisionFitTheme.type.sectionTitle, modifier = Modifier.semantics { heading() })
            Text(text = "đã nạp / mục tiêu", style = VisionFitTheme.type.caption, color = VisionFitTheme.colors.textMuted)
        }
        BrutalSurface(
            modifier = Modifier.fillMaxWidth().popIn(delayMillis = 150, durationMillis = 700),
            shape = RoundedCornerShape(VfRadius.CardXL),
            shadowOffset = VfDimens.ShadowM,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MacroJar(Macro.PROTEIN, "Protein", summary.protein, delayMillis = 300, modifier = Modifier.weight(1f))
                MacroJar(Macro.CARBS, "Carbs", summary.carbs, delayMillis = 420, modifier = Modifier.weight(1f))
                MacroJar(Macro.FAT, "Fat", summary.fat, delayMillis = 540, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroJar(
    macro: Macro,
    label: String,
    progress: MacroProgress,
    delayMillis: Int,
    modifier: Modifier = Modifier,
) {
    val colors = VisionFitTheme.colors
    val palette = macroPalette(macro)
    val percent = round(progress.fraction * 100).toInt()
    val eaten = progress.eatenG.roundToInt()
    val rise = animatedProgress(target = 1f, durationMillis = 1_000, delayMillis = delayMillis)
    val fill = progress.fraction.toFloat().coerceIn(0f, 1f)
    val jarShape = RoundedCornerShape(VfRadius.Card)

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "$label: $eaten trên ${progress.targetG} gam, $percent phần trăm" +
                        if (progress.isOver) ", vượt mục tiêu" else ""
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(jarShape)
                    .background(colors.background)
                    .border(VfDimens.Border, colors.ink, jarShape),
            ) {
                if (fill > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(fill)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                scaleY = rise
                            }
                            .then(
                                if (progress.isOver) {
                                    Modifier.stripes(base = colors.coral, stripe = colors.surface.copy(alpha = 0.22f), periodMillis = 900)
                                } else {
                                    Modifier.background(palette.fill)
                                },
                            )
                            .then(if (fill < 1f) Modifier.topBorder(VfDimens.Border, colors.ink) else Modifier),
                    )
                }
                Text(
                    text = "$percent%",
                    style = VisionFitTheme.type.numberM,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, style = VisionFitTheme.type.bodyStrong)
                Text(
                    text = "$eaten / ${progress.targetG} g",
                    style = VisionFitTheme.type.caption.copy(fontWeight = if (progress.isOver) FontWeight.Bold else FontWeight.Normal),
                    color = if (progress.isOver) colors.error else colors.textSecondary,
                )
            }
        }
        if (progress.isOver) {
            OverSticker(
                overByG = progress.overByG.roundToInt().coerceAtLeast(1),
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-12).dp),
            )
        }
    }
}

@Composable
private fun OverSticker(overByG: Int, modifier: Modifier = Modifier) {
    BrutalSurface(
        modifier = modifier.wiggle(fromDegrees = -6f, toDegrees = 5f, periodMillis = 1_000),
        shape = RoundedCornerShape(VfRadius.S),
        shadowOffset = VfDimens.ShadowXs,
    ) {
        Text(
            text = "Vượt ${overByG}g!",
            style = VisionFitTheme.type.labelS,
            color = VisionFitTheme.colors.errorStrong,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
        )
    }
}
