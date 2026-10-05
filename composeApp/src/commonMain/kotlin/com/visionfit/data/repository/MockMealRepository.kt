package com.visionfit.data.repository

import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.MealEntry
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.mealTitleFor
import com.visionfit.domain.repository.MealRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

    private var nextMealNumber = 1

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

    override suspend fun saveMeal(
        jobId: String?,
        type: MealType,
        loggedAt: LocalDateTime,
        photo: MealPhoto?,
        items: List<FoodItem>,
        isEdited: Boolean,
    ): String {
        require(items.isNotEmpty()) { "A meal needs at least one dish" }
        delay(latencyMillis)
        val meal = ConfirmedMeal(
            id = "meal-saved-${nextMealNumber++}",
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

    /** Newest first, like the diary timeline. */
    private fun dayLog(date: LocalDate, meals: List<MealEntry>) = DayLog(
        date = date,
        meals = meals.filter { it.loggedAt.date == date }.sortedByDescending { it.loggedAt },
    )
}
