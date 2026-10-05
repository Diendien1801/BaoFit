package com.visionfit.presentation.analysis

import com.visionfit.domain.model.AnalysisFailure
import com.visionfit.domain.model.AnalysisJob
import com.visionfit.domain.model.AnalysisStage
import kotlin.time.Duration

enum class StepKind { UPLOAD, QUEUE, RECOGNIZE, CALCULATE }

enum class StepStatus { DONE, ACTIVE, PENDING, FAILED }

data class AnalysisStep(
    val kind: StepKind,
    val status: StepStatus,
    val duration: Duration?,
)

data class AnalysisUiState(
    val isLoading: Boolean = true,
    /** The job id does not exist (e.g. it was deleted on another device). */
    val isMissing: Boolean = false,
    val job: AnalysisJob? = null,
    val isRetrying: Boolean = false,
) {
    val stage: AnalysisStage? get() = job?.stage
    val isWorking: Boolean get() = stage == AnalysisStage.RECOGNIZING || stage == AnalysisStage.CALCULATING
    val failure: AnalysisFailure? get() = job?.failure

    val steps: List<AnalysisStep>
        get() {
            val job = job ?: return emptyList()
            val recognize = when (job.stage) {
                AnalysisStage.RECOGNIZING -> StepStatus.ACTIVE
                AnalysisStage.FAILED -> StepStatus.FAILED
                else -> StepStatus.DONE
            }
            val calculate = when (job.stage) {
                AnalysisStage.CALCULATING -> StepStatus.ACTIVE
                AnalysisStage.COMPLETED -> StepStatus.DONE
                else -> StepStatus.PENDING
            }
            return listOf(
                AnalysisStep(StepKind.UPLOAD, StepStatus.DONE, job.timings.upload),
                AnalysisStep(StepKind.QUEUE, StepStatus.DONE, job.timings.queue),
                AnalysisStep(StepKind.RECOGNIZE, recognize, job.timings.recognition.takeIf { recognize == StepStatus.DONE }),
                AnalysisStep(StepKind.CALCULATE, calculate, job.timings.calculation.takeIf { calculate == StepStatus.DONE }),
            )
        }
}

sealed interface AnalysisEvent {
    data object Close : AnalysisEvent
    data object BackToDashboard : AnalysisEvent
    data object ReviewResult : AnalysisEvent
    data object Later : AnalysisEvent
    data object Retry : AnalysisEvent
    data object ManualEntry : AnalysisEvent
}

sealed interface AnalysisEffect {
    data object NavigateToDashboard : AnalysisEffect
    data class NavigateToReview(val jobId: String, val manualEntry: Boolean) : AnalysisEffect
}
