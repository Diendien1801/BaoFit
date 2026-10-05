package com.visionfit.domain.repository

import com.visionfit.domain.model.AnalysisJob
import com.visionfit.domain.model.AuthResult
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.DayLog
import com.visionfit.domain.model.FoodItem
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

interface AuthRepository {
    val session: StateFlow<UserSession?>

    suspend fun login(email: String, password: String): AuthResult

    suspend fun register(email: String, password: String): AuthResult

    /** Returns `false` when the reset e-mail could not be sent. */
    suspend fun requestPasswordReset(email: String): Boolean

    suspend fun logout()
}

interface ProfileRepository {
    val profile: StateFlow<BodyProfile>

    suspend fun saveProfile(profile: BodyProfile)
}

interface MealRepository {
    fun observeDay(date: LocalDate): Flow<DayLog>

    /** Day logs for every date from [from] to [to] (inclusive), oldest first. */
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayLog>>

    /**
     * Saves the reviewed dishes as a confirmed meal. When the meal came from an analysis job,
     * pass its [jobId] so the pending diary entry is replaced instead of duplicated.
     *
     * @return the id of the saved meal.
     */
    suspend fun saveMeal(
        jobId: String?,
        type: MealType,
        loggedAt: LocalDateTime,
        photo: MealPhoto?,
        items: List<FoodItem>,
        isEdited: Boolean,
    ): String
}

interface MealAnalysisRepository {
    /** Uploads the photo and queues it for analysis. Returns the job id to observe. */
    suspend fun submitPhoto(photo: MealPhoto, capturedAt: LocalDateTime): String

    /** Emits `null` when no job with [jobId] exists. */
    fun observeJob(jobId: String): Flow<AnalysisJob?>

    suspend fun retry(jobId: String)
}

/**
 * The device camera and photo library. A production build implements this per platform
 * (CameraX / PhotoPicker on Android, AVFoundation / PHPicker on iOS).
 */
interface CameraSource {
    /** What the viewfinder currently shows. */
    val previewFrame: MealPhoto

    suspend fun capturePhoto(): MealPhoto

    /** Most recent photos in the device library, newest first. */
    suspend fun recentPhotos(): List<MealPhoto>
}

interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>

    /** When the local cache was last synced with the server, or `null` if never. */
    val lastSyncedAt: StateFlow<LocalDateTime?>
}
