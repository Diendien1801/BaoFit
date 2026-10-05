package com.visionfit.presentation.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/** Snackbar in the house style: ink card, yellow shadow and action. */
@Composable
fun VfSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data -> VfSnackbar(data) }
}

@Composable
private fun VfSnackbar(data: SnackbarData) {
    BrutalSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(VfRadius.L),
        color = VisionFitTheme.colors.ink,
        shadowOffset = VfDimens.ShadowS,
        shadowColor = VisionFitTheme.colors.yellow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.visuals.message,
                style = VisionFitTheme.type.bodySmall,
                color = VisionFitTheme.colors.surface,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            data.visuals.actionLabel?.let { label ->
                VfTextButton(
                    text = label,
                    onClick = { data.performAction() },
                    color = VisionFitTheme.colors.yellow,
                )
            }
        }
    }
}
