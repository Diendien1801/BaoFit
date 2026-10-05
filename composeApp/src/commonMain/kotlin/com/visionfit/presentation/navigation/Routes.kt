package com.visionfit.presentation.navigation

import kotlinx.serialization.Serializable

/** Type-safe destinations. Arguments travel as constructor properties. */
@Serializable
data object AuthDestination

@Serializable
data class OnboardingDestination(val isEditing: Boolean = false)

@Serializable
data object GoalDestination

@Serializable
data object DashboardDestination

@Serializable
data object HistoryDestination

@Serializable
data object CameraDestination

@Serializable
data class AnalysisDestination(val jobId: String)

@Serializable
data class ReviewDestination(val jobId: String, val manualEntry: Boolean = false)
