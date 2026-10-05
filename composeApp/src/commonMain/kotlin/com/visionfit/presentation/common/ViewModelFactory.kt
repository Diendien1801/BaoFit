package com.visionfit.presentation.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visionfit.di.AppContainer
import com.visionfit.di.LocalAppContainer

/**
 * Creates a ViewModel scoped to the current navigation entry, built from the app's
 * dependency graph. [key] separates instances that share a type (e.g. two job ids).
 */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    key: String? = null,
    crossinline create: AppContainer.() -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(key = key) { container.create() }
}
