package com.visionfit.presentation.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * App-level snackbar. Screens call it from their effect handlers; it shows the message in a
 * scope that outlives the screen, so "Đã lưu bữa ăn" still appears after navigating away.
 */
@Stable
class AppMessenger(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    fun show(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

val LocalAppMessenger = staticCompositionLocalOf<AppMessenger> {
    error("AppMessenger not provided. Wrap the UI in VisionFitApp().")
}
