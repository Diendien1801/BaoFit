package com.visionfit.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.visionfit.VisionFitApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "VisionFit",
        // Phone-sized window that matches the 390 × 844 design frames.
        state = rememberWindowState(size = DpSize(390.dp, 844.dp)),
    ) {
        VisionFitApp()
    }
}
