package com.visionfit.data.repository

import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.DiaryStatus
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.MealEntry
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.mealTitleFor
import com.visionfit.domain.repository.MealRepository
import com.visionfit.domain.usecase.CalculateStreakUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

internal class MockMealRepository(
    private val store: InMemoryVisionFitStore,
    private val latencyMillis: Long = 600,
) : MealRepository {

    private val calculateStreak = CalculateStreakUseCase()

    /** The store holds every day already, so the diary is ready from the start. */
    override val status: StateFlow<DiaryStatus> = MutableStateFlow(DiaryStatus.READY)

    override suspend fun refresh() = Unit

    override fun observeDay(date: LocalDate): Flow<DayLog> =
        store.meals
            .map { meals -> dayLog(date, meals) }
            .distinctUntilChanged()

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayLog>> =
        store.meals
            .map { meals ->
                generateSequence(from) { it.plus(1, DateTimeUnit.DAY) }
                    .takeWhile { it <= to }
                    .map { dayLog(it, meals) }
                    .toList()
            }
            .distinctUntilChanged()

    /** What meal-service computes, done here on the demo data. */
    override fun observeStreak(today: LocalDate): Flow<Int> =
        store.meals
            .map { meals ->
                val days = meals.filterIsInstance<ConfirmedMeal>().groupBy { it.loggedAt.date }.map { (date, dayMeals) -> DayLog(date, dayMeals) }
                calculateStreak(days, today)
            }
            .distinctUntilChanged()

    override suspend fun saveMeal(
        id: String,
        jobId: String?,
        type: MealType,
        loggedAt: LocalDateTime,
        photo: MealPhoto?,
        items: List<FoodItem>,
        isEdited: Boolean,
    ): String {
        require(items.isNotEmpty()) { "A meal needs at least one dish" }
        delay(latencyMillis)
        // Same rule as meal-service: a retry with the same id keeps the first meal.
        if (store.meals.value.any { it is ConfirmedMeal && it.id == id }) return id
        val meal = ConfirmedMeal(
            id = id,
            type = type,
            loggedAt = loggedAt,
            photo = photo,
            title = mealTitleFor(items),
            items = items,
            isEdited = isEdited,
        )
        store.meals.update { meals ->
            meals.filterNot { it is PendingMeal && it.jobId == jobId } + meal
        }
        return meal.id
    }

    override fun observeMeal(id: String): Flow<ConfirmedMeal?> =
        store.meals
            .map { meals -> meals.firstOrNull { it is ConfirmedMeal && it.id == id } as ConfirmedMeal? }
            .distinctUntilChanged()

    override suspend fun updateMeal(id: String, type: MealType, items: List<FoodItem>) {
        require(items.isNotEmpty()) { "A meal needs at least one dish" }
        delay(latencyMillis)
        store.meals.update { meals ->
            meals.map { meal ->
                if (meal is ConfirmedMeal && meal.id == id) {
                    // A new meal, not `copy`: the totals must follow the new dishes.
                    ConfirmedMeal(meal.id, type, meal.loggedAt, meal.photo, mealTitleFor(items), items, isEdited = true)
                } else {
                    meal
                }
            }
        }
    }

    override suspend fun deleteMeal(id: String) {
        delay(latencyMillis)
        store.meals.update { meals -> meals.filterNot { it is ConfirmedMeal && it.id == id } }
    }

    /** Newest first, like the diary timeline. */
    private fun dayLog(date: LocalDate, meals: List<MealEntry>) = DayLog(
        date = date,
        meals = meals.filter { it.loggedAt.date == date }.sortedByDescending { it.loggedAt },
    )
}
