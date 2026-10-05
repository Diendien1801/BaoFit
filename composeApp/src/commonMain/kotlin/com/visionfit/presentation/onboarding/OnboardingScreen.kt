package com.visionfit.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionfit.core.mvi.CollectEffects
import com.visionfit.domain.model.ActivityLevel
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.FitnessGoal
import com.visionfit.domain.model.Sex
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.designsystem.components.StepIndicator
import com.visionfit.presentation.designsystem.components.VfBackButton
import com.visionfit.presentation.designsystem.components.VfPrimaryButton
import com.visionfit.presentation.designsystem.components.VfTopBar
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.components.topBorder
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.onboarding.components.ActivityChoice
import com.visionfit.presentation.onboarding.components.GoalChoice
import com.visionfit.presentation.onboarding.components.SexCard
import com.visionfit.presentation.onboarding.components.StepperCard
import com.visionfit.presentation.onboarding.components.TdeeSummaryRow
import com.visionfit.presentation.onboarding.components.TwoColumnRow

@Composable
fun OnboardingRoute(
    isEditing: Boolean,
    onNavigateToGoal: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: OnboardingViewModel = containerViewModel {
        OnboardingViewModel(profileRepository, calculateNutritionTarget, isEditing)
    },
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            OnboardingEffect.NavigateToGoal -> onNavigateToGoal()
            OnboardingEffect.NavigateBack -> onNavigateBack()
        }
    }
    OnboardingScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun OnboardingScreen(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VisionFitTheme.colors.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        VfTopBar(
            navigation = { VfBackButton(onClick = { onEvent(OnboardingEvent.Back) }) },
            title = { StepIndicator(current = 1, total = 2) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VfDimens.ScreenPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (state.isEditing) "Cập nhật chỉ số của bạn nhé!" else "Kể mình nghe về bạn nào!",
                style = VisionFitTheme.type.headlineL,
                modifier = Modifier.semantics { heading() },
            )
            BodyMetricsGrid(state, onEvent)
            Section("Mức độ vận động") { ActivityGrid(state.profile.activityLevel, onEvent) }
            Section("Mục tiêu") { GoalRow(state.profile.goal, onEvent) }
        }
        OnboardingFooter(state, onEvent)
    }
}

@Composable
private fun BodyMetricsGrid(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    val colors = VisionFitTheme.colors
    val profile = state.profile
    fun stepper(metric: BodyMetric) = { delta: Int -> onEvent(OnboardingEvent.MetricStepped(metric, delta)) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TwoColumnRow { cell ->
            SexCard(
                sex = profile.sex,
                onSexSelected = { onEvent(OnboardingEvent.SexSelected(it)) },
                modifier = cell.popIn(delayMillis = 50),
            )
            StepperCard(
                label = "Tuổi",
                value = profile.ageYears,
                unit = "tuổi",
                background = colors.yellowPale,
                onStep = stepper(BodyMetric.AGE),
                canIncrease = state.canIncrease(BodyMetric.AGE),
                canDecrease = state.canDecrease(BodyMetric.AGE),
                increaseLabel = "Tăng tuổi",
                decreaseLabel = "Giảm tuổi",
                modifier = cell.popIn(delayMillis = 120),
            )
        }
        TwoColumnRow { cell ->
            StepperCard(
                label = "Chiều cao",
                value = profile.heightCm,
                unit = "cm",
                background = colors.skyContainer,
                onStep = stepper(BodyMetric.HEIGHT),
                canIncrease = state.canIncrease(BodyMetric.HEIGHT),
                canDecrease = state.canDecrease(BodyMetric.HEIGHT),
                increaseLabel = "Tăng chiều cao",
                decreaseLabel = "Giảm chiều cao",
                modifier = cell.popIn(delayMillis = 190),
            )
            StepperCard(
                label = "Cân nặng",
                value = profile.weightKg,
                unit = "kg",
                background = colors.mintContainer,
                onStep = stepper(BodyMetric.WEIGHT),
                canIncrease = state.canIncrease(BodyMetric.WEIGHT),
                canDecrease = state.canDecrease(BodyMetric.WEIGHT),
                increaseLabel = "Tăng cân nặng",
                decreaseLabel = "Giảm cân nặng",
                modifier = cell.popIn(delayMillis = 260),
            )
        }
    }
}

@Composable
private fun ActivityGrid(selected: ActivityLevel, onEvent: (OnboardingEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ActivityLevel.entries.chunked(2).forEach { pair ->
            TwoColumnRow(spacing = 10.dp) { cell ->
                pair.forEach { level ->
                    ActivityChoice(
                        level = level,
                        selected = level == selected,
                        onClick = { onEvent(OnboardingEvent.ActivitySelected(level)) },
                        modifier = cell,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalRow(selected: FitnessGoal, onEvent: (OnboardingEvent) -> Unit) {
    TwoColumnRow(spacing = 10.dp) { cell ->
        FitnessGoal.entries.forEach { goal ->
            GoalChoice(
                goal = goal,
                selected = goal == selected,
                onClick = { onEvent(OnboardingEvent.GoalSelected(goal)) },
                modifier = cell,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, style = VisionFitTheme.type.sectionTitleSmall, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun OnboardingFooter(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    val colors = VisionFitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .topBorder(VfDimens.Border, colors.ink)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = VfDimens.ScreenPadding, end = VfDimens.ScreenPadding, top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.target?.let { TdeeSummaryRow(tdeeKcal = it.tdeeKcal, targetKcal = it.dailyKcal) }
        VfPrimaryButton(
            text = "Tính mục tiêu của tôi",
            onClick = { onEvent(OnboardingEvent.Submit) },
            containerColor = colors.primary,
            contentColor = colors.surface,
            iconTint = colors.yellow,
            loading = state.isSaving,
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingPreview() {
    VisionFitTheme {
        val profile = BodyProfile.Default
        OnboardingScreen(
            state = OnboardingUiState(profile = profile, target = CalculateNutritionTargetUseCase()(profile)),
            onEvent = {},
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingFemaleBulkPreview() {
    VisionFitTheme {
        val profile = BodyProfile.Default.copy(
            sex = Sex.FEMALE,
            ageYears = 90,
            heightCm = 158,
            weightKg = 49,
            activityLevel = ActivityLevel.ACTIVE,
            goal = FitnessGoal.MUSCLE_GAIN,
        )
        OnboardingScreen(
            state = OnboardingUiState(profile = profile, target = CalculateNutritionTargetUseCase()(profile), isEditing = true),
            onEvent = {},
        )
    }
}
