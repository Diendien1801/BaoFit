package com.visionfit.presentation.analysis

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.domain.model.AnalysisStage
import com.visionfit.domain.repository.MealAnalysisRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AnalysisViewModel(
    private val jobId: String,
    private val analysisRepository: MealAnalysisRepository,
) : MviViewModel<AnalysisUiState, AnalysisEvent, AnalysisEffect>(AnalysisUiState()) {

    init {
        analysisRepository.observeJob(jobId)
            .onEach { job ->
                updateState {
                    copy(
                        isLoading = false,
                        isMissing = job == null,
                        job = job,
                        isRetrying = isRetrying && job?.stage == AnalysisStage.FAILED,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: AnalysisEvent) {
        when (event) {
            AnalysisEvent.Close,
            AnalysisEvent.BackToDashboard,
            AnalysisEvent.Later,
            -> sendEffect(AnalysisEffect.NavigateToDashboard)
            AnalysisEvent.ReviewResult -> sendEffect(AnalysisEffect.NavigateToReview(jobId, manualEntry = false))
            AnalysisEvent.ManualEntry -> sendEffect(AnalysisEffect.NavigateToReview(jobId, manualEntry = true))
            AnalysisEvent.Retry -> retry()
        }
    }

    private fun retry() {
        if (currentState.isRetrying || currentState.stage != AnalysisStage.FAILED) return
        updateState { copy(isRetrying = true) }
        viewModelScope.launch {
            analysisRepository.retry(jobId)
            updateState { copy(isRetrying = false) }
        }
    }
}
