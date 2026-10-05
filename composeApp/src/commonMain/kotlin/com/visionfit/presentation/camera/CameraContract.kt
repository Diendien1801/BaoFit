package com.visionfit.presentation.camera

import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import kotlinx.datetime.LocalTime

enum class CaptureMode { CAMERA, LIBRARY }

enum class CameraLens { BACK, FRONT }

data class CameraUiState(
    val mode: CaptureMode = CaptureMode.CAMERA,
    val isFlashOn: Boolean = false,
    val lens: CameraLens = CameraLens.BACK,
    val previewFrame: MealPhoto? = null,
    val libraryPhotos: List<MealPhoto> = emptyList(),
    val selectedLibraryPhoto: MealPhoto? = null,
    val isLoadingLibrary: Boolean = false,
    val mealType: MealType = MealType.SNACK,
    val time: LocalTime = LocalTime(0, 0),
    /** Capturing / uploading the photo before the analysis job exists. */
    val isSubmitting: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && (mode == CaptureMode.CAMERA || selectedLibraryPhoto != null)
}

sealed interface CameraEvent {
    data class ModeSelected(val mode: CaptureMode) : CameraEvent
    data object ToggleFlash : CameraEvent
    data object SwitchLens : CameraEvent
    data object ShutterClicked : CameraEvent
    data object LibraryShortcutClicked : CameraEvent
    data class LibraryPhotoSelected(val photo: MealPhoto) : CameraEvent
    data object Close : CameraEvent
}

sealed interface CameraEffect {
    data class NavigateToAnalysis(val jobId: String) : CameraEffect
    data object NavigateBack : CameraEffect
    data object SubmitFailed : CameraEffect
}
