package com.visionfit.data.mock

import com.visionfit.domain.model.DetectedFood
import com.visionfit.domain.model.DetectionRegion
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.NutrientsPer100g

/**
 * Builds a [FoodItem] from the nutrition of the whole portion, which is how dishes are usually
 * described ("150 g thịt kho ≈ 345 kcal"). It is stored per 100 g so the weight stays editable.
 */
internal fun portion(
    id: String,
    name: String,
    grams: Int,
    kcal: Double,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    confidence: Int? = null,
): FoodItem {
    val factor = 100.0 / grams
    return FoodItem(
        id = id,
        name = name,
        grams = grams,
        per100g = NutrientsPer100g(
            kcal = kcal * factor,
            proteinG = proteinG * factor,
            carbsG = carbsG * factor,
            fatG = fatG * factor,
        ),
        confidence = confidence,
    )
}

/** Builds a [FoodItem] from per-100 g values, the way a nutrition database returns them. */
internal fun per100g(
    id: String,
    name: String,
    grams: Int,
    kcal: Double,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    confidence: Int?,
) = FoodItem(id, name, grams, NutrientsPer100g(kcal, proteinG, carbsG, fatG), confidence)

/** What the mock vision model "recognizes" on each bundled photo. */
internal object MockAiCatalog {

    fun resultFor(photo: MealPhoto, jobId: String): List<DetectedFood> = when (photo) {
        MockPhotos.FamilyTray -> familyTray(jobId)
        MockPhotos.ComTam -> comTam(jobId)
        MockPhotos.MilkTea -> milkTea(jobId)
        MockPhotos.BanhMi -> banhMi(jobId)
        else -> emptyList()
    }

    /** The dinner tray from the design: 4 dishes, 819 kcal. One dish has low confidence. */
    private fun familyTray(jobId: String) = listOf(
        DetectedFood(
            per100g("$jobId-1", "Thịt kho", 150, 230.0, 14.0, 4.0, 17.0, confidence = 92),
            DetectionRegion(centerX = 0.688f, centerY = 0.505f, radius = 0.162f),
        ),
        DetectedFood(
            per100g("$jobId-2", "Đậu phụ sốt cà chua", 200, 95.0, 7.0, 5.0, 5.5, confidence = 89),
            DetectionRegion(centerX = 0.376f, centerY = 0.448f, radius = 0.147f),
        ),
        DetectedFood(
            per100g("$jobId-3", "Khoai tây xào thịt", 180, 130.0, 5.0, 17.0, 5.0, confidence = 78),
            DetectionRegion(centerX = 0.312f, centerY = 0.693f, radius = 0.176f),
        ),
        DetectedFood(
            per100g("$jobId-4", "Canh dưa chuột", 250, 20.0, 0.8, 3.0, 0.5, confidence = 86),
            DetectionRegion(centerX = 0.821f, centerY = 0.229f, radius = 0.153f),
        ),
    )

    private fun comTam(jobId: String) = listOf(
        DetectedFood(
            per100g("$jobId-1", "Sườn nướng", 180, 230.0, 25.0, 4.0, 13.0, confidence = 94),
            DetectionRegion(centerX = 0.34f, centerY = 0.47f, radius = 0.24f),
        ),
        DetectedFood(
            per100g("$jobId-2", "Cơm tấm", 220, 130.0, 2.7, 28.0, 0.3, confidence = 90),
            DetectionRegion(centerX = 0.72f, centerY = 0.27f, radius = 0.15f),
        ),
        DetectedFood(
            per100g("$jobId-3", "Dưa leo", 60, 16.0, 0.7, 3.6, 0.1, confidence = 72),
            DetectionRegion(centerX = 0.91f, centerY = 0.42f, radius = 0.1f),
        ),
    )

    private fun milkTea(jobId: String) = listOf(
        DetectedFood(
            per100g("$jobId-1", "Trà sữa trân châu đường đen size L", 500, 78.0, 0.8, 12.4, 2.8, confidence = 88),
            DetectionRegion(centerX = 0.49f, centerY = 0.52f, radius = 0.33f),
        ),
    )

    private fun banhMi(jobId: String) = listOf(
        DetectedFood(
            per100g("$jobId-1", "Bánh mì nguyên cám", 140, 247.0, 9.0, 45.0, 3.4, confidence = 91),
            DetectionRegion(centerX = 0.63f, centerY = 0.56f, radius = 0.18f),
        ),
        DetectedFood(
            per100g("$jobId-2", "Trứng ốp la", 55, 196.0, 13.6, 0.8, 15.0, confidence = 96),
            DetectionRegion(centerX = 0.49f, centerY = 0.34f, radius = 0.12f),
        ),
        DetectedFood(
            per100g("$jobId-3", "Giăm bông", 60, 145.0, 17.0, 1.5, 8.0, confidence = 83),
            DetectionRegion(centerX = 0.26f, centerY = 0.36f, radius = 0.15f),
        ),
        DetectedFood(
            per100g("$jobId-4", "Xúc xích", 50, 290.0, 11.0, 3.0, 26.0, confidence = 64),
            DetectionRegion(centerX = 0.28f, centerY = 0.66f, radius = 0.13f),
        ),
    )
}
