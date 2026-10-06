package com.visionfit

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.visionfit.domain.model.AnalysisStage
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.MealType
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.presentation.analysis.AnalysisScreen
import com.visionfit.presentation.auth.AuthMode
import com.visionfit.presentation.auth.AuthScreen
import com.visionfit.presentation.auth.AuthUiState
import com.visionfit.presentation.camera.CameraScreen
import com.visionfit.presentation.camera.CameraUiState
import com.visionfit.presentation.dashboard.DashboardScreen
import com.visionfit.presentation.goal.GoalScreen
import com.visionfit.presentation.goal.GoalUiState
import com.visionfit.presentation.history.HistoryScreen
import com.visionfit.presentation.onboarding.OnboardingScreen
import com.visionfit.presentation.onboarding.OnboardingUiState
import com.visionfit.presentation.preview.PreviewFixtures
import com.visionfit.presentation.preview.VisionFitPreview
import com.visionfit.presentation.review.ReviewScreen
import kotlinx.datetime.LocalTime
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders every screen on a matrix of real window sizes (small phone, phone landscape, tablet,
 * desktop) and with large text into `build/screenshots/responsive/<device>/`. Fails if a screen
 * throws at any size.
 */
class ResponsiveScreenshotTest {

    private enum class Device(val widthDp: Int, val heightDp: Int, val fontScale: Float = 1f) {
        SMALL_PHONE(320, 640),

        /** A common Android phone (1080 × 2340 at 450 dpi = 384 × 832) minus status and 3-button navigation bars. */
        ANDROID_PHONE(384, 760),
        PHONE_LANDSCAPE(844, 390),
        PHONE_LARGE_TEXT(390, 844, fontScale = 1.3f),
        TABLET_PORTRAIT(834, 1194),
        TABLET_LANDSCAPE(1194, 834),
        DESKTOP(1280, 800),
    }

    private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        "01-auth" to { AuthScreen(AuthUiState(), onEvent = {}) },
        "01b-auth-register" to { AuthScreen(AuthUiState(mode = AuthMode.REGISTER), onEvent = {}) },
        "02-onboarding" to {
            val profile = BodyProfile.Default
            OnboardingScreen(OnboardingUiState(profile, CalculateNutritionTargetUseCase()(profile)), onEvent = {})
        },
        "03-goal" to { GoalScreen(GoalUiState(PreviewFixtures.target), onEvent = {}) },
        "04-dashboard" to { DashboardScreen(PreviewFixtures.dashboard(), onEvent = {}) },
        "05-camera" to {
            CameraScreen(
                CameraUiState(
                    previewFrame = PreviewFixtures.trayPhoto,
                    libraryPhotos = listOf(PreviewFixtures.comTamPhoto),
                    mealType = MealType.DINNER,
                    time = LocalTime(18, 40),
                ),
                onEvent = {},
            )
        },
        "06-analysis" to { AnalysisScreen(PreviewFixtures.analysis(AnalysisStage.COMPLETED), onEvent = {}) },
        "07-review" to { ReviewScreen(PreviewFixtures.review(), onEvent = {}) },
        "08-history" to { HistoryScreen(PreviewFixtures.history(), onEvent = {}) },
    )

    @Test
    fun smallPhone() = renderAll(Device.SMALL_PHONE)

    @Test
    fun androidPhone() = renderAll(Device.ANDROID_PHONE)

    @Test
    fun phoneLandscape() = renderAll(Device.PHONE_LANDSCAPE)

    @Test
    fun phoneLargeText() = renderAll(Device.PHONE_LARGE_TEXT)

    @Test
    fun tabletPortrait() = renderAll(Device.TABLET_PORTRAIT)

    @Test
    fun tabletLandscape() = renderAll(Device.TABLET_LANDSCAPE)

    @Test
    fun desktop() = renderAll(Device.DESKTOP)

    private fun renderAll(device: Device) {
        val dir = File("build/screenshots/responsive/${device.name.lowercase()}").apply { mkdirs() }
        screens.forEach { (name, content) -> snapshot(File(dir, "$name.png"), device, content) }
    }

    private fun snapshot(file: File, device: Device, content: @Composable () -> Unit) {
        val scale = 1.5f
        val scene = ImageComposeScene(
            width = (device.widthDp * scale).toInt(),
            height = (device.heightDp * scale).toInt(),
            density = Density(scale, device.fontScale),
        ) {
            VisionFitPreview(content)
        }
        try {
            var frameTime = 0L
            repeat(30) {
                scene.render(frameTime)
                frameTime += 100_000_000L
                Thread.sleep(20)
            }
            val bytes = scene.render(frameTime + 5_000_000_000L).encodeToData(EncodedImageFormat.PNG)?.bytes
            assertTrue(bytes != null && bytes.isNotEmpty(), "Nothing rendered for ${file.name} on $device")
            file.writeBytes(bytes)
        } finally {
            scene.close()
        }
    }
}
