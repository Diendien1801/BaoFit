package com.visionfit.presentation.onboarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.visionfit.domain.model.ActivityLevel
import com.visionfit.domain.model.FitnessGoal
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.CheckBadge
import com.visionfit.presentation.designsystem.icons.IconSpec
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.common.description
import com.visionfit.presentation.common.icon
import com.visionfit.presentation.common.title

/** Selectable card with a yellow check badge hanging off its top-right corner. */
@Composable
private fun ChoiceCard(
    selected: Boolean,
    onClick: () -> Unit,
    background: Color,
    minHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        BrutalSurface(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = minHeight)
                .semantics { this.selected = selected },
            shape = RoundedCornerShape(VfRadius.XL),
            color = background,
            shadowOffset = if (selected) VfDimens.ShadowM else VfDimens.ShadowXs,
            onClick = onClick,
            role = Role.RadioButton,
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
        if (selected) {
            CheckBadge(Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-9).dp))
        }
    }
}

@Composable
fun ActivityChoice(
    level: ActivityLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VisionFitTheme.colors
    val background by animateColorAsState(if (selected) colors.primary else colors.surface, label = "activityBg")
    val content = if (selected) colors.surface else colors.ink
    ChoiceCard(selected = selected, onClick = onClick, background = background, minHeight = 64.dp, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(level.icon, background = level.tint())
            Column {
                Text(text = level.title, style = VisionFitTheme.type.bodyStrong, color = content)
                Text(
                    text = level.description,
                    style = VisionFitTheme.type.chip.copy(fontWeight = FontWeight.Normal),
                    color = if (selected) colors.onPrimaryMuted else colors.textSecondary,
                )
            }
        }
    }
}

@Composable
fun GoalChoice(
    goal: FitnessGoal,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VisionFitTheme.colors
    val (onColor, offColor) = when (goal) {
        FitnessGoal.FAT_LOSS -> colors.coral to colors.coralContainer
        FitnessGoal.MUSCLE_GAIN -> colors.mint to colors.mintPale
    }
    val background by animateColorAsState(if (selected) onColor else offColor, label = "goalBg")
    ChoiceCard(selected = selected, onClick = onClick, background = background, minHeight = 60.dp, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(goal.icon, background = colors.surface)
            Column {
                Text(text = goal.title, style = VisionFitTheme.type.cardTitle)
                Text(text = goal.description, style = VisionFitTheme.type.caption)
            }
        }
    }
}

@Composable
private fun IconChip(icon: IconSpec, background: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .border(VfDimens.Border, VisionFitTheme.colors.ink, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        VfIcon(icon, contentDescription = null, size = 18.dp, tint = VisionFitTheme.colors.ink)
    }
}

private val ActivityLevel.title: String
    get() = when (this) {
        ActivityLevel.SEDENTARY -> "Ít vận động"
        ActivityLevel.LIGHT -> "Vận động nhẹ"
        ActivityLevel.MODERATE -> "Vận động vừa"
        ActivityLevel.ACTIVE -> "Vận động nhiều"
    }

private val ActivityLevel.description: String
    get() = when (this) {
        ActivityLevel.SEDENTARY -> "Ngồi nhiều, ít tập"
        ActivityLevel.LIGHT -> "Tập 1–3 buổi / tuần"
        ActivityLevel.MODERATE -> "Tập 3–5 buổi / tuần"
        ActivityLevel.ACTIVE -> "Tập 6–7 buổi / tuần"
    }

private val ActivityLevel.icon: IconSpec
    get() = when (this) {
        ActivityLevel.SEDENTARY -> VfIcons.Sofa
        ActivityLevel.LIGHT -> VfIcons.Walk
        ActivityLevel.MODERATE -> VfIcons.Bike
        ActivityLevel.ACTIVE -> VfIcons.Dumbbell
    }

@Composable
private fun ActivityLevel.tint(): Color {
    val colors = VisionFitTheme.colors
    return when (this) {
        ActivityLevel.SEDENTARY -> colors.pinkContainer
        ActivityLevel.LIGHT -> colors.skyContainer
        ActivityLevel.MODERATE -> colors.mintContainer
        ActivityLevel.ACTIVE -> colors.yellowPale
    }
}
