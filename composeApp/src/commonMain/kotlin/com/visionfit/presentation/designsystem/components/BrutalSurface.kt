package com.visionfit.presentation.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/**
 * The building block of the "playful brutalist" look: flat fill, 2dp ink outline and a hard
 * offset shadow. When [onClick] is set it becomes a button that sinks into its shadow when
 * pressed (the design's `:active { transform: translate(3px,3px); box-shadow: 0 }`).
 */
@Composable
fun BrutalSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(VfRadius.Card),
    color: Color = VisionFitTheme.colors.surface,
    borderColor: Color = VisionFitTheme.colors.ink,
    borderWidth: Dp = VfDimens.Border,
    dashed: Boolean = false,
    shadowOffset: Dp = VfDimens.ShadowM,
    shadowColor: Color = VisionFitTheme.colors.ink,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role = Role.Button,
    pressDepth: Dp = 3.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (isPressed && enabled && onClick != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh),
        label = "brutalPress",
    )
    val density = LocalDensity.current
    val shadowPx = with(density) { shadowOffset.toPx() }
    val travelPx = with(density) { pressDepth.toPx() }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    val outline = when {
        borderWidth.value <= 0f || borderColor.alpha == 0f -> Modifier
        dashed -> Modifier.dashedBorder(borderWidth, borderColor, shape, dash = 7.dp, gap = 5.dp)
        else -> Modifier.border(borderWidth, borderColor, shape)
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.55f)
            .graphicsLayer {
                translationX = press * travelPx
                translationY = press * travelPx
            }
            .hardShadow(shape, shadowColor) { shadowPx * (1f - press) }
            .then(clickModifier)
            .clip(shape)
            .background(color)
            .then(outline),
        contentAlignment = contentAlignment,
        content = content,
    )
}
