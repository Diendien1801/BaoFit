package com.visionfit.data.mock

import com.visionfit.core.time.TimeProvider
import com.visionfit.domain.model.AnalysisFailure
import com.visionfit.domain.model.AnalysisJob
import com.visionfit.domain.model.AnalysisStage
import com.visionfit.domain.model.AnalysisTimings
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DetectedFood
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.MealEntry
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.PendingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlin.time.Duration.Companion.milliseconds

internal data class MockAccount(
    val userId: String,
    val email: String,
    val password: String,
    val displayName: String,
    val isLocked: Boolean = false,
)

/**
 * The fake backend shared by every mock repository: one place holds meals, analysis jobs,
 * the profile and accounts so the repositories stay consistent with each other (saving a
 * reviewed meal replaces its pending diary entry, a finished job updates that entry, …).
 *
 * Seeded relative to "today" so the demo always looks like the design: 1.663 kcal eaten,
 * a dinner tray being analyzed and a 6-day streak with two days over target.
 */
internal class InMemoryVisionFitStore(timeProvider: TimeProvider) {

    val accounts = MutableStateFlow(
        listOf(
            MockAccount("user-an", DEMO_EMAIL, DEMO_PASSWORD, displayName = "An"),
            MockAccount("user-locked", LOCKED_EMAIL, DEMO_PASSWORD, displayName = "Khoa", isLocked = true),
        ),
    )

    val profile = MutableStateFlow(BodyProfile.Default)

    val meals: MutableStateFlow<List<MealEntry>>

    val jobs: MutableStateFlow<Map<String, AnalysisJob>>

    /** The answer the mock AI will give for each job once it finishes. */
    val aiResults = MutableStateFlow<Map<String, List<DetectedFood>>>(emptyMap())

    init {
        val today = timeProvider.today()
        val seed = Seed(today)
        meals = MutableStateFlow(seed.meals())
        jobs = MutableStateFlow(seed.jobs().associateBy { it.id })
        aiResults.value = seed.jobs().associate { it.id to MockAiCatalog.resultFor(it.photo, it.id) }
    }

    companion object {
        const val DEMO_EMAIL = "an@visionfit.vn"
        const val DEMO_PASSWORD = "visionfit123"
        const val LOCKED_EMAIL = "locked@visionfit.vn"
    }
}

private class Seed(private val today: LocalDate) {

    private val dinnerJobId = "job-dinner-$today"
    private val failedJobId = "job-late-snack-${daysAgo(1)}"

    fun jobs(): List<AnalysisJob> = listOf(
        AnalysisJob(
            id = dinnerJobId,
            mealType = MealType.DINNER,
            capturedAt = at(today, 18, 40),
            photo = MockPhotos.FamilyTray,
            photoSizeBytes = MockPhotos.compressedSizeBytes(MockPhotos.FamilyTray),
            stage = AnalysisStage.RECOGNIZING,
            timings = AnalysisTimings(upload = 600.milliseconds, queue = 100.milliseconds),
        ),
        AnalysisJob(
            id = failedJobId,
            mealType = MealType.SNACK,
            capturedAt = at(daysAgo(1), 21, 15),
            photo = MockPhotos.MilkTea,
            photoSizeBytes = MockPhotos.compressedSizeBytes(MockPhotos.MilkTea),
            stage = AnalysisStage.FAILED,
            timings = AnalysisTimings(upload = 800.milliseconds, queue = 200.milliseconds),
            failure = AnalysisFailure.INVALID_AI_RESPONSE,
        ),
    )

    fun meals(): List<MealEntry> = todayMeals() + pastMeals()

    private fun todayMeals(): List<MealEntry> = listOf(
        confirmed(
            id = "meal-breakfast-$today",
            type = MealType.BREAKFAST,
            at = at(today, 7, 30),
            photo = MockPhotos.BanhMi,
            title = "Bánh mì, trứng ốp la & giăm bông",
            items = listOf(
                portion("$today-b1", "Bánh mì nguyên cám", 140, 230.0, 8.0, 40.0, 3.0, confidence = 91),
                portion("$today-b2", "Trứng ốp la", 55, 100.0, 6.5, 0.5, 8.0, confidence = 96),
                portion("$today-b3", "Giăm bông & xúc xích", 70, 121.0, 3.5, 11.5, 8.0, confidence = 83),
            ),
        ),
        confirmed(
            id = "meal-lunch-$today",
            type = MealType.LUNCH,
            at = at(today, 12, 10),
            photo = MockPhotos.ComTam,
            title = "Cơm tấm sườn nướng",
            isEdited = true,
            items = listOf(
                portion("$today-l1", "Cơm tấm", 200, 260.0, 5.0, 57.0, 0.6, confidence = 90),
                portion("$today-l2", "Sườn nướng", 160, 432.0, 33.0, 8.0, 26.0, confidence = 94),
                portion("$today-l3", "Đồ chua & mỡ hành", 60, 130.0, 4.0, 31.0, 3.4),
            ),
        ),
        confirmed(
            id = "meal-snack-$today",
            type = MealType.SNACK,
            at = at(today, 15, 20),
            photo = MockPhotos.MilkTea,
            title = "Trà sữa trân châu",
            items = listOf(portion("$today-s1", "Trà sữa trân châu", 500, 390.0, 4.0, 62.0, 14.0, confidence = 88)),
        ),
        PendingMeal(
            id = "meal-dinner-$today",
            type = MealType.DINNER,
            loggedAt = at(today, 18, 40),
            photo = MockPhotos.FamilyTray,
            jobId = dinnerJobId,
            status = PendingStatus.ANALYZING,
        ),
    )

    /**
     * Six completed days (the streak). Two go over the 2.034 kcal target. Past meals have no
     * photo on purpose to exercise the placeholder, and yesterday holds a failed analysis and
     * a very long dish name.
     */
    private fun pastMeals(): List<MealEntry> = buildList {
        day(1) {
            meal(MealType.BREAKFAST, 7, 5, "Phở bò tái", 520, 32, 62, 14)
            meal(MealType.LUNCH, 12, 0, "Bún bò Huế đặc biệt thêm giò heo, chả cua và rau sống ăn kèm", 680, 38, 74, 24, edited = true)
            meal(MealType.SNACK, 16, 0, "Sữa chua nếp cẩm", 185, 6, 32, 4)
            meal(MealType.DINNER, 19, 10, "Cá kho tộ, canh chua & cơm trắng", 600, 36, 72, 16)
        }
        add(
            PendingMeal(
                id = "meal-late-snack-${daysAgo(1)}",
                type = MealType.SNACK,
                loggedAt = at(daysAgo(1), 21, 15),
                photo = MockPhotos.MilkTea,
                jobId = failedJobId,
                status = PendingStatus.FAILED,
            ),
        )
        day(2) {
            meal(MealType.BREAKFAST, 8, 0, "Xôi gà", 480, 22, 70, 12)
            meal(MealType.LUNCH, 12, 30, "Cơm gà xối mỡ", 860, 40, 92, 36)
            meal(MealType.SNACK, 16, 30, "Bánh tráng trộn", 350, 8, 48, 14)
            meal(MealType.DINNER, 20, 0, "Lẩu thái hải sản", 720, 46, 54, 34)
        }
        day(3) {
            meal(MealType.BREAKFAST, 7, 45, "Bánh cuốn chả lụa", 390, 16, 58, 10)
            meal(MealType.LUNCH, 12, 15, "Cơm văn phòng: gà rang gừng, rau muống xào", 750, 38, 96, 22)
            meal(MealType.DINNER, 19, 0, "Salad ức gà sốt mè rang", 580, 48, 30, 28)
        }
        day(4) {
            meal(MealType.BREAKFAST, 7, 20, "Hủ tiếu Nam Vang", 540, 28, 72, 14)
            meal(MealType.LUNCH, 11, 50, "Cơm tấm bì chả", 790, 34, 98, 28)
            meal(MealType.SNACK, 15, 40, "Chè ba màu", 260, 4, 54, 3)
            meal(MealType.DINNER, 19, 30, "Gỏi cuốn tôm thịt (4 cuốn)", 370, 24, 52, 6)
        }
        day(5) {
            meal(MealType.BREAKFAST, 6, 50, "Bánh mì thịt nguội", 520, 22, 60, 20)
            meal(MealType.LUNCH, 12, 20, "Bún chả Hà Nội", 710, 34, 82, 26)
            meal(MealType.SNACK, 15, 10, "Trà sữa matcha", 390, 4, 60, 15)
            meal(MealType.DINNER, 20, 15, "Pizza hải sản (3 miếng)", 670, 30, 72, 28)
        }
        day(6) {
            meal(MealType.BREAKFAST, 7, 0, "Cháo gà xé phay", 350, 24, 46, 6)
            meal(MealType.LUNCH, 12, 5, "Cơm sườn ram mặn", 760, 36, 94, 26)
            meal(MealType.DINNER, 18, 50, "Mì Quảng tôm thịt", 560, 30, 70, 16)
            meal(MealType.SNACK, 21, 0, "Sinh tố bơ ít đường", 180, 3, 18, 11)
        }
    }

    private fun MutableList<MealEntry>.day(daysBack: Int, block: DayBuilder.() -> Unit) {
        addAll(DayBuilder(daysAgo(daysBack)).apply(block).meals)
    }

    private inner class DayBuilder(private val date: LocalDate) {
        val meals = mutableListOf<MealEntry>()

        fun meal(
            type: MealType,
            hour: Int,
            minute: Int,
            title: String,
            kcal: Int,
            proteinG: Int,
            carbsG: Int,
            fatG: Int,
            edited: Boolean = false,
        ) {
            val id = "meal-$date-${meals.size}"
            meals += confirmed(
                id = id,
                type = type,
                at = at(date, hour, minute),
                photo = null,
                title = title,
                isEdited = edited,
                items = listOf(
                    portion("$id-1", title, 350, kcal.toDouble(), proteinG.toDouble(), carbsG.toDouble(), fatG.toDouble()),
                ),
            )
        }
    }

    private fun confirmed(
        id: String,
        type: MealType,
        at: LocalDateTime,
        photo: MealPhoto?,
        title: String,
        items: List<FoodItem>,
        isEdited: Boolean = false,
    ) = ConfirmedMeal(id, type, at, photo, title, items, isEdited)

    private fun daysAgo(days: Int): LocalDate = today.minus(days, DateTimeUnit.DAY)

    private fun at(date: LocalDate, hour: Int, minute: Int) = LocalDateTime(date, LocalTime(hour, minute))
}
