package com.visionfit.presentation.goal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.visionfit.core.format.VnFormat
import com.visionfit.presentation.common.icon
import com.visionfit.presentation.common.title
import com.visionfit.domain.model.FitnessGoal
import com.visionfit.domain.model.NutritionTarget
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.Macro
import com.visionfit.presentation.designsystem.components.ProgressRing
import com.visionfit.presentation.designsystem.components.animatedProgress
import com.visionfit.presentation.designsystem.components.dashedBorder
import com.visionfit.presentation.designsystem.components.macroPalette
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.components.wiggle
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import kotlin.math.roundToInt

/** Yellow hero card: counting-up daily target plus how it was computed. */
@Composable
fun TargetEnergyCard(target: NutritionTarget, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val type = VisionFitTheme.type
    val countUp = animatedProgress(target = 1f, durationMillis = 1_400, delayMillis = 0)
    Box(modifier = modifier.padding(top = 16.dp)) {
        BrutalSurface(
            modifier = Modifier.fillMaxWidth().popIn(delayMillis = 100, durationMillis = 700),
            shape = RoundedCornerShape(VfRadius.Hero),
            color = colors.yellow,
            shadowOffset = VfDimens.ShadowXL,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(text = "NĂNG LƯỢNG MỤC TIÊU", style = type.overline)
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${VnFormat.thousands(target.dailyKcal)} kcal mỗi ngày"
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = VnFormat.thousands((target.dailyKcal * countUp).roundToInt()),
                        style = type.numberHero,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = "kcal / ngày",
                        style = type.buttonSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatCell("BMR", VnFormat.thousands(target.bmrKcal), Modifier.weight(1f))
                    StatCell("TDEE ×${VnFormat.factor(target.activityMultiplier)}", VnFormat.thousands(target.tdeeKcal), Modifier.weight(1f))
                    StatCell(
                        label = "Điều chỉnh",
                        value = VnFormat.signed(target.adjustmentKcal),
                        modifier = Modifier.weight(1f),
                        background = colors.ink,
                        labelColor = colors.onPrimaryMuted,
                        valueColor = colors.yellow,
                    )
                }
            }
        }
        GoalBadge(
            goal = target.goal,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-16).dp, y = (-16).dp)
                .wiggle(fromDegrees = -8f, toDegrees = 4f, periodMillis = 1_600),
        )
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    background: Color = VisionFitTheme.colors.surface,
    labelColor: Color = VisionFitTheme.colors.textSecondary,
    valueColor: Color = VisionFitTheme.colors.ink,
) {
    val shape = RoundedCornerShape(VfRadius.M)
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(VfDimens.Border, VisionFitTheme.colors.ink, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text = label, style = VisionFitTheme.type.chip, color = labelColor, maxLines = 1)
        Text(text = value, style = VisionFitTheme.type.numberS, color = valueColor, maxLines = 1)
    }
}

@Composable
private fun GoalBadge(goal: FitnessGoal, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    BrutalSurface(
        modifier = modifier,
        shape = CircleShape,
        color = if (goal == FitnessGoal.FAT_LOSS) colors.coral else colors.mint,
        shadowOffset = VfDimens.ShadowS,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VfIcon(goal.icon, contentDescription = null, size = 16.dp, tint = colors.ink, strokeWidth = 2.8f)
            Text(text = goal.title, style = VisionFitTheme.type.labelL)
        }
    }
}

/** Three tilted cards with a ring per macro (energy share) and the gram target. */
@Composable
fun MacroSplitRow(target: NutritionTarget, modifier: Modifier = Modifier) {
    val macros = target.macros
    val cards = listOf(
        MacroCardSpec(Macro.PROTEIN, "Protein", macros.proteinPercent, macros.proteinG, tilt = -2f, topOffset = 0, delay = 300),
        MacroCardSpec(Macro.CARBS, "Carbs", macros.carbsPercent, macros.carbsG, tilt = 1.5f, topOffset = 6, delay = 400),
        MacroCardSpec(Macro.FAT, "Fat", macros.fatPercent, macros.fatG, tilt = -1f, topOffset = 0, delay = 500),
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.forEach { spec ->
            MacroRingCard(spec, Modifier.weight(1f).padding(top = spec.topOffset.dp))
        }
    }
}

private data class MacroCardSpec(
    val macro: Macro,
    val label: String,
    val percent: Int,
    val grams: Int,
    val tilt: Float,
    val topOffset: Int,
    val delay: Int,
)

@Composable
private fun MacroRingCard(spec: MacroCardSpec, modifier: Modifier = Modifier) {
    val palette = macroPalette(spec.macro)
    val ring = animatedProgress(target = spec.percent / 100f, durationMillis = 1_100, delayMillis = spec.delay + 150)
    BrutalSurface(
        modifier = modifier
            .rotate(spec.tilt)
            .popIn(delayMillis = spec.delay, durationMillis = 600)
            .semantics(mergeDescendants = true) {
                contentDescription = "${spec.label}: ${spec.percent} phần trăm, ${spec.grams} gam"
            },
        shape = RoundedCornerShape(VfRadius.Card),
        shadowOffset = VfDimens.ShadowM,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = ring,
                    radius = 28.dp,
                    strokeWidth = 12.dp,
                    trackColor = palette.track,
                    progressColor = palette.fill,
                    modifier = Modifier.size(72.dp),
                )
                Text(text = "${spec.percent}%", style = VisionFitTheme.type.cardTitle)
            }
            Text(text = spec.label, style = VisionFitTheme.type.label)
            Text(text = "${spec.grams}g", style = VisionFitTheme.type.numberM)
        }
    }
}

/** Dashed hint: the goal can be changed later from the profile. */
@Composable
fun EditLaterHint(modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val shape = RoundedCornerShape(VfRadius.XL)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .dashedBorder(VfDimens.Border, colors.ink, shape, dash = 7.dp, gap = 5.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(VfRadius.Xs))
                .background(colors.mintContainer)
                .border(VfDimens.Border, colors.ink, RoundedCornerShape(VfRadius.Xs)),
            contentAlignment = Alignment.Center,
        ) {
            VfIcon(VfIcons.Pencil, contentDescription = null, size = 16.dp, tint = colors.ink)
        }
        Text(
            text = buildAnnotatedString {
                append("Muốn đổi mục tiêu? Vào ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Hồ sơ") }
                append(" chỉnh lúc nào cũng được.")
            },
            style = VisionFitTheme.type.bodySmall,
        )
    }
}
