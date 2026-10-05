package com.visionfit.domain.model

import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

/** Server-side stages of a photo analysis job. Upload and queueing happen before the job exists. */
enum class AnalysisStage {
    RECOGNIZING,
    CALCULATING,
    COMPLETED,
    FAILED,
    ;

    val isTerminal: Boolean get() = this == COMPLETED || this == FAILED
}

enum class AnalysisFailure {
    /** The vision model answered with something that could not be parsed. */
    INVALID_AI_RESPONSE,
    NO_FOOD_DETECTED,
}

/**
 * Where a dish sits on the photo, normalized to the image: [centerX] and [radius] are fractions
 * of the image width and [centerY] a fraction of its height. Keeping it normalized lets every
 * screen draw the markers at any size and crop.
 */
data class DetectionRegion(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
)

data class DetectedFood(
    val item: FoodItem,
    val region: DetectionRegion?,
)

data class AnalysisTimings(
    val upload: Duration? = null,
    val queue: Duration? = null,
    val recognition: Duration? = null,
    val calculation: Duration? = null,
)

data class AnalysisJob(
    val id: String,
    val mealType: MealType,
    val capturedAt: LocalDateTime,
    val photo: MealPhoto,
    val photoSizeBytes: Long,
    val stage: AnalysisStage,
    val timings: AnalysisTimings,
    /** Dishes found so far. Filled during [AnalysisStage.CALCULATING] (without nutrition yet). */
    val detections: List<DetectedFood> = emptyList(),
    val failure: AnalysisFailure? = null,
) {
    val totalKcal: Int get() = detections.sumOf { it.item.kcal }
}
