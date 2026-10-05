package com.visionfit.data.mock

import com.visionfit.domain.model.MealPhoto

/** Demo photos bundled as Compose resources. The UI resolves the `bundled://` keys. */
object MockPhotos {
    const val SCHEME = "bundled://"

    val FamilyTray = MealPhoto("${SCHEME}family_tray")
    val ComTam = MealPhoto("${SCHEME}com_tam")
    val MilkTea = MealPhoto("${SCHEME}milk_tea")
    val BanhMi = MealPhoto("${SCHEME}banh_mi")

    /** What the camera "sees" in the mock viewfinder. */
    val Viewfinder = FamilyTray

    /** The device gallery shown in library mode, newest first. */
    val Gallery = listOf(ComTam, FamilyTray, MilkTea, BanhMi)

    /** Approximate size after on-device compression, used for the upload step. */
    fun compressedSizeBytes(photo: MealPhoto): Long = when (photo) {
        FamilyTray -> 1_200_000
        ComTam -> 860_000
        MilkTea -> 540_000
        BanhMi -> 730_000
        else -> 1_000_000
    }
}
