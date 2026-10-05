package com.visionfit.presentation.camera

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.core.time.TimeProvider
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.repository.CameraSource
import com.visionfit.domain.repository.MealAnalysisRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CameraViewModel(
    private val cameraSource: CameraSource,
    private val analysisRepository: MealAnalysisRepository,
    private val timeProvider: TimeProvider,
) : MviViewModel<CameraUiState, CameraEvent, CameraEffect>(
    timeProvider.now().let { now ->
        CameraUiState(
            previewFrame = cameraSource.previewFrame,
            mealType = MealType.forTime(now.time),
            time = now.time,
        )
    },
) {

    init {
        loadLibrary()
    }

    override fun onEvent(event: CameraEvent) {
        when (event) {
            is CameraEvent.ModeSelected -> updateState { copy(mode = event.mode) }
            CameraEvent.ToggleFlash -> updateState { copy(isFlashOn = !isFlashOn) }
            CameraEvent.SwitchLens -> updateState {
                copy(lens = if (lens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK)
            }
            CameraEvent.LibraryShortcutClicked -> updateState {
                copy(mode = CaptureMode.LIBRARY, selectedLibraryPhoto = selectedLibraryPhoto ?: libraryPhotos.firstOrNull())
            }
            is CameraEvent.LibraryPhotoSelected -> updateState { copy(selectedLibraryPhoto = event.photo) }
            CameraEvent.ShutterClicked -> submit()
            CameraEvent.Close -> sendEffect(CameraEffect.NavigateBack)
        }
    }

    private fun loadLibrary() {
        updateState { copy(isLoadingLibrary = true) }
        viewModelScope.launch {
            val photos = cameraSource.recentPhotos()
            updateState {
                copy(libraryPhotos = photos, selectedLibraryPhoto = selectedLibraryPhoto ?: photos.firstOrNull(), isLoadingLibrary = false)
            }
        }
    }

    private fun submit() {
        val state = currentState
        if (!state.canSubmit) return
        updateState { copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                val photo: MealPhoto = when (state.mode) {
                    CaptureMode.CAMERA -> cameraSource.capturePhoto()
                    CaptureMode.LIBRARY -> requireNotNull(state.selectedLibraryPhoto)
                }
                val jobId = analysisRepository.submitPhoto(photo, capturedAt = timeProvider.now())
                sendEffect(CameraEffect.NavigateToAnalysis(jobId))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                sendEffect(CameraEffect.SubmitFailed)
            } finally {
                updateState { copy(isSubmitting = false) }
            }
        }
    }
}
