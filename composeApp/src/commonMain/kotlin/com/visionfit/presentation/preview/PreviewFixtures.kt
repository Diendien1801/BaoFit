package com.visionfit.presentation.preview

import com.visionfit.domain.model.AnalysisFailure
import com.visionfit.domain.model.AnalysisJob
import com.visionfit.domain.model.AnalysisStage
import com.visionfit.domain.model.AnalysisTimings
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.DetectedFood
import com.visionfit.domain.model.DetectionRegion
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.DayStatus
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.MealEntry
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.NutrientsPer100g
import com.visionfit.domain.model.NutritionTarget
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.PendingStatus
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.domain.usecase.SummarizeDayUseCase
import com.visionfit.presentation.analysis.AnalysisUiState
import com.visionfit.presentation.dashboard.DashboardUiState
import com.visionfit.presentation.history.HistoryUiState
import com.visionfit.presentation.review.EditableFood
import com.visionfit.presentation.review.ReviewUiState
import com.visionfit.presentation.dashboard.Greeting
import com.visionfit.presentation.dashboard.WeekDay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlin.time.Duration.Companion.milliseconds

/**
 * Deterministic UI states for `@Preview`s and the screenshot test, matching the design
 * (Monday 05/10, 1.663 / 2.034 kcal eaten, dinner tray being analyzed).
 */
internal object PreviewFixtures {

    val today: LocalDate = LocalDate(2026, 10, 5)

    val target: NutritionTarget = CalculateNutritionTargetUseCase()(BodyProfile.Default)

    val trayPhoto = MealPhoto("bundled://family_tray")
    val comTamPhoto = MealPhoto("bundled://com_tam")
    val milkTeaPhoto = MealPhoto("bundled://milk_tea")
    val banhMiPhoto = MealPhoto("bundled://banh_mi")

    const val DINNER_JOB_ID = "job-dinner-preview"

    fun at(hour: Int, minute: Int, date: LocalDate = today) = LocalDateTime(date, LocalTime(hour, minute))

    fun portion(name: String, grams: Int, kcal: Double, p: Double, c: Double, f: Double, confidence: Int? = 90): FoodItem {
        val factor = 100.0 / grams
        return FoodItem(
            id = "preview-$name",
            name = name,
            grams = grams,
            per100g = NutrientsPer100g(kcal * factor, p * factor, c * factor, f * factor),
            confidence = confidence,
        )
    }

    /** Newest first, as the repositories return them. */
    fun todayMeals(dinnerStatus: PendingStatus = PendingStatus.ANALYZING, extraFatMeal: Boolean = false): List<MealEntry> = buildList {
        add(PendingMeal("dinner", MealType.DINNER, at(18, 40), trayPhoto, DINNER_JOB_ID, dinnerStatus))
        if (extraFatMeal) {
            add(
                ConfirmedMeal(
                    "late-snack", MealType.SNACK, at(16, 45), null, "Bánh tráng nướng trứng cút, xúc xích & phô mai",
                    listOf(portion("Bánh tráng nướng", 220, 640.0, 18.0, 70.0, 32.0)), isEdited = false,
                ),
            )
        }
        add(
            ConfirmedMeal(
                "snack", MealType.SNACK, at(15, 20), milkTeaPhoto, "Trà sữa trân châu",
                listOf(portion("Trà sữa trân châu", 500, 390.0, 4.0, 62.0, 14.0)), isEdited = false,
            ),
        )
        add(
            ConfirmedMeal(
                "lunch", MealType.LUNCH, at(12, 10), comTamPhoto, "Cơm tấm sườn nướng",
                listOf(
                    portion("Cơm tấm", 200, 260.0, 5.0, 57.0, 0.6),
                    portion("Sườn nướng", 160, 432.0, 33.0, 8.0, 26.0),
                    portion("Đồ chua & mỡ hành", 60, 130.0, 4.0, 31.0, 3.4, confidence = null),
                ),
                isEdited = true,
            ),
        )
        add(
            ConfirmedMeal(
                "breakfast", MealType.BREAKFAST, at(7, 30), banhMiPhoto, "Bánh mì, trứng ốp la & giăm bông",
                listOf(
                    portion("Bánh mì nguyên cám", 140, 230.0, 8.0, 40.0, 3.0),
                    portion("Trứng ốp la", 55, 100.0, 6.5, 0.5, 8.0),
                    portion("Giăm bông & xúc xích", 70, 121.0, 3.5, 11.5, 8.0),
                ),
                isEdited = false,
            ),
        )
    }

    fun todayLog(emptyDay: Boolean = false, overTarget: Boolean = false) =
        DayLog(today, if (emptyDay) emptyList() else todayMeals(extraFatMeal = overTarget))

    fun week(): List<WeekDay> {
        val statuses = listOf(
            DayStatus.WITHIN_TARGET, DayStatus.OVER_TARGET, DayStatus.WITHIN_TARGET,
            DayStatus.WITHIN_TARGET, DayStatus.OVER_TARGET, DayStatus.WITHIN_TARGET,
        )
        return statuses.mapIndexed { index, status ->
            WeekDay(today.minus(6 - index, DateTimeUnit.DAY), status, isToday = false)
        } + WeekDay(today, DayStatus.WITHIN_TARGET, isToday = true)
    }

    fun dashboard(offline: Boolean = false, overTarget: Boolean = false, emptyDay: Boolean = false): DashboardUiState {
        val log = todayLog(emptyDay = emptyDay, overTarget = overTarget)
        return DashboardUiState(
            isLoading = false,
            userName = "An",
            initials = "AN",
            today = today,
            greeting = Greeting.EVENING,
            isOffline = offline,
            lastSyncedAt = LocalTime(18, 42),
            week = week(),
            streakDays = 6,
            summary = SummarizeDayUseCase()(log, target),
            meals = log.meals,
            notificationCount = log.pendingMeals.size,
        )
    }

    /** The dinner tray from the design: 4 dishes, 819 kcal, one low-confidence guess. */
    fun trayDetections(): List<DetectedFood> = listOf(
        DetectedFood(
            FoodItem("d1", "Thịt kho", 150, NutrientsPer100g(230.0, 14.0, 4.0, 17.0), 92),
            DetectionRegion(0.688f, 0.505f, 0.162f),
        ),
        DetectedFood(
            FoodItem("d2", "Đậu phụ sốt cà chua", 200, NutrientsPer100g(95.0, 7.0, 5.0, 5.5), 89),
            DetectionRegion(0.376f, 0.448f, 0.147f),
        ),
        DetectedFood(
            FoodItem("d3", "Khoai tây xào thịt", 180, NutrientsPer100g(130.0, 5.0, 17.0, 5.0), 78),
            DetectionRegion(0.312f, 0.693f, 0.176f),
        ),
        DetectedFood(
            FoodItem("d4", "Canh dưa chuột", 250, NutrientsPer100g(20.0, 0.8, 3.0, 0.5), 86),
            DetectionRegion(0.821f, 0.229f, 0.153f),
        ),
    )

    fun analysisJob(stage: AnalysisStage): AnalysisJob = AnalysisJob(
        id = DINNER_JOB_ID,
        mealType = MealType.DINNER,
        capturedAt = at(18, 40),
        photo = trayPhoto,
        photoSizeBytes = 1_200_000,
        stage = stage,
        timings = AnalysisTimings(
            upload = 600.milliseconds,
            queue = 100.milliseconds,
            recognition = 4_200.milliseconds.takeIf { stage == AnalysisStage.CALCULATING || stage == AnalysisStage.COMPLETED },
            calculation = 1_100.milliseconds.takeIf { stage == AnalysisStage.COMPLETED },
        ),
        detections = if (stage == AnalysisStage.CALCULATING || stage == AnalysisStage.COMPLETED) trayDetections() else emptyList(),
        failure = AnalysisFailure.INVALID_AI_RESPONSE.takeIf { stage == AnalysisStage.FAILED },
    )

    fun analysis(stage: AnalysisStage) = AnalysisUiState(isLoading = false, job = analysisJob(stage))

    /** Review of the dinner tray; 371 kcal were left before it, so it goes 448 kcal over. */
    fun review(manual: Boolean = false): ReviewUiState = ReviewUiState(
        isLoading = false,
        jobId = DINNER_JOB_ID,
        isManualEntry = manual,
        mealType = MealType.DINNER,
        loggedAt = at(18, 40),
        photo = trayPhoto,
        detectedCount = 4,
        items = if (manual) {
            emptyList()
        } else {
            trayDetections().mapIndexed { index, detected -> EditableFood.from(detected.item, index, detected.region) }
        },
        remainingBeforeKcal = 371,
    )

    fun history(emptyPastDay: Boolean = false): HistoryUiState {
        val dates = (0 until 7).map { today.minus(it, DateTimeUnit.DAY) }
        val selected = if (emptyPastDay) LocalDate(2026, 9, 12) else today
        val log = if (emptyPastDay) DayLog(selected, emptyList()) else todayLog()
        return HistoryUiState(
            isLoading = false,
            today = today,
            dates = if (emptyPastDay) dates + selected else dates,
            selectedDate = selected,
            summary = SummarizeDayUseCase()(log, target),
            meals = log.meals,
        )
    }
}
