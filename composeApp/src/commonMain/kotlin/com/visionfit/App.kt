package com.visionfit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.visionfit.data.remote.defaultApiBaseUrl
import com.visionfit.di.AppContainer
import com.visionfit.di.LocalAppContainer
import com.visionfit.presentation.common.AppMessenger
import com.visionfit.presentation.common.LocalAppMessenger
import com.visionfit.presentation.designsystem.components.VfSnackbarHost
import com.visionfit.presentation.designsystem.layout.ProvideWindowLayout
import com.visionfit.presentation.designsystem.layout.VfContentWidth
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.navigation.VisionFitNavHost

/** Shared entry point for Android, iOS and desktop. */
@Composable
fun VisionFitApp(container: AppContainer = AppGraph.container) {
    VisionFitTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val messenger = remember(snackbarHostState, scope) { AppMessenger(snackbarHostState, scope) }
        CompositionLocalProvider(
            LocalAppContainer provides container,
            LocalAppMessenger provides messenger,
        ) {
            // Every screen picks its layout (bottom bar or rail, one or two panes) from this.
            ProvideWindowLayout {
                Box(Modifier.fillMaxSize()) {
                    VisionFitNavHost()
                    VfSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            // Clears the floating bottom bar and fixed footers.
                            .padding(bottom = 96.dp)
                            .widthIn(max = VfContentWidth.Form),
                    )
                }
            }
        }
    }
}

/**
 * Process-wide dependency graph. It outlives configuration changes (ViewModels that survive a
 * rotation keep talking to the same repositories) and is created on first use.
 *
 * Signs in against the real backend; pass `apiBaseUrl = null` to go back to the demo account.
 */
object AppGraph {
    val container: AppContainer by lazy { AppContainer(apiBaseUrl = defaultApiBaseUrl) }
}
