package com.visionfit.presentation.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.visionfit.presentation.designsystem.icons.IconSpec
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/** Full-width 58dp call to action. */
@Composable
fun VfPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = VisionFitTheme.colors.yellow,
    contentColor: Color = VisionFitTheme.colors.ink,
    leadingIcon: IconSpec? = null,
    trailingIcon: IconSpec? = VfIcons.ArrowRight,
    iconTint: Color = contentColor,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = VfDimens.CtaHeight,
) {
    BrutalSurface(
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(VfRadius.XL),
        color = containerColor,
        shadowOffset = VfDimens.ShadowM,
        onClick = onClick,
        enabled = enabled && !loading,
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.2f),
                    strokeWidth = 3.dp,
                )
            } else {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingIcon?.let { VfIcon(it, contentDescription = null, size = 22.dp, tint = iconTint) }
                    Text(
                        text = text,
                        style = if (height < VfDimens.CtaHeight) VisionFitTheme.type.buttonSmall else VisionFitTheme.type.button,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    trailingIcon?.let { VfIcon(it, contentDescription = null, size = 22.dp, tint = iconTint) }
                }
            }
        }
    }
}

/** The 46dp square tile used for back / close / calendar / notifications. */
@Composable
fun VfIconTileButton(
    icon: IconSpec,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = VisionFitTheme.colors.surface,
    iconTint: Color = VisionFitTheme.colors.ink,
    iconFill: Color = Color.Transparent,
    shadowColor: Color = VisionFitTheme.colors.ink,
    size: Dp = VfDimens.TopBarButton,
) {
    BrutalSurface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(VfRadius.L),
        color = containerColor,
        shadowOffset = VfDimens.ShadowS,
        shadowColor = shadowColor,
        onClick = onClick,
        onClickLabel = contentDescription,
        contentAlignment = Alignment.Center,
    ) {
        VfIcon(icon, contentDescription = contentDescription, size = 20.dp, tint = iconTint, fill = iconFill)
    }
}

/** Borderless purple text action, 44dp tall so it stays an easy touch target. */
@Composable
fun VfTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = VisionFitTheme.colors.primary,
    trailingIcon: IconSpec? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp),
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = VisionFitTheme.type.bodyStrong, color = color, textAlign = TextAlign.Center)
        trailingIcon?.let { VfIcon(it, contentDescription = null, size = 16.dp, tint = color) }
    }
}

/** Full-width variant of [VfTextButton] for secondary actions under a CTA. */
@Composable
fun VfSecondaryLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        VfTextButton(text = text, onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}
