package com.visionfit.presentation.review

import com.visionfit.domain.model.DetectionRegion
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.Macros
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.NutrientsPer100g
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt

/**
 * A dish being reviewed. Inputs are kept as raw text so typing never gets reformatted under
 * the cursor; numbers are derived from them.
 */
data class EditableFood(
    val id: String,
    /** Stable color slot, so a dish keeps its color when others are removed. */
    val colorIndex: Int,
    val name: String,
    val gramsText: String,
    val kcalText: String,
    val per100g: NutrientsPer100g,
    /** `null` when added by the user. */
    val confidence: Int?,
    val region: DetectionRegion?,
) {
    val grams: Int get() = gramsText.toIntOrNull() ?: 0
    val kcal: Int get() = (grams * per100g.kcal / 100).roundToInt()

    val macros: Macros
        get() = Macros(
            proteinG = grams * per100g.proteinG / 100,
            carbsG = grams * per100g.carbsG / 100,
            fatG = grams * per100g.fatG / 100,
        )

    val isManual: Boolean get() = confidence == null
    val isLowConfidence: Boolean get() = confidence != null && confidence < LOW_CONFIDENCE_THRESHOLD

    fun toFoodItem() = FoodItem(id = id, name = name.trim(), grams = grams, per100g = per100g, confidence = confidence)

    companion object {
        const val LOW_CONFIDENCE_THRESHOLD = 85

        fun from(item: FoodItem, colorIndex: Int, region: DetectionRegion?) = EditableFood(
            id = item.id,
            colorIndex = colorIndex,
            name = item.name,
            gramsText = item.grams.toString(),
            kcalText = item.kcal.toString(),
            per100g = item.per100g,
            confidence = item.confidence,
            region = region,
        )
    }
}

enum class ReviewIssue {
    /** Nothing to save. */
    NO_ITEMS,
    MISSING_NAME,
    MISSING_WEIGHT,
}

data class ReviewUiState(
    val isLoading: Boolean = true,
    val isMissing: Boolean = false,
    val jobId: String = "",
    val isManualEntry: Boolean = false,
    val mealType: MealType = MealType.DINNER,
    val loggedAt: LocalDateTime? = null,
    val photo: MealPhoto? = null,
    /** How many dishes the AI found (shown on the summary sticker). */
    val detectedCount: Int = 0,
    val items: List<EditableFood> = emptyList(),
    /** Calories left for the day before this meal; negative if already over. */
    val remainingBeforeKcal: Int = 0,
    /** Items highlighted after a failed save attempt. */
    val invalidItemIds: Set<String> = emptySet(),
    val isSaving: Boolean = false,
) {
    val totalKcal: Int get() = items.sumOf { it.kcal }
    val totalMacros: Macros get() = items.fold(Macros.Zero) { total, item -> total + item.macros }
    val remainingAfterKcal: Int get() = remainingBeforeKcal - totalKcal
    val isOverTarget: Boolean get() = remainingAfterKcal < 0
    val canSave: Boolean get() = !isSaving && items.isNotEmpty()
}

sealed interface ReviewEvent {
    data class NameChanged(val id: String, val value: String) : ReviewEvent
    data class GramsChanged(val id: String, val value: String) : ReviewEvent
    data class KcalChanged(val id: String, val value: String) : ReviewEvent
    data class RemoveItem(val id: String) : ReviewEvent
    data object UndoRemove : ReviewEvent
    data object AddItem : ReviewEvent
    data object Save : ReviewEvent
    data object Retake : ReviewEvent
    data object Close : ReviewEvent
}

sealed interface ReviewEffect {
    data class MealSaved(val mealType: MealType, val totalKcal: Int) : ReviewEffect
    data object NavigateToCamera : ReviewEffect
    data object NavigateToDashboard : ReviewEffect
    data class ItemRemoved(val name: String) : ReviewEffect
    data class ShowIssue(val issue: ReviewIssue, val itemNumber: Int?) : ReviewEffect
    data object SaveFailed : ReviewEffect
}
