package com.visionfit

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.visionfit.core.time.FixedTimeProvider
import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end through the real navigation graph, ViewModels and mock repositories:
 * sign in → open the dinner being analyzed → wait for the AI → review → save → back on the dashboard.
 */
@OptIn(ExperimentalTestApi::class)
class AppFlowUiTest {

    private val now = LocalDateTime(2026, 10, 5, 19, 0)

    @Test
    fun signInAnalyzeReviewAndSave() = runComposeUiTest {
        // Stickers and spinners animate forever, so the clock never idles on its own: drive it by hand.
        mainClock.autoAdvance = false
        val container = AppContainer(FixedTimeProvider(now))
        setContent { VisionFitApp(container) }
        mainClock.advanceTimeBy(FRAME_MILLIS)

        val fields = onAllNodes(hasSetTextAction())
        fields[0].performTextInput(InMemoryVisionFitStore.DEMO_EMAIL)
        fields[1].performTextInput(InMemoryVisionFitStore.DEMO_PASSWORD)
        onAllNodesWithText("Đăng nhập").onLast().performClick()

        waitForText("Chào buổi tối, An!")
        // The bell (1 result pending) opens the diary, where the dinner being analyzed is on top.
        onAllNodes(hasContentDescription("Thông báo", substring = true)).onFirst().performClick()
        waitForText("Nhật ký bữa ăn")
        waitForText("AI đang “nếm thử”…")
        onAllNodesWithText("AI đang “nếm thử”…").onFirst().performClick()

        waitForText("Xem & xác nhận kết quả", timeoutMillis = 15_000)
        onAllNodesWithText("Xem & xác nhận kết quả").onFirst().performClick()

        waitForText("Kiểm tra kết quả")
        waitForText("Ối, hơi quá tay rồi!")
        onAllNodesWithText("Xác nhận & lưu").onFirst().performClick()

        waitForText("Chào buổi tối, An!")
        val today = runBlocking { container.mealRepository.observeDay(now.date).first() }
        assertTrue(today.pendingMeals.isEmpty())
        assertEquals(1_663 + 819, today.confirmedMeals.sumOf { it.kcal })
        waitForText("4 bữa đã lưu")
    }

    private fun ComposeUiTest.waitForText(text: String, timeoutMillis: Long = 5_000) {
        waitUntil(conditionDescription = "text '$text' is shown", timeoutMillis = timeoutMillis) {
            mainClock.advanceTimeBy(FRAME_MILLIS)
            onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val FRAME_MILLIS = 100L
    }
}
