package com.visionfit.presentation.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.FitnessGoal
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.designsystem.components.StepIndicator
import com.visionfit.presentation.designsystem.components.VfBackButton
import com.visionfit.presentation.designsystem.components.VfFullScreenLoading
import com.visionfit.presentation.designsystem.components.VfPrimaryButton
import com.visionfit.presentation.designsystem.components.VfSecondaryLink
import com.visionfit.presentation.designsystem.components.VfTopBar
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.goal.components.ConfettiBurst
import com.visionfit.presentation.goal.components.EditLaterHint
import com.visionfit.presentation.goal.components.MacroSplitRow
import com.visionfit.presentation.goal.components.TargetEnergyCard

@Composable
fun GoalRoute(
    onNavigateToDashboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: GoalViewModel = containerViewModel { GoalViewModel(profileRepository, calculateNutritionTarget) },
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            GoalEffect.NavigateToDashboard -> onNavigateToDashboard()
            GoalEffect.NavigateToProfile -> onNavigateToProfile()
            GoalEffect.NavigateBack -> onNavigateBack()
        }
    }
    GoalScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun GoalScreen(state: GoalUiState, onEvent: (GoalEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        ConfettiBurst()
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            VfTopBar(
                navigation = { VfBackButton(onClick = { onEvent(GoalEvent.Back) }) },
                title = { StepIndicator(current = 2, total = 2) },
            )
            val target = state.target
            if (target == null) {
                VfFullScreenLoading(Modifier.weight(1f))
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = VfDimens.ScreenPadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        text = "Tèn tén! Đây là mục tiêu mỗi ngày của bạn",
                        style = VisionFitTheme.type.headlineL,
                        modifier = Modifier.semantics { heading() },
                    )
                    TargetEnergyCard(target)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Chia macros thế này nhé", style = VisionFitTheme.type.sectionTitle, modifier = Modifier.semantics { heading() })
                        MacroSplitRow(target)
                    }
                    EditLaterHint()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = VfDimens.ScreenPadding, end = VfDimens.ScreenPadding, top = 8.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                VfPrimaryButton(
                    text = "Bắt đầu theo dõi thôi!",
                    onClick = { onEvent(GoalEvent.StartTracking) },
                    containerColor = colors.primary,
                    contentColor = colors.surface,
                    iconTint = colors.yellow,
                    enabled = target != null,
                )
                VfSecondaryLink(text = "Chỉnh lại thông tin", onClick = { onEvent(GoalEvent.EditProfile) })
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GoalPreview() {
    VisionFitTheme {
        GoalScreen(state = GoalUiState(CalculateNutritionTargetUseCase()(BodyProfile.Default)), onEvent = {})
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GoalMuscleGainPreview() {
    VisionFitTheme {
        val profile = BodyProfile.Default.copy(goal = FitnessGoal.MUSCLE_GAIN, weightKg = 80)
        GoalScreen(state = GoalUiState(CalculateNutritionTargetUseCase()(profile)), onEvent = {})
    }
}
