package com.visionfit.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.layout.LocalWindowLayout
import com.visionfit.presentation.designsystem.layout.safeHorizontal
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/**
 * Three-slot header used by the flow screens: a 46dp action on each side and a centered
 * title. Empty slots keep their width so the title stays centered. Side padding follows the
 * window gutter and clears a landscape display cutout.
 */
@Composable
fun VfTopBar(
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = { Spacer(Modifier.size(VfDimens.TopBarButton)) },
    action: @Composable () -> Unit = { Spacer(Modifier.size(VfDimens.TopBarButton)) },
    title: @Composable () -> Unit,
) {
    val gutter = LocalWindowLayout.current.gutter
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeHorizontal)
            .padding(start = gutter, end = gutter, top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigation()
        Box(Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) { title() }
        action()
    }
}

@Composable
fun VfBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    VfIconTileButton(icon = VfIcons.ChevronLeft, contentDescription = "Quay lại", onClick = onClick, modifier = modifier)
}

@Composable
fun VfCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    VfIconTileButton(icon = VfIcons.Close, contentDescription = "Đóng", onClick = onClick, modifier = modifier)
}

/** "Bước 1 / 2" with one pill per step. */
@Composable
fun StepIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics { contentDescription = "Bước $current trên $total" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(text = "Bước $current / $total", style = VisionFitTheme.type.labelM)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 12.dp)
                        .clip(CircleShape)
                        .background(if (index < current) VisionFitTheme.colors.primary else VisionFitTheme.colors.surface)
                        .border(VfDimens.Border, VisionFitTheme.colors.ink, CircleShape),
                )
            }
        }
    }
}
