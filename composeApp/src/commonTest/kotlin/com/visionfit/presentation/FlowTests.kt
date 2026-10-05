package com.visionfit.presentation

import com.visionfit.core.time.FixedTimeProvider
import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.data.mock.MockPhotos
import com.visionfit.di.AppContainer
import com.visionfit.domain.model.AnalysisFailure
import com.visionfit.domain.model.AnalysisStage
import com.visionfit.domain.model.AuthError
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DayStatus
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.PendingStatus
import com.visionfit.domain.usecase.CredentialError
import com.visionfit.presentation.analysis.AnalysisEvent
import com.visionfit.presentation.analysis.AnalysisViewModel
import com.visionfit.presentation.auth.AuthEffect
import com.visionfit.presentation.auth.AuthEvent
import com.visionfit.presentation.auth.AuthViewModel
import com.visionfit.presentation.dashboard.DashboardViewModel
import com.visionfit.presentation.dashboard.Greeting
import com.visionfit.presentation.history.HistoryEvent
import com.visionfit.presentation.history.HistoryViewModel
import com.visionfit.presentation.review.ReviewEffect
import com.visionfit.presentation.review.ReviewEvent
import com.visionfit.presentation.review.ReviewIssue
import com.visionfit.presentation.review.ReviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FlowTests {

    private val dispatcher = StandardTestDispatcher()
    private val now = LocalDateTime(2026, 10, 5, 19, 0)
    private val today = now.date
    private val dinnerJobId = "job-dinner-$today"

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.app() = AppContainer(FixedTimeProvider(now), applicationScope = backgroundScope)

    private fun <T> TestScope.collect(flow: Flow<T>): List<T> {
        val values = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.toList(values) }
        return values
    }

    @Test
    fun seededDinnerIsAnalyzedReviewedAndSaved() = runTest(dispatcher) {
        val app = app()
        val analysis = AnalysisViewModel(dinnerJobId, app.mealAnalysisRepository)
        runCurrent()
        assertEquals(AnalysisStage.RECOGNIZING, analysis.state.value.stage)

        advanceTimeBy(3_600)
        assertEquals(AnalysisStage.CALCULATING, analysis.state.value.stage)
        assertEquals(4, analysis.state.value.job?.detections?.size)

        settle()
        assertEquals(AnalysisStage.COMPLETED, analysis.state.value.stage)
        assertEquals(819, analysis.state.value.job?.totalKcal)
        val pending = app.mealRepository.observeDay(today).first().pendingMeals.single()
        assertEquals(PendingStatus.READY_FOR_REVIEW, pending.status)

        val review = reviewViewModel(app, dinnerJobId)
        val effects = collect(review.effects)
        settle()
        with(review.state.value) {
            assertEquals(819, totalKcal)
            assertEquals(46, totalMacros.proteinG.toInt())
            assertEquals(371, remainingBeforeKcal)
            assertEquals(-448, remainingAfterKcal)
            assertTrue(isOverTarget)
        }

        // Doubling the weight rescales calories from the per-100 g value.
        val firstId = review.state.value.items.first().id
        review.onEvent(ReviewEvent.GramsChanged(firstId, "300"))
        assertEquals("690", review.state.value.items.first().kcalText)
        assertEquals(1_164, review.state.value.totalKcal)

        review.onEvent(ReviewEvent.Save)
        settle()
        assertEquals(ReviewEffect.MealSaved(MealType.DINNER, 1_164), effects.last())

        val day = app.mealRepository.observeDay(today).first()
        assertTrue(day.pendingMeals.isEmpty(), "the pending entry is replaced, not duplicated")
        val dinner = day.confirmedMeals.first()
        assertEquals(1_164, dinner.kcal)
        assertTrue(dinner.isEdited)
        assertEquals("Thịt kho, Đậu phụ sốt cà chua & 2 món khác", dinner.title)
    }

    @Test
    fun reviewValidatesBlankNamesAndSupportsUndo() = runTest(dispatcher) {
        val app = app()
        val analysis = AnalysisViewModel(dinnerJobId, app.mealAnalysisRepository)
        settle()
        assertEquals(AnalysisStage.COMPLETED, analysis.state.value.stage)

        val review = reviewViewModel(app, dinnerJobId)
        val effects = collect(review.effects)
        settle()

        val removedId = review.state.value.items[1].id
        review.onEvent(ReviewEvent.RemoveItem(removedId))
        assertEquals(3, review.state.value.items.size)
        review.onEvent(ReviewEvent.UndoRemove)
        assertEquals(removedId, review.state.value.items[1].id, "undo restores the dish at its position")

        review.onEvent(ReviewEvent.AddItem)
        review.onEvent(ReviewEvent.Save)
        settle()
        val added = review.state.value.items.last()
        assertEquals(ReviewEffect.ShowIssue(ReviewIssue.MISSING_NAME, itemNumber = 5), effects.last())
        assertEquals(setOf(added.id), review.state.value.invalidItemIds)
        assertTrue(app.mealRepository.observeDay(today).first().pendingMeals.isNotEmpty(), "nothing saved")
    }

    @Test
    fun everySecondCaptureFailsOnceThenRetrySucceeds() = runTest(dispatcher) {
        val app = app()
        val first = app.mealAnalysisRepository.submitPhoto(MockPhotos.ComTam, now)
        val second = app.mealAnalysisRepository.submitPhoto(MockPhotos.BanhMi, now)
        settle()

        assertEquals(AnalysisStage.COMPLETED, app.mealAnalysisRepository.observeJob(first).first()?.stage)
        val failed = app.mealAnalysisRepository.observeJob(second).first()
        assertEquals(AnalysisStage.FAILED, failed?.stage)
        assertEquals(AnalysisFailure.INVALID_AI_RESPONSE, failed?.failure)

        val analysis = AnalysisViewModel(second, app.mealAnalysisRepository)
        runCurrent()
        analysis.onEvent(AnalysisEvent.Retry)
        settle()
        assertEquals(AnalysisStage.COMPLETED, analysis.state.value.stage)
        assertEquals(4, analysis.state.value.job?.detections?.size)
    }

    @Test
    fun authValidatesAndSignsIn() = runTest(dispatcher) {
        val app = app()
        val auth = AuthViewModel(app.authRepository, app.validateCredentials)
        val effects = collect(auth.effects)

        auth.onEvent(AuthEvent.EmailChanged("an@visionfit"))
        auth.onEvent(AuthEvent.Submit)
        assertEquals(CredentialError.EMAIL_INVALID, auth.state.value.emailError)

        auth.onEvent(AuthEvent.EmailChanged(InMemoryVisionFitStore.DEMO_EMAIL))
        auth.onEvent(AuthEvent.PasswordChanged("wrong-password"))
        auth.onEvent(AuthEvent.Submit)
        assertTrue(auth.state.value.isSubmitting)
        settle()
        assertEquals(AuthError.INVALID_CREDENTIALS, auth.state.value.submitError)

        auth.onEvent(AuthEvent.PasswordChanged(InMemoryVisionFitStore.DEMO_PASSWORD))
        auth.onEvent(AuthEvent.Submit)
        settle()
        assertEquals(AuthEffect.NavigateToDashboard, effects.last())
        assertEquals("An", app.authRepository.session.value?.displayName)
    }

    @Test
    fun dashboardShowsTheDesignDay() = runTest(dispatcher) {
        val app = app()
        app.authRepository.login(InMemoryVisionFitStore.DEMO_EMAIL, InMemoryVisionFitStore.DEMO_PASSWORD)
        val dashboard = DashboardViewModel(
            app.authRepository, app.profileRepository, app.mealRepository, app.networkMonitor, app.timeProvider,
            app.calculateNutritionTarget, app.summarizeDay, app.calculateStreak,
        )
        settle()
        with(dashboard.state.value) {
            assertFalse(isLoading)
            assertEquals("AN", initials)
            assertEquals(Greeting.EVENING, greeting)
            assertEquals(1_663, summary?.eatenKcal)
            assertEquals(371, summary?.remainingKcal)
            assertEquals(6, streakDays)
            assertEquals(1, notificationCount)
            assertEquals(
                listOf(
                    DayStatus.WITHIN_TARGET, DayStatus.OVER_TARGET, DayStatus.WITHIN_TARGET, DayStatus.WITHIN_TARGET,
                    DayStatus.OVER_TARGET, DayStatus.WITHIN_TARGET, DayStatus.WITHIN_TARGET,
                ),
                week.map { it.status },
            )
            assertTrue(meals.first() is PendingMeal)
            assertTrue(meals.drop(1).all { it is ConfirmedMeal })
        }
    }

    @Test
    fun historySwitchesDaysAndHandlesEmptyDates() = runTest(dispatcher) {
        val app = app()
        val history = HistoryViewModel(
            app.mealRepository, app.profileRepository, app.networkMonitor, app.timeProvider,
            app.calculateNutritionTarget, app.summarizeDay,
        )
        settle()
        assertEquals(4, history.state.value.meals.size)
        assertEquals(7, history.state.value.dates.size)

        history.onEvent(HistoryEvent.DateSelected(LocalDate(2026, 10, 4)))
        settle()
        assertEquals(5, history.state.value.meals.size, "4 confirmed meals + 1 failed analysis")
        assertTrue(history.state.value.meals.any { it is PendingMeal && it.status == PendingStatus.FAILED })

        val longAgo = LocalDate(2026, 9, 1)
        history.onEvent(HistoryEvent.DatePicked(longAgo))
        settle()
        with(history.state.value) {
            assertEquals(longAgo, selectedDate)
            assertTrue(meals.isEmpty())
            assertEquals(longAgo, dates.last(), "a picked date gets its own chip")
            assertFalse(isDatePickerVisible)
        }
    }

    private fun reviewViewModel(app: AppContainer, jobId: String, manual: Boolean = false) = ReviewViewModel(
        jobId = jobId,
        isManualEntry = manual,
        analysisRepository = app.mealAnalysisRepository,
        mealRepository = app.mealRepository,
        profileRepository = app.profileRepository,
        calculateTarget = app.calculateNutritionTarget,
        summarizeDay = app.summarizeDay,
    )

    private fun TestScope.runCurrent() = testScheduler.runCurrent()

    /**
     * Lets every pending delay elapse. `advanceUntilIdle()` is not enough here: the analysis
     * simulation runs in `backgroundScope`, which it deliberately ignores.
     */
    private fun TestScope.settle() {
        advanceTimeBy(SETTLE_MILLIS)
        testScheduler.runCurrent()
    }

    private companion object {
        const val SETTLE_MILLIS = 30_000L
    }
}
