package com.visionfit.data.remote.dto

import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.NutrientsPer100g
import com.visionfit.domain.model.mealTitleFor
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

// Bodies of meal-service's `/api/v1/meals`. MealType travels by name ("LUNCH"), as the server expects.

@Serializable
internal data class Per100gDto(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)

/** `POST /api/v1/meals`. No default values: every field is always sent, `null` included. */
@Serializable
internal data class SaveMealRequest(
    val jobId: String?,
    val type: MealType,
    val loggedAt: LocalDateTime,
    val photoUrl: String?,
    val isEdited: Boolean,
    val items: List<SaveMealItem>,
)

/** `PUT /api/v1/meals/{id}`: the time, photo and job of a saved meal never change. */
@Serializable
internal data class UpdateMealRequest(
    val type: MealType,
    val items: List<SaveMealItem>,
)

@Serializable
internal data class SaveMealItem(
    val name: String,
    val grams: Int,
    val per100g: Per100gDto,
    val confidence: Int?,
)

/** A saved meal as `GET` and `POST` return it. */
@Serializable
internal data class MealDto(
    val id: String,
    val jobId: String? = null,
    val type: MealType,
    val loggedAt: LocalDateTime,
    val photoUrl: String? = null,
    val isEdited: Boolean,
    val items: List<MealItemDto>,
)

@Serializable
internal data class MealItemDto(
    val id: String,
    val name: String,
    val grams: Int,
    val per100g: Per100gDto,
    val confidence: Int? = null,
)

internal fun FoodItem.toSaveItem() = SaveMealItem(
    name = name,
    grams = grams,
    per100g = Per100gDto(per100g.kcal, per100g.proteinG, per100g.carbsG, per100g.fatG),
    confidence = confidence,
)

/** The server stores no title: the app builds it from the dish names, like every other meal. */
internal fun MealDto.toDomain(): ConfirmedMeal {
    val foods = items.map { item ->
        FoodItem(
            id = item.id,
            name = item.name,
            grams = item.grams,
            per100g = NutrientsPer100g(item.per100g.kcal, item.per100g.proteinG, item.per100g.carbsG, item.per100g.fatG),
            confidence = item.confidence,
        )
    }
    return ConfirmedMeal(
        id = id,
        type = type,
        loggedAt = loggedAt,
        photo = photoUrl?.let(::MealPhoto),
        title = mealTitleFor(foods),
        items = foods,
        isEdited = isEdited,
    )
}
