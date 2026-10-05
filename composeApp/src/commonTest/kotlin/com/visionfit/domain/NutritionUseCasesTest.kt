package com.visionfit.domain

import com.visionfit.domain.model.ActivityLevel
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.DayStatus
import com.visionfit.domain.model.FitnessGoal
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.NutrientsPer100g
import com.visionfit.domain.model.Sex
import com.visionfit.domain.model.mealTitleFor
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.domain.usecase.CalculateStreakUseCase
import com.visionfit.domain.usecase.SummarizeDayUseCase
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NutritionUseCasesTest {

    private val calculateTarget = CalculateNutritionTargetUseCase()
    private val summarize = SummarizeDayUseCase()
    private val today = LocalDate(2026, 10, 5)

    @Test
    fun defaultProfileMatchesTheDesignNumbers() {
        val target = calculateTarget(BodyProfile.Default)

        assertEquals(1_635, target.bmrKcal)
        assertEquals(2_534, target.tdeeKcal)
        assertEquals(-500, target.adjustmentKcal)
        assertEquals(2_034, target.dailyKcal)
        assertEquals(136, target.macros.proteinG)
        assertEquals(246, target.macros.carbsG)
        assertEquals(56, target.macros.fatG)
        assertEquals(27, target.macros.proteinPercent)
        assertEquals(48, target.macros.carbsPercent)
        assertEquals(25, target.macros.fatPercent)
    }

    @Test
    fun femaleMuscleGainAddsSurplusAndKeepsPercentagesAtHundred() {
        val profile = BodyProfile(Sex.FEMALE, 30, 160, 55, ActivityLevel.LIGHT, FitnessGoal.MUSCLE_GAIN)
        val target = calculateTarget(profile)

        // 10·55 + 6.25·160 − 5·30 − 161 = 1239
        assertEquals(1_239, target.bmrKcal)
        assertEquals((1_239 * 1.375 + 300).roundToInt(), target.dailyKcal)
        assertEquals(110, target.macros.proteinG)
        with(target.macros) { assertEquals(100, proteinPercent + carbsPercent + fatPercent) }
    }

    @Test
    fun daySummaryOnlyCountsConfirmedMeals() {
        val log = DayLog(today, designMeals())
        val summary = summarize(log, calculateTarget(BodyProfile.Default))

        assertEquals(1_663, summary.eatenKcal)
        assertEquals(371, summary.remainingKcal)
        assertEquals(82, round(summary.fraction * 100).toInt())
        assertEquals(64, summary.eatenMacros.proteinG.roundToInt())
        assertEquals(210, summary.eatenMacros.carbsG.roundToInt())
        assertEquals(63, summary.eatenMacros.fatG.roundToInt())
        assertTrue(summary.fat.isOver)
        assertEquals(112, round(summary.fat.fraction * 100).toInt(), "112.5 % rounds half-to-even like the design")
        assertFalse(summary.carbs.isOver)
        assertEquals(DayStatus.WITHIN_TARGET, summarize.status(log, calculateTarget(BodyProfile.Default)))
    }

    @Test
    fun emptyDayHasNoDataStatus() {
        val target = calculateTarget(BodyProfile.Default)
        assertEquals(DayStatus.NO_DATA, summarize.status(DayLog(today, emptyList()), target))
    }

    @Test
    fun streakCountsConsecutiveCompletedDaysBeforeToday() {
        val streak = CalculateStreakUseCase()
        val days = (1..6).map { DayLog(today.minus(it, DateTimeUnit.DAY), listOf(meal("d$it", 500.0))) } +
            DayLog(today.minus(8, DateTimeUnit.DAY), listOf(meal("old", 500.0)))

        assertEquals(6, streak(days, today))
        assertEquals(0, streak(days.drop(1), today), "yesterday missing breaks the streak")
    }

    @Test
    fun mealTitleSummarizesDishes() {
        val items = listOf("Thịt kho", "Đậu phụ", "Khoai tây", "Canh").map { item(it, 100.0) }
        assertEquals("Thịt kho", mealTitleFor(items.take(1)))
        assertEquals("Thịt kho & Đậu phụ", mealTitleFor(items.take(2)))
        assertEquals("Thịt kho, Đậu phụ & Khoai tây", mealTitleFor(items.take(3)))
        assertEquals("Thịt kho, Đậu phụ & 2 món khác", mealTitleFor(items))
    }

    private fun designMeals() = listOf(
        meal("breakfast", kcal = 451.0, p = 18.0, c = 52.0, f = 19.0),
        meal("lunch", kcal = 822.0, p = 42.0, c = 96.0, f = 30.0),
        meal("snack", kcal = 390.0, p = 4.0, c = 62.0, f = 14.0),
    )

    private fun meal(id: String, kcal: Double, p: Double = 0.0, c: Double = 0.0, f: Double = 0.0) = ConfirmedMeal(
        id = id,
        type = MealType.LUNCH,
        loggedAt = LocalDateTime(today, LocalTime(12, 0)),
        photo = null,
        title = id,
        items = listOf(FoodItem("$id-1", id, 100, NutrientsPer100g(kcal, p, c, f), confidence = 90)),
        isEdited = false,
    )

    private fun item(name: String, kcal: Double) = FoodItem(name, name, 100, NutrientsPer100g(kcal, 0.0, 0.0, 0.0), 90)
}
