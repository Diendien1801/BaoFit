package com.visionfit.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.visionfit.core.log.AppLog
import com.visionfit.core.time.SystemTimeProvider
import com.visionfit.core.time.TimeProvider
import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.data.remote.ApiUrls
import com.visionfit.data.remote.TokenStore
import com.visionfit.data.remote.createHttpClient
import com.visionfit.data.repository.MockAuthRepository
import com.visionfit.data.repository.MockCameraSource
import com.visionfit.data.repository.MockMealAnalysisRepository
import com.visionfit.data.repository.MockMealRepository
import com.visionfit.data.repository.MockNetworkMonitor
import com.visionfit.data.repository.MockProfileRepository
import com.visionfit.data.repository.RemoteAuthRepository
import com.visionfit.data.repository.RemoteMealRepository
import com.visionfit.data.repository.RemoteProfileRepository
import com.visionfit.domain.repository.AuthRepository
import com.visionfit.domain.repository.CameraSource
import com.visionfit.domain.repository.MealAnalysisRepository
import com.visionfit.domain.repository.MealRepository
import com.visionfit.domain.repository.NetworkMonitor
import com.visionfit.domain.repository.ProfileRepository
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.domain.usecase.SummarizeDayUseCase
import com.visionfit.domain.usecase.ValidateCredentialsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency graph. Each `Mock*` repository is replaced by a Ktor-backed implementation of
 * the same domain interface once its service exists; nothing above the data layer changes.
 * So far auth (identity-service), profile (profile-service) and the meal diary (meal-service)
 * talk to the backend; photo analysis is still the mock.
 */
class AppContainer(
    val timeProvider: TimeProvider = SystemTimeProvider(),
    /** Outlives every screen, so analysis jobs keep running in the background. */
    applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    /** Backend services, or `null` to keep auth, profile and meals on the in-memory mocks (tests, previews). */
    api: ApiUrls? = null,
) {
    /** With a backend it only carries the mock analysis pipeline, so no demo meals. */
    private val store = InMemoryVisionFitStore(timeProvider, withDemoData = api == null)

    init {
        if (api == null) {
            AppLog.d("Api", "Using in-memory mocks for auth, profile and meals")
        } else {
            AppLog.d("Api", "identity-service: ${api.identity} | profile-service: ${api.profile} | meal-service: ${api.meal}")
        }
    }

    /** Shared: auth fills it on sign-in, every other remote repository sends its access token. */
    private val tokenStore = TokenStore()

    val authRepository: AuthRepository =
        if (api == null) MockAuthRepository(store) else RemoteAuthRepository(createHttpClient(api.identity), tokenStore)
    val profileRepository: ProfileRepository =
        if (api == null) {
            MockProfileRepository(store)
        } else {
            RemoteProfileRepository(createHttpClient(api.profile), tokenStore, authRepository.session, applicationScope)
        }
    val mealRepository: MealRepository =
        if (api == null) {
            MockMealRepository(store)
        } else {
            RemoteMealRepository(createHttpClient(api.meal), tokenStore, authRepository.session, store)
        }
    val mealAnalysisRepository: MealAnalysisRepository = MockMealAnalysisRepository(store, applicationScope)
    val networkMonitor: NetworkMonitor = MockNetworkMonitor(timeProvider)
    val cameraSource: CameraSource = MockCameraSource()

    val calculateNutritionTarget = CalculateNutritionTargetUseCase()
    val summarizeDay = SummarizeDayUseCase()
    val validateCredentials = ValidateCredentialsUseCase()
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided. Wrap the UI in VisionFitApp().")
}
