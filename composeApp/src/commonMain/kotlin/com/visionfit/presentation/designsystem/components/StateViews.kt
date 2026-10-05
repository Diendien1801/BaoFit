package com.visionfit.presentation.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.visionfit.presentation.designsystem.icons.IconSpec
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/** Spinning sparkle in a yellow tile: the app-wide loading indicator. */
@Composable
fun VfLoadingIndicator(modifier: Modifier = Modifier, label: String = "Đang tải…") {
    Column(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrutalSurface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(VfRadius.L),
            color = VisionFitTheme.colors.yellow,
            shadowOffset = VfDimens.ShadowS,
            contentAlignment = Alignment.Center,
        ) {
            VfIcon(
                icon = VfIcons.Sparkle,
                contentDescription = null,
                size = 28.dp,
                tint = VisionFitTheme.colors.ink,
                fill = VisionFitTheme.colors.surface,
                modifier = Modifier.spinning(1_400),
            )
        }
        Text(text = label, style = VisionFitTheme.type.caption, color = VisionFitTheme.colors.textSecondary)
    }
}

@Composable
fun VfFullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { VfLoadingIndicator() }
}

/** Dashed card for "nothing here yet" and recoverable errors. */
@Composable
fun VfMessageCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: IconSpec = VfIcons.Plate,
    iconContainer: androidx.compose.ui.graphics.Color = VisionFitTheme.colors.primaryContainer,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    BrutalSurface(
        modifier = modifier.fillMaxWidth(),
        dashed = true,
        shadowOffset = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrutalSurface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(VfRadius.M),
                color = iconContainer,
                shadowOffset = VfDimens.ShadowXs,
                contentAlignment = Alignment.Center,
            ) {
                VfIcon(icon, contentDescription = null, size = 24.dp, tint = VisionFitTheme.colors.ink)
            }
            Text(text = title, style = VisionFitTheme.type.cardTitle, textAlign = TextAlign.Center)
            Text(
                text = message,
                style = VisionFitTheme.type.bodySmall,
                color = VisionFitTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                VfPrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.padding(top = 6.dp),
                    height = 50.dp,
                    trailingIcon = null,
                )
            }
        }
    }
}

@Composable
fun VfErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Ối, chưa tải được",
) {
    Box(modifier = modifier.fillMaxSize().padding(VfDimens.ScreenPadding), contentAlignment = Alignment.Center) {
        VfMessageCard(
            title = title,
            message = message,
            icon = VfIcons.Alert,
            iconContainer = VisionFitTheme.colors.coralContainer,
            actionLabel = "Thử lại",
            onAction = onRetry,
        )
    }
}
