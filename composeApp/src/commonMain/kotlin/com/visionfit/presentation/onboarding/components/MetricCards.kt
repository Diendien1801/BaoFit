package com.visionfit.presentation.onboarding.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.visionfit.domain.model.Sex
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.OvershootEasing
import com.visionfit.presentation.designsystem.components.hardShadow
import com.visionfit.presentation.designsystem.icons.IconSpec
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.common.label
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Tinted 116dp tile with a label + big value on the left and controls on the right. */
@Composable
private fun MetricTile(
    label: String,
    background: Color,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
    controls: @Composable () -> Unit,
) {
    BrutalSurface(
        modifier = modifier.heightIn(min = 116.dp),
        shape = RoundedCornerShape(VfRadius.Card),
        color = background,
        shadowOffset = VfDimens.ShadowM,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).heightIn(min = 92.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = label, style = VisionFitTheme.type.label)
                value()
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { controls() }
        }
    }
}

@Composable
fun SexCard(sex: Sex, onSexSelected: (Sex) -> Unit, modifier: Modifier = Modifier) {
    MetricTile(
        label = "Giới tính",
        background = VisionFitTheme.colors.pinkContainer,
        modifier = modifier,
        value = {
            Text(text = sex.label, style = VisionFitTheme.type.headlineL.copy(lineHeight = VisionFitTheme.type.numberStat.lineHeight))
        },
        controls = {
            SexOption("Nam", selected = sex == Sex.MALE) { onSexSelected(Sex.MALE) }
            SexOption("Nữ", selected = sex == Sex.FEMALE) { onSexSelected(Sex.FEMALE) }
        },
    )
}

@Composable
private fun SexOption(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = VisionFitTheme.colors
    BrutalSurface(
        modifier = Modifier.size(width = 56.dp, height = VfDimens.SmallButton).semantics { this.selected = selected },
        shape = RoundedCornerShape(VfRadius.M),
        color = if (selected) colors.ink else colors.surface,
        shadowOffset = 0.dp,
        pressDepth = 2.dp,
        onClick = onClick,
        role = Role.RadioButton,
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = VisionFitTheme.type.labelL, color = if (selected) colors.surface else colors.ink)
    }
}

/** Big number + unit with − / + buttons. Holding a button keeps stepping. */
@Composable
fun StepperCard(
    label: String,
    value: Int,
    unit: String,
    background: Color,
    onStep: (delta: Int) -> Unit,
    canIncrease: Boolean,
    canDecrease: Boolean,
    increaseLabel: String,
    decreaseLabel: String,
    modifier: Modifier = Modifier,
) {
    MetricTile(
        label = label,
        background = background,
        modifier = modifier,
        value = {
            Row {
                PoppingNumber(value = value, style = VisionFitTheme.type.numberStat, modifier = Modifier.alignByBaseline())
                Text(text = unit, style = VisionFitTheme.type.label, modifier = Modifier.alignByBaseline().padding(start = 3.dp))
            }
        },
        controls = {
            RepeatingStepButton(VfIcons.Plus, increaseLabel, enabled = canIncrease) { onStep(+1) }
            RepeatingStepButton(VfIcons.Minus, decreaseLabel, enabled = canDecrease) { onStep(-1) }
        },
    )
}

/** Re-plays a small "pop" each time [value] changes (design keyframe `vf-num`). */
@Composable
private fun PoppingNumber(value: Int, style: TextStyle, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    var isFirstValue by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (isFirstValue) {
            isFirstValue = false
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(350, easing = OvershootEasing))
        }
    }
    Text(
        text = value.toString(),
        style = style,
        modifier = modifier.graphicsLayer {
            val p = progress.value
            val scale = 0.6f + 0.4f * p
            scaleX = scale
            scaleY = scale
            alpha = (0.2f + 0.8f * p).coerceIn(0f, 1f)
        },
    )
}

@Composable
private fun RepeatingStepButton(
    icon: IconSpec,
    contentDescription: String,
    enabled: Boolean,
    onStep: () -> Unit,
) {
    val colors = VisionFitTheme.colors
    val latestOnStep by rememberUpdatedState(onStep)
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh),
        label = "stepPress",
    )
    val travel = with(LocalDensity.current) { 2.dp.toPx() }
    val shape = RoundedCornerShape(VfRadius.M)
    Box(
        modifier = Modifier
            .size(VfDimens.SmallButton)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                if (!enabled) disabled()
                onClick { latestOnStep(); true }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        pressed = true
                        latestOnStep()
                        val repeater = launch {
                            delay(400)
                            while (true) {
                                latestOnStep()
                                delay(70)
                            }
                        }
                        waitForUpOrCancellation()
                        repeater.cancel()
                        pressed = false
                    }
                }
            }
            .graphicsLayer {
                translationX = press * travel
                translationY = press * travel
            }
            .hardShadow(shape, colors.ink) { travel * (1f - press) }
            .clip(shape)
            .background(colors.surface)
            .border(VfDimens.Border, colors.ink, shape),
        contentAlignment = Alignment.Center,
    ) {
        VfIcon(icon, contentDescription = null, size = 18.dp, tint = colors.ink)
    }
}
