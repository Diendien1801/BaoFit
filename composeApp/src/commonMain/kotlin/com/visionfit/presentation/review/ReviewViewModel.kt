package com.visionfit.presentation.review

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.domain.model.AnalysisJob
import com.visionfit.domain.model.NutrientsPer100g
import com.visionfit.domain.repository.MealAnalysisRepository
import com.visionfit.domain.repository.MealRepository
import com.visionfit.domain.repository.ProfileRepository
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.domain.usecase.SummarizeDayUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val jobId: String,
    private val isManualEntry: Boolean,
    private val analysisRepository: MealAnalysisRepository,
    private val mealRepository: MealRepository,
    profileRepository: ProfileRepository,
    calculateTarget: CalculateNutritionTargetUseCase,
    summarizeDay: SummarizeDayUseCase,
) : MviViewModel<ReviewUiState, ReviewEvent, ReviewEffect>(ReviewUiState(jobId = jobId, isManualEntry = isManualEntry)) {

    /** What the AI proposed, to tell whether the user edited anything. */
    private var original: List<EditableFood> = emptyList()
    private var lastRemoved: Pair<Int, EditableFood>? = null
    private var nextColorIndex = 0
    private var nextNewItemNumber = 1

    init {
        viewModelScope.launch {
            val job = analysisRepository.observeJob(jobId).first()
            if (job == null) {
                updateState { copy(isLoading = false, isMissing = true) }
                return@launch
            }
            loadJob(job)
            val target = profileRepository.profile.map(calculateTarget::invoke)
            combine(mealRepository.observeDay(job.capturedAt.date), target) { day, dailyTarget ->
                summarizeDay(day, dailyTarget).remainingKcal
            }
                .onEach { remaining -> updateState { copy(remainingBeforeKcal = remaining) } }
                .launchIn(viewModelScope)
        }
    }

    private fun loadJob(job: AnalysisJob) {
        val items = if (isManualEntry) {
            emptyList()
        } else {
            job.detections.mapIndexed { index, detected -> EditableFood.from(detected.item, colorIndex = index, region = detected.region) }
        }
        original = items
        nextColorIndex = items.size
        updateState {
            copy(
                isLoading = false,
                mealType = job.mealType,
                loggedAt = job.capturedAt,
                photo = job.photo,
                detectedCount = job.detections.size,
                items = items,
            )
        }
    }

    override fun onEvent(event: ReviewEvent) {
        when (event) {
            is ReviewEvent.NameChanged -> editItem(event.id) { copy(name = event.value.take(MAX_NAME_LENGTH)) }
            is ReviewEvent.GramsChanged -> editItem(event.id) { withGrams(event.value) }
            is ReviewEvent.KcalChanged -> editItem(event.id) { withKcal(event.value) }
            is ReviewEvent.RemoveItem -> removeItem(event.id)
            ReviewEvent.UndoRemove -> undoRemove()
            ReviewEvent.AddItem -> addItem()
            ReviewEvent.Save -> save()
            ReviewEvent.Retake -> sendEffect(ReviewEffect.NavigateToCamera)
            ReviewEvent.Close -> sendEffect(ReviewEffect.NavigateToDashboard)
        }
    }

    private fun editItem(id: String, transform: EditableFood.() -> EditableFood) {
        updateState {
            copy(
                items = items.map { if (it.id == id) it.transform() else it },
                invalidItemIds = invalidItemIds - id,
            )
        }
    }

    /** New weight rescales calories and macros; per-100 g values stay. */
    private fun EditableFood.withGrams(text: String): EditableFood {
        val digits = text.filter(Char::isDigit).take(MAX_GRAM_DIGITS)
        val updated = copy(gramsText = digits)
        return updated.copy(kcalText = updated.kcal.toString())
    }

    /** New calories are spread back onto the per-100 g value, so later weight edits stay proportional. */
    private fun EditableFood.withKcal(text: String): EditableFood {
        val cleaned = sanitizeDecimal(text)
        val kcal = cleaned.replace(',', '.').toDoubleOrNull() ?: 0.0
        val per100Kcal = if (grams > 0) kcal * 100 / grams else 0.0
        return copy(kcalText = cleaned, per100g = per100g.copy(kcal = per100Kcal))
    }

    private fun removeItem(id: String) {
        val index = currentState.items.indexOfFirst { it.id == id }
        if (index < 0) return
        val removed = currentState.items[index]
        lastRemoved = index to removed
        updateState { copy(items = items - removed, invalidItemIds = invalidItemIds - id) }
        sendEffect(ReviewEffect.ItemRemoved(removed.name.ifBlank { "món số ${index + 1}" }))
    }

    private fun undoRemove() {
        val (index, item) = lastRemoved ?: return
        lastRemoved = null
        updateState {
            val restored = items.toMutableList().apply { add(index.coerceAtMost(size), item) }
            copy(items = restored)
        }
    }

    private fun addItem() {
        val item = EditableFood(
            id = "$jobId-manual-${nextNewItemNumber++}",
            colorIndex = nextColorIndex++,
            name = "",
            gramsText = DEFAULT_NEW_ITEM_GRAMS.toString(),
            kcalText = "0",
            per100g = NutrientsPer100g.Unknown,
            confidence = null,
            region = null,
        )
        updateState { copy(items = items + item) }
    }

    private fun save() {
        val state = currentState
        if (state.isSaving) return
        val issue = validate(state)
        if (issue != null) {
            val (kind, invalid) = issue
            val firstIndex = state.items.indexOfFirst { it.id in invalid }
            updateState { copy(invalidItemIds = invalid) }
            sendEffect(ReviewEffect.ShowIssue(kind, itemNumber = firstIndex.takeIf { it >= 0 }?.plus(1)))
            return
        }
        val loggedAt = state.loggedAt ?: return
        updateState { copy(isSaving = true) }
        viewModelScope.launch {
            try {
                mealRepository.saveMeal(
                    jobId = jobId,
                    type = state.mealType,
                    loggedAt = loggedAt,
                    photo = state.photo,
                    items = state.items.map { it.toFoodItem() },
                    isEdited = isEdited(state.items),
                )
                sendEffect(ReviewEffect.MealSaved(state.mealType, state.totalKcal))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                sendEffect(ReviewEffect.SaveFailed)
            } finally {
                updateState { copy(isSaving = false) }
            }
        }
    }

    private fun validate(state: ReviewUiState): Pair<ReviewIssue, Set<String>>? {
        if (state.items.isEmpty()) return ReviewIssue.NO_ITEMS to emptySet()
        val unnamed = state.items.filter { it.name.isBlank() }.map { it.id }.toSet()
        if (unnamed.isNotEmpty()) return ReviewIssue.MISSING_NAME to unnamed
        val weightless = state.items.filter { it.grams <= 0 }.map { it.id }.toSet()
        if (weightless.isNotEmpty()) return ReviewIssue.MISSING_WEIGHT to weightless
        return null
    }

    private fun isEdited(items: List<EditableFood>): Boolean {
        if (items.size != original.size) return true
        return items.zip(original).any { (now, before) ->
            now.id != before.id || now.name.trim() != before.name || now.grams != before.grams || now.kcal != before.kcal
        }
    }

    private fun sanitizeDecimal(text: String): String {
        val builder = StringBuilder()
        var hasSeparator = false
        for (char in text) {
            when {
                char.isDigit() -> builder.append(char)
                (char == ',' || char == '.') && !hasSeparator && builder.isNotEmpty() -> {
                    builder.append(',')
                    hasSeparator = true
                }
            }
            if (builder.length >= MAX_KCAL_CHARS) break
        }
        return builder.toString()
    }

    private companion object {
        const val MAX_NAME_LENGTH = 60
        const val MAX_GRAM_DIGITS = 4
        const val MAX_KCAL_CHARS = 6
        const val DEFAULT_NEW_ITEM_GRAMS = 100
    }
}
