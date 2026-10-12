package com.visionfit.presentation

import com.visionfit.core.time.FixedTimeProvider
import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.di.AppContainer
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.ProfileStatus
import com.visionfit.domain.repository.ProfileRepository
import com.visionfit.presentation.auth.AuthEffect
import com.visionfit.presentation.auth.AuthEvent
import com.visionfit.presentation.auth.AuthViewModel
import com.visionfit.presentation.dashboard.DashboardEvent
import com.visionfit.presentation.dashboard.DashboardViewModel
import com.visionfit.presentation.onboarding.BodyMetric
import com.visionfit.presentation.onboarding.OnboardingEffect
import com.visionfit.presentation.onboarding.OnboardingEvent
import com.visionfit.presentation.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** How screens behave while the profile downloads, is missing, or failed (only possible with the backend). */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileGateTests {

    private val dispatcher = StandardTestDispatcher()
    private val profiles = FakeProfileRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.app() = AppContainer(FixedTimeProvider(LocalDateTime(2026, 10, 5, 19, 0)), applicationScope = backgroundScope)

    private fun <T> TestScope.collect(flow: Flow<T>): List<T> {
        val values = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.toList(values) }
        return values
    }

    private fun TestScope.settle() {
        advanceTimeBy(30_000)
        testScheduler.runCurrent()
    }

    private fun signInViewModel(app: AppContainer): AuthViewModel =
        AuthViewModel(app.authRepository, profiles, app.validateCredentials).apply {
            onEvent(AuthEvent.EmailChanged(InMemoryVisionFitStore.DEMO_EMAIL))
            onEvent(AuthEvent.PasswordChanged(InMemoryVisionFitStore.DEMO_PASSWORD))
        }

    @Test
    fun signInWaitsForTheProfileBeforeLeaving() = runTest(dispatcher) {
        val auth = signInViewModel(app())
        val effects = collect(auth.effects)

        auth.onEvent(AuthEvent.Submit)
        settle()
        assertTrue(auth.state.value.isSubmitting, "still downloading the profile")
        assertTrue(effects.isEmpty())

        profiles.status.value = ProfileStatus.READY
        settle()
        assertFalse(auth.state.value.isSubmitting)
        assertEquals(listOf<AuthEffect>(AuthEffect.NavigateToDashboard), effects)
    }

    @Test
    fun signInWithoutAProfileResumesOnboarding() = runTest(dispatcher) {
        profiles.status.value = ProfileStatus.MISSING
        val auth = signInViewModel(app())
        val effects = collect(auth.effects)

        auth.onEvent(AuthEvent.Submit)
        settle()

        assertEquals(listOf<AuthEffect>(AuthEffect.NavigateToOnboarding), effects)
    }

    @Test
    fun dashboardWaitsForTheProfileAndOffersARetryWhenItFails() = runTest(dispatcher) {
        val app = app()
        val dashboard = DashboardViewModel(
            app.authRepository, profiles, app.mealRepository, app.networkMonitor, app.timeProvider,
            app.calculateNutritionTarget, app.summarizeDay, app.calculateStreak,
        )
        settle()
        assertTrue(dashboard.state.value.isLoading)

        profiles.status.value = ProfileStatus.FAILED
        settle()
        assertTrue(dashboard.state.value.isProfileFailed)

        dashboard.onEvent(DashboardEvent.RetryProfileClicked)
        settle()
        assertEquals(1, profiles.refreshes)
        assertFalse(dashboard.state.value.isProfileFailed)
    }

    @Test
    fun profileEditorHidesTheDefaultsUntilTheSavedProfileArrives() = runTest(dispatcher) {
        val app = app()
        val editor = OnboardingViewModel(profiles, app.authRepository, app.calculateNutritionTarget, isEditing = true)
        settle()
        assertFalse(editor.state.value.isFormVisible)

        editor.onEvent(OnboardingEvent.Submit)
        settle()
        assertTrue(profiles.saved.isEmpty(), "the defaults must never overwrite the real profile")

        val saved = BodyProfile.Default.copy(ageYears = 41, weightKg = 90)
        profiles.profile.value = saved
        profiles.status.value = ProfileStatus.READY
        settle()
        assertTrue(editor.state.value.isFormVisible)
        assertEquals(saved, editor.state.value.profile)

        editor.onEvent(OnboardingEvent.MetricStepped(BodyMetric.WEIGHT, -1))
        editor.onEvent(OnboardingEvent.Submit)
        settle()
        assertEquals(listOf(saved.copy(weightKg = 89)), profiles.saved)
    }

    @Test
    fun signingOutFromTheProfileEditorReturnsToSignIn() = runTest(dispatcher) {
        val app = app()
        app.authRepository.login(InMemoryVisionFitStore.DEMO_EMAIL, InMemoryVisionFitStore.DEMO_PASSWORD)
        profiles.status.value = ProfileStatus.READY
        val editor = OnboardingViewModel(profiles, app.authRepository, app.calculateNutritionTarget, isEditing = true)
        val effects = collect(editor.effects)

        editor.onEvent(OnboardingEvent.LogoutClicked)
        settle()

        assertEquals(null, app.authRepository.session.value)
        assertEquals(listOf<OnboardingEffect>(OnboardingEffect.NavigateToAuth), effects)
    }

    private class FakeProfileRepository : ProfileRepository {
        override val profile = MutableStateFlow(BodyProfile.Default)
        override val status = MutableStateFlow(ProfileStatus.LOADING)
        val saved = mutableListOf<BodyProfile>()
        var refreshes = 0

        override suspend fun saveProfile(profile: BodyProfile): Boolean {
            saved += profile
            this.profile.value = profile
            return true
        }

        override suspend fun refresh() {
            refreshes++
            status.value = ProfileStatus.LOADING
        }
    }
}
