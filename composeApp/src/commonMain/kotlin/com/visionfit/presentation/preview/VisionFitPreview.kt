package com.visionfit.presentation.preview

import androidx.compose.runtime.Composable
import com.visionfit.presentation.designsystem.layout.ProvideWindowLayout
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/** Theme plus a measured window, so a @Preview of any size shows the layout that size gets. */
@Composable
fun VisionFitPreview(content: @Composable () -> Unit) {
    VisionFitTheme { ProvideWindowLayout(content = content) }
}
