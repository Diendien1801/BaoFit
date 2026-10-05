package com.visionfit.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.visionfit.core.time.SystemTimeProvider
import com.visionfit.core.time.TimeProvider
import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.data.repository.MockAuthRepository
import com.visionfit.data.repository.MockCameraSource
import com.visionfit.data.repository.MockMealAnalysisRepository
import com.visionfit.data.repository.MockMealRepository
import com.visionfit.data.repository.MockNetworkMonitor
import com.visionfit.data.repository.MockProfileRepository
import com.visionfit.domain.repository.AuthRepository
import com.visionfit.domain.repository.CameraSource
import com.visionfit.domain.repository.MealAnalysisRepository
import com.visionfit.domain.repository.MealRepository
import com.visionfit.domain.repository.NetworkMonitor
import com.visionfit.domain.repository.ProfileRepository
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.domain.usecase.CalculateStreakUseCase
import com.visionfit.domain.usecase.SummarizeDayUseCase
import com.visionfit.domain.usecase.ValidateCredentialsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency graph. To plug in a real backend, replace the `Mock*` repositories with
 * Ktor-backed implementations of the same domain interfaces; nothing above the data layer changes.
 */
class AppContainer(
    val timeProvider: TimeProvider = SystemTimeProvider(),
    /** Outlives every screen, so analysis jobs keep running in the background. */
    applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val store = InMemoryVisionFitStore(timeProvider)

    val authRepository: AuthRepository = MockAuthRepository(store)
    val profileRepository: ProfileRepository = MockProfileRepository(store)
    val mealRepository: MealRepository = MockMealRepository(store)
    val mealAnalysisRepository: MealAnalysisRepository = MockMealAnalysisRepository(store, applicationScope)
    val networkMonitor: NetworkMonitor = MockNetworkMonitor(timeProvider)
    val cameraSource: CameraSource = MockCameraSource()

    val calculateNutritionTarget = CalculateNutritionTargetUseCase()
    val summarizeDay = SummarizeDayUseCase()
    val calculateStreak = CalculateStreakUseCase()
    val validateCredentials = ValidateCredentialsUseCase()
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided. Wrap the UI in VisionFitApp().")
}
