package com.visionfit.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.math.roundToInt

enum class MealType {
    BREAKFAST,
    LUNCH,
    SNACK,
    DINNER,
    ;

    companion object {
        /** Infers the meal from the time it was captured. */
        fun forTime(time: LocalTime): MealType = when (time.hour) {
            in 4..9 -> BREAKFAST
            in 10..13 -> LUNCH
            in 17..21 -> DINNER
            else -> SNACK
        }
    }
}

/**
 * Reference to a meal photo. Bundled demo photos use the `bundled://` scheme; a real backend
 * would return `https://` URLs and the UI layer would load them with an image loader.
 */
data class MealPhoto(val uri: String)

data class NutrientsPer100g(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
) {
    companion object {
        val Unknown = NutrientsPer100g(0.0, 0.0, 0.0, 0.0)
    }
}

/**
 * One dish on the tray. Nutrition is stored per 100 g so editing the weight rescales
 * calories and macros.
 *
 * @param confidence AI confidence 0–100, or `null` when the user added the dish manually.
 */
data class FoodItem(
    val id: String,
    val name: String,
    val grams: Int,
    val per100g: NutrientsPer100g,
    val confidence: Int?,
) {
    val kcal: Int get() = (grams * per100g.kcal / 100).roundToInt()

    val macros: Macros
        get() = Macros(
            proteinG = grams * per100g.proteinG / 100,
            carbsG = grams * per100g.carbsG / 100,
            fatG = grams * per100g.fatG / 100,
        )

    val isManual: Boolean get() = confidence == null
}

/** A row in the meal diary: either confirmed by the user or still going through AI analysis. */
sealed interface MealEntry {
    val id: String
    val type: MealType
    val loggedAt: LocalDateTime
    val photo: MealPhoto?
}

data class ConfirmedMeal(
    override val id: String,
    override val type: MealType,
    override val loggedAt: LocalDateTime,
    override val photo: MealPhoto?,
    val title: String,
    val items: List<FoodItem>,
    val isEdited: Boolean,
) : MealEntry {
    val kcal: Int get() = items.sumOf { it.kcal }
    val macros: Macros get() = items.fold(Macros.Zero) { total, item -> total + item.macros }
}

data class PendingMeal(
    override val id: String,
    override val type: MealType,
    override val loggedAt: LocalDateTime,
    override val photo: MealPhoto?,
    val jobId: String,
    val status: PendingStatus,
) : MealEntry

enum class PendingStatus {
    ANALYZING,
    WAITING_FOR_NETWORK,
    READY_FOR_REVIEW,
    FAILED,
}

/** "Thịt kho", "A & B", "A, B & C", "A, B & 2 món khác". */
fun mealTitleFor(items: List<FoodItem>): String {
    val names = items.map { it.name.trim() }.filter { it.isNotEmpty() }
    return when (names.size) {
        0 -> ""
        1 -> names[0]
        2 -> "${names[0]} & ${names[1]}"
        3 -> "${names[0]}, ${names[1]} & ${names[2]}"
        else -> "${names[0]}, ${names[1]} & ${names.size - 2} món khác"
    }
}
