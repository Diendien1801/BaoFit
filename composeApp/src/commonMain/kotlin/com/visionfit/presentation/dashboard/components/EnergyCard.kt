package com.visionfit.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.visionfit.core.format.VnFormat
import com.visionfit.domain.model.DaySummary
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.ProgressRing
import com.visionfit.presentation.designsystem.components.animatedProgress
import com.visionfit.presentation.designsystem.components.floating
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

/** Purple hero: remaining-calories ring with "eaten" / "target" stickers. */
@Composable
fun EnergyCard(summary: DaySummary, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val type = VisionFitTheme.type
    val countUp = animatedProgress(target = 1f, durationMillis = 1_300, delayMillis = 0)
    val ring = animatedProgress(target = summary.fraction.toFloat().coerceAtMost(1f), durationMillis = 1_400, delayMillis = 200)
    val percent = round(summary.fraction * 100).toInt()
    val isOver = summary.isOverTarget

    BrutalSurface(
        modifier = modifier
            .fillMaxWidth()
            .popIn(delayMillis = 50, durationMillis = 700)
            .semantics(mergeDescendants = true) {
                contentDescription = if (isOver) {
                    "Hôm nay đã vượt mục tiêu ${abs(summary.remainingKcal)} kcal"
                } else {
                    "Còn ${summary.remainingKcal} kcal, đã nạp ${summary.eatenKcal} trên ${summary.targetKcal} kcal"
                }
            },
        shape = RoundedCornerShape(32.dp),
        color = colors.primary,
        shadowOffset = VfDimens.ShadowXL,
    ) {
        Blob(Modifier.align(Alignment.TopEnd).offset(x = 46.dp, y = (-52).dp).size(150.dp))
        Blob(Modifier.align(Alignment.BottomStart).offset(x = (-30).dp, y = 60.dp).size(120.dp))
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "NĂNG LƯỢNG HÔM NAY", style = type.overline, color = colors.yellow)
                Text(
                    text = "${summary.confirmedMealCount} bữa đã lưu",
                    style = type.caption,
                    color = colors.onPrimaryMuted,
                )
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
                    ProgressRing(
                        progress = ring,
                        radius = 70.dp,
                        strokeWidth = 16.dp,
                        trackWidth = 18.dp,
                        trackColor = Color.White.copy(alpha = 0.18f),
                        progressColor = if (isOver) colors.coral else colors.yellow,
                        outlineColor = colors.ink,
                        outlineWidth = 22.dp,
                        modifier = Modifier.size(176.dp),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val remaining = (abs(summary.remainingKcal) * countUp).roundToInt()
                        Text(
                            text = if (isOver) "+${VnFormat.thousands(remaining)}" else VnFormat.thousands(remaining),
                            style = type.numberXL,
                            color = colors.surface,
                        )
                        Text(
                            text = if (isOver) "kcal vượt mục tiêu" else "kcal còn lại",
                            style = type.caption,
                            color = colors.onPrimaryMuted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    EnergySticker(
                        label = "ĐÃ NẠP",
                        kcal = (summary.eatenKcal * countUp).roundToInt(),
                        background = colors.surface,
                        labelColor = colors.textMuted,
                        modifier = Modifier.rotate(-3f).floating(amplitude = 6.dp, periodMillis = 3_200),
                    )
                    EnergySticker(
                        label = "MỤC TIÊU",
                        kcal = summary.targetKcal,
                        background = colors.mint,
                        labelColor = colors.onMint,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .rotate(3f)
                            .floating(amplitude = 6.dp, periodMillis = 3_200, delayMillis = 800),
                    )
                    PercentPill(percent = percent, isOver = isOver)
                }
            }
        }
    }
}

@Composable
private fun Blob(modifier: Modifier) {
    Box(modifier.clip(CircleShape).background(VisionFitTheme.colors.primaryBlob))
}

@Composable
private fun EnergySticker(
    label: String,
    kcal: Int,
    background: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    BrutalSurface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = background,
        shadowOffset = VfDimens.ShadowS,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(text = label, style = VisionFitTheme.type.micro, color = labelColor)
            Text(
                text = buildAnnotatedString {
                    append(VnFormat.thousands(kcal))
                    withStyle(SpanStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)) { append(" kcal") }
                },
                style = VisionFitTheme.type.numberM,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PercentPill(percent: Int, isOver: Boolean) {
    val colors = VisionFitTheme.colors
    BrutalSurface(
        shape = CircleShape,
        color = if (isOver) colors.coral else colors.yellow,
        shadowOffset = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VfIcon(VfIcons.Flame, contentDescription = null, size = 14.dp, tint = colors.ink, strokeWidth = 2.4f)
            Text(text = "$percent% mục tiêu", style = VisionFitTheme.type.labelM)
        }
    }
}
