package com.visionfit.data.repository

import com.visionfit.data.mock.MockPhotos
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.repository.CameraSource
import kotlinx.coroutines.delay

/** A "camera" that always looks at the dinner tray, and a small fake photo library. */
internal class MockCameraSource(private val shutterMillis: Long = 250) : CameraSource {

    override val previewFrame: MealPhoto = MockPhotos.Viewfinder

    override suspend fun capturePhoto(): MealPhoto {
        delay(shutterMillis)
        return previewFrame
    }

    override suspend fun recentPhotos(): List<MealPhoto> {
        delay(shutterMillis)
        return MockPhotos.Gallery
    }
}
