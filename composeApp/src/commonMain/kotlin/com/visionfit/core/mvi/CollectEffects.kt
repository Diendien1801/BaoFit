package com.visionfit.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Collects one-shot [effects] only while the screen is at least STARTED, on
 * `Main.immediate` so an effect emitted right before a lifecycle change is not lost.
 */
@Composable
fun <T> CollectEffects(effects: Flow<T>, onEffect: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnEffect by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                effects.collect { latestOnEffect(it) }
            }
        }
    }
}
