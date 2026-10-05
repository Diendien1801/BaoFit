package com.visionfit.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.visionfit.presentation.analysis.AnalysisRoute
import com.visionfit.presentation.auth.AuthRoute
import com.visionfit.presentation.camera.CameraRoute
import com.visionfit.presentation.dashboard.DashboardRoute
import com.visionfit.presentation.goal.GoalRoute
import com.visionfit.presentation.history.HistoryRoute
import com.visionfit.presentation.onboarding.OnboardingRoute
import com.visionfit.presentation.review.ReviewRoute

/**
 * App flow:
 * Auth → (register) Onboarding → Goal → Dashboard; (login) Dashboard.
 * Dashboard ↔ History via the bottom bar; Camera → Analysis → Review → Dashboard.
 */
@Composable
fun VisionFitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AuthDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable<AuthDestination> {
            AuthRoute(
                onNavigateToDashboard = { navController.navigateToDashboard() },
                onNavigateToOnboarding = { navController.navigate(OnboardingDestination(isEditing = false)) },
            )
        }
        composable<OnboardingDestination> { entry ->
            val route = entry.toRoute<OnboardingDestination>()
            OnboardingRoute(
                isEditing = route.isEditing,
                onNavigateToGoal = { navController.navigate(GoalDestination) { launchSingleTop = true } },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<GoalDestination> {
            GoalRoute(
                onNavigateToDashboard = { navController.navigateToDashboard() },
                onNavigateToProfile = {
                    // Coming from onboarding: just go back to it; otherwise open the profile editor.
                    val cameFromOnboarding =
                        navController.previousBackStackEntry?.destination?.hasRoute<OnboardingDestination>() == true
                    if (cameFromOnboarding) navController.popBackStack() else navController.navigate(OnboardingDestination(isEditing = true))
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<DashboardDestination> {
            DashboardRoute(
                onNavigateToHistory = { navController.navigateToTab(HistoryDestination) },
                onNavigateToCamera = { navController.navigate(CameraDestination) { launchSingleTop = true } },
                onNavigateToGoal = { navController.navigate(GoalDestination) { launchSingleTop = true } },
                onNavigateToProfile = { navController.navigate(OnboardingDestination(isEditing = true)) { launchSingleTop = true } },
                onNavigateToAnalysis = { jobId -> navController.navigate(AnalysisDestination(jobId)) },
                onNavigateToReview = { jobId -> navController.navigate(ReviewDestination(jobId)) },
            )
        }
        composable<HistoryDestination> {
            HistoryRoute(
                onNavigateToAnalysis = { jobId -> navController.navigate(AnalysisDestination(jobId)) },
                onNavigateToReview = { jobId -> navController.navigate(ReviewDestination(jobId)) },
                onNavigateToCamera = { navController.navigate(CameraDestination) { launchSingleTop = true } },
                onNavigateToDashboard = { navController.navigateToDashboard() },
                onNavigateToGoal = { navController.navigate(GoalDestination) { launchSingleTop = true } },
                onNavigateToProfile = { navController.navigate(OnboardingDestination(isEditing = true)) { launchSingleTop = true } },
            )
        }
        composable<CameraDestination> {
            CameraRoute(
                onNavigateToAnalysis = { jobId ->
                    navController.navigate(AnalysisDestination(jobId)) { popUpTo<CameraDestination> { inclusive = true } }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<AnalysisDestination> { entry ->
            val route = entry.toRoute<AnalysisDestination>()
            AnalysisRoute(
                jobId = route.jobId,
                onNavigateToDashboard = { navController.navigateToDashboard() },
                onNavigateToReview = { jobId, manual ->
                    navController.navigate(ReviewDestination(jobId, manual)) { popUpTo<AnalysisDestination> { inclusive = true } }
                },
            )
        }
        composable<ReviewDestination> { entry ->
            val route = entry.toRoute<ReviewDestination>()
            ReviewRoute(
                jobId = route.jobId,
                manualEntry = route.manualEntry,
                onNavigateToDashboard = { navController.navigateToDashboard() },
                onNavigateToCamera = {
                    navController.navigate(CameraDestination) { popUpTo<ReviewDestination> { inclusive = true } }
                },
            )
        }
    }
}

/** Returns to an existing Dashboard, or makes it the only screen (after sign-in / onboarding). */
private fun NavController.navigateToDashboard() {
    if (!popBackStack<DashboardDestination>(inclusive = false)) {
        navigate(DashboardDestination) {
            popUpTo(graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}

/** Bottom-bar style navigation: one copy per tab, Dashboard stays underneath. */
private fun NavController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo<DashboardDestination> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
