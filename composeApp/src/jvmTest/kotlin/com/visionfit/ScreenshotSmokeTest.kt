package com.visionfit

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.domain.model.AnalysisStage
import com.visionfit.domain.model.MealType
import com.visionfit.presentation.analysis.AnalysisScreen
import com.visionfit.presentation.auth.AuthScreen
import com.visionfit.presentation.camera.CameraScreen
import com.visionfit.presentation.camera.CameraUiState
import com.visionfit.presentation.camera.CaptureMode
import kotlinx.datetime.LocalTime
import com.visionfit.presentation.dashboard.DashboardScreen
import com.visionfit.presentation.preview.PreviewFixtures
import com.visionfit.presentation.preview.VisionFitPreview
import com.visionfit.presentation.history.HistoryScreen
import com.visionfit.presentation.goal.GoalScreen
import com.visionfit.presentation.goal.GoalUiState
import com.visionfit.presentation.review.ReviewScreen
import com.visionfit.presentation.onboarding.OnboardingScreen
import com.visionfit.presentation.onboarding.OnboardingUiState
import com.visionfit.presentation.auth.AuthUiState
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders every stateless screen with its preview state off-screen. Fails if a screen throws
 * while composing, and writes PNGs to `build/screenshots` for a visual check against the design.
 */
class ScreenshotSmokeTest {

    private val outputDir = File("build/screenshots").apply { mkdirs() }

    @Test
    fun authLogin() = snapshot("01-auth-login") {
        AuthScreen(state = AuthUiState(), onEvent = {})
    }

    @Test
    fun onboarding() = snapshot("02-onboarding") {
        val profile = BodyProfile.Default
        OnboardingScreen(OnboardingUiState(profile, CalculateNutritionTargetUseCase()(profile)), onEvent = {})
    }

    @Test
    fun dashboard() = snapshot("04-dashboard", heightDp = 1330) {
        DashboardScreen(PreviewFixtures.dashboard(), onEvent = {})
    }

    @Test
    fun dashboardOfflineOverTarget() = snapshot("04b-dashboard-offline-over", heightDp = 1500) {
        DashboardScreen(PreviewFixtures.dashboard(offline = true, overTarget = true), onEvent = {})
    }

    @Test
    fun dashboardEmptyDay() = snapshot("04c-dashboard-empty") {
        DashboardScreen(PreviewFixtures.dashboard(emptyDay = true), onEvent = {})
    }

    @Test
    fun camera() = snapshot("05-camera") {
        CameraScreen(
            CameraUiState(
                previewFrame = PreviewFixtures.trayPhoto,
                libraryPhotos = listOf(PreviewFixtures.comTamPhoto),
                mealType = MealType.DINNER,
                time = LocalTime(18, 40),
            ),
            onEvent = {},
        )
    }

    @Test
    fun cameraLibrary() = snapshot("05b-camera-library") {
        val photos = listOf(PreviewFixtures.comTamPhoto, PreviewFixtures.trayPhoto, PreviewFixtures.milkTeaPhoto, PreviewFixtures.banhMiPhoto)
        CameraScreen(
            CameraUiState(mode = CaptureMode.LIBRARY, libraryPhotos = photos, selectedLibraryPhoto = photos[1], mealType = MealType.DINNER, time = LocalTime(18, 40)),
            onEvent = {},
        )
    }

    @Test
    fun analysisRecognizing() = snapshot("06-analysis-recognizing") {
        AnalysisScreen(PreviewFixtures.analysis(AnalysisStage.RECOGNIZING), onEvent = {})
    }

    @Test
    fun analysisCompleted() = snapshot("06b-analysis-completed") {
        AnalysisScreen(PreviewFixtures.analysis(AnalysisStage.COMPLETED), onEvent = {})
    }

    @Test
    fun analysisFailed() = snapshot("06c-analysis-failed") {
        AnalysisScreen(PreviewFixtures.analysis(AnalysisStage.FAILED), onEvent = {})
    }

    @Test
    fun review() = snapshot("07-review", heightDp = 1310) {
        ReviewScreen(PreviewFixtures.review(), onEvent = {})
    }

    @Test
    fun reviewManualEmpty() = snapshot("07b-review-manual-empty") {
        ReviewScreen(PreviewFixtures.review(manual = true), onEvent = {})
    }

    @Test
    fun history() = snapshot("08-history", heightDp = 1320) {
        HistoryScreen(PreviewFixtures.history(), onEvent = {})
    }

    @Test
    fun historyEmptyDay() = snapshot("08b-history-empty") {
        HistoryScreen(PreviewFixtures.history(emptyPastDay = true), onEvent = {})
    }

    @Test
    fun goal() = snapshot("03-goal") {
        GoalScreen(GoalUiState(PreviewFixtures.target), onEvent = {})
    }

    private fun snapshot(name: String, heightDp: Int = 844, content: @Composable () -> Unit) {
        val scale = 2f
        val scene = ImageComposeScene(
            width = (390 * scale).toInt(),
            height = (heightDp * scale).toInt(),
            density = Density(scale),
        ) {
            VisionFitPreview(content)
        }
        try {
            // Let resources load (real time) and entrance animations finish (frame time).
            var frameTime = 0L
            repeat(40) {
                scene.render(frameTime)
                frameTime += 100_000_000L
                Thread.sleep(25)
            }
            val image = scene.render(frameTime + 5_000_000_000L)
            val bytes = image.encodeToData(EncodedImageFormat.PNG)?.bytes
            assertTrue(bytes != null && bytes.isNotEmpty(), "Nothing rendered for $name")
            File(outputDir, "$name.png").writeBytes(bytes)
        } finally {
            scene.close()
        }
    }
}
