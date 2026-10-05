package com.visionfit.data.repository

import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.data.mock.MockAiCatalog
import com.visionfit.data.mock.MockPhotos
import com.visionfit.domain.model.AnalysisFailure
import com.visionfit.domain.model.AnalysisJob
import com.visionfit.domain.model.AnalysisStage
import com.visionfit.domain.model.AnalysisTimings
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.MealType
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.PendingStatus
import com.visionfit.domain.repository.MealAnalysisRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * Simulates the asynchronous analysis pipeline: recognizing (3.5 s) → calculating (3.5 s) →
 * completed. Jobs keep running in [scope] when the user leaves the screen, and the matching
 * pending diary entry is kept in sync.
 *
 * So both outcomes can be demoed, every second photo submitted fails on its first attempt with
 * an invalid AI response; retrying it succeeds. The seeded dinner job resumes the first time a
 * screen observes it.
 *
 * Not thread-safe by design: callers and [scope] are confined to the main thread, like a
 * repository that only hops threads inside its network client.
 */
internal class MockMealAnalysisRepository(
    private val store: InMemoryVisionFitStore,
    private val scope: CoroutineScope,
    private val uploadMillis: Long = 700,
    private val stageMillis: Long = 3_500,
) : MealAnalysisRepository {

    private val running = mutableMapOf<String, Job>()
    private val failOnNextRun = mutableSetOf<String>()
    private var submissions = 0

    override suspend fun submitPhoto(photo: MealPhoto, capturedAt: LocalDateTime): String {
        delay(uploadMillis)
        submissions++
        val jobId = "job-$submissions-${capturedAt.date}-${capturedAt.time.hour}${capturedAt.time.minute}"
        val mealType = MealType.forTime(capturedAt.time)
        val job = AnalysisJob(
            id = jobId,
            mealType = mealType,
            capturedAt = capturedAt,
            photo = photo,
            photoSizeBytes = MockPhotos.compressedSizeBytes(photo),
            stage = AnalysisStage.RECOGNIZING,
            timings = AnalysisTimings(upload = 600.milliseconds, queue = 100.milliseconds),
        )
        store.aiResults.update { it + (jobId to MockAiCatalog.resultFor(photo, jobId)) }
        store.jobs.update { it + (jobId to job) }
        store.meals.update {
            it + PendingMeal(
                id = "meal-$jobId",
                type = mealType,
                loggedAt = capturedAt,
                photo = photo,
                jobId = jobId,
                status = PendingStatus.ANALYZING,
            )
        }
        if (submissions % 2 == 0) failOnNextRun += jobId
        startSimulation(jobId)
        return jobId
    }

    override fun observeJob(jobId: String): Flow<AnalysisJob?> =
        store.jobs
            .map { it[jobId] }
            .distinctUntilChanged()
            .onStart {
                val job = store.jobs.value[jobId]
                if (job != null && !job.stage.isTerminal) startSimulation(jobId)
            }

    override suspend fun retry(jobId: String) {
        val job = store.jobs.value[jobId] ?: return
        if (job.stage != AnalysisStage.FAILED) return
        updateJob(jobId) {
            it.copy(
                stage = AnalysisStage.RECOGNIZING,
                failure = null,
                detections = emptyList(),
                timings = it.timings.copy(recognition = null, calculation = null),
            )
        }
        updatePendingStatus(jobId, PendingStatus.ANALYZING)
        startSimulation(jobId)
    }

    private fun startSimulation(jobId: String) {
        if (running[jobId]?.isActive == true) return
        running[jobId] = scope.launch {
            delay(stageMillis)
            if (failOnNextRun.remove(jobId) || store.aiResults.value[jobId].isNullOrEmpty()) {
                val failure = if (store.aiResults.value[jobId].isNullOrEmpty()) {
                    AnalysisFailure.NO_FOOD_DETECTED
                } else {
                    AnalysisFailure.INVALID_AI_RESPONSE
                }
                updateJob(jobId) { it.copy(stage = AnalysisStage.FAILED, failure = failure) }
                updatePendingStatus(jobId, PendingStatus.FAILED)
                return@launch
            }
            val detections = store.aiResults.value[jobId].orEmpty()
            updateJob(jobId) {
                it.copy(
                    stage = AnalysisStage.CALCULATING,
                    detections = detections,
                    timings = it.timings.copy(recognition = 4_200.milliseconds),
                )
            }
            delay(stageMillis)
            updateJob(jobId) {
                it.copy(
                    stage = AnalysisStage.COMPLETED,
                    timings = it.timings.copy(calculation = 1_100.milliseconds),
                )
            }
            updatePendingStatus(jobId, PendingStatus.READY_FOR_REVIEW)
        }
    }

    private fun updateJob(jobId: String, transform: (AnalysisJob) -> AnalysisJob) {
        store.jobs.update { jobs ->
            val job = jobs[jobId] ?: return@update jobs
            jobs + (jobId to transform(job))
        }
    }

    private fun updatePendingStatus(jobId: String, status: PendingStatus) {
        store.meals.update { meals ->
            meals.map { if (it is PendingMeal && it.jobId == jobId) it.copy(status = status) else it }
        }
    }
}
