package com.visionfit.presentation.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionfit.core.mvi.CollectEffects
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.FitnessGoal
import com.visionfit.domain.model.NutritionTarget
import com.visionfit.domain.usecase.CalculateNutritionTargetUseCase
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.designsystem.components.StepIndicator
import com.visionfit.presentation.designsystem.components.VfBackButton
import com.visionfit.presentation.designsystem.components.VfFullScreenLoading
import com.visionfit.presentation.designsystem.components.VfPrimaryButton
import com.visionfit.presentation.designsystem.components.VfSecondaryLink
import com.visionfit.presentation.designsystem.components.VfTextButton
import com.visionfit.presentation.designsystem.components.VfTopBar
import com.visionfit.presentation.designsystem.layout.LocalWindowLayout
import com.visionfit.presentation.designsystem.layout.VfContentWidth
import com.visionfit.presentation.designsystem.layout.WidthClass
import com.visionfit.presentation.designsystem.layout.maxContentWidth
import com.visionfit.presentation.designsystem.layout.safeHorizontal
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.goal.components.ConfettiBurst
import com.visionfit.presentation.goal.components.EditLaterHint
import com.visionfit.presentation.goal.components.MacroSplitRow
import com.visionfit.presentation.goal.components.TargetEnergyCard
import com.visionfit.presentation.preview.VisionFitPreview

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
    val layout = LocalWindowLayout.current
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
                        .windowInsetsPadding(WindowInsets.safeHorizontal)
                        .maxContentWidth(if (layout.usesTwoPanes) VfContentWidth.TwoPane else VfContentWidth.Column)
                        .padding(horizontal = layout.gutter, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        text = "Tèn tén! Đây là mục tiêu mỗi ngày của bạn",
                        style = VisionFitTheme.type.headlineL,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (layout.usesTwoPanes) {
                        // Energy on the left, its macro split beside it.
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            TargetEnergyCard(target, Modifier.weight(1f))
                            Column(Modifier.weight(1f).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                MacroSection(target)
                                EditLaterHint()
                            }
                        }
                    } else {
                        TargetEnergyCard(target)
                        MacroSection(target)
                        EditLaterHint()
                    }
                }
            }
            GoalFooter(canStart = target != null, onEvent = onEvent)
        }
    }
}

@Composable
private fun MacroSection(target: NutritionTarget) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "Chia macros thế này nhé", style = VisionFitTheme.type.sectionTitle, modifier = Modifier.semantics { heading() })
        MacroSplitRow(target)
    }
}

/**
 * "Bắt đầu" with "Chỉnh lại thông tin" under it on phones; on wider or landscape windows the
 * link moves beside the button so the footer stays one row high.
 */
@Composable
private fun GoalFooter(canStart: Boolean, onEvent: (GoalEvent) -> Unit) {
    val colors = VisionFitTheme.colors
    val layout = LocalWindowLayout.current
    val start: @Composable (Modifier) -> Unit = { buttonModifier ->
        VfPrimaryButton(
            text = "Bắt đầu theo dõi thôi!",
            onClick = { onEvent(GoalEvent.StartTracking) },
            containerColor = colors.primary,
            contentColor = colors.surface,
            iconTint = colors.yellow,
            enabled = canStart,
            modifier = buttonModifier,
        )
    }
    val footerModifier = Modifier
        .windowInsetsPadding(WindowInsets.navigationBars)
        .windowInsetsPadding(WindowInsets.safeHorizontal)
        .maxContentWidth(VfContentWidth.Column)
        .padding(
            start = layout.gutter,
            end = layout.gutter,
            top = 8.dp,
            bottom = if (layout.isShort) 12.dp else 22.dp,
        )
    if (layout.widthClass != WidthClass.COMPACT || layout.isShort) {
        Row(
            modifier = footerModifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VfTextButton(text = "Chỉnh lại thông tin", onClick = { onEvent(GoalEvent.EditProfile) })
            start(Modifier.weight(1f))
        }
    } else {
        Column(modifier = footerModifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            start(Modifier)
            VfSecondaryLink(text = "Chỉnh lại thông tin", onClick = { onEvent(GoalEvent.EditProfile) })
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GoalPreview() {
    VisionFitPreview {
        GoalScreen(state = GoalUiState(CalculateNutritionTargetUseCase()(BodyProfile.Default)), onEvent = {})
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GoalMuscleGainPreview() {
    VisionFitPreview {
        val profile = BodyProfile.Default.copy(goal = FitnessGoal.MUSCLE_GAIN, weightKg = 80)
        GoalScreen(state = GoalUiState(CalculateNutritionTargetUseCase()(profile)), onEvent = {})
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun GoalDesktopPreview() {
    VisionFitPreview {
        GoalScreen(state = GoalUiState(CalculateNutritionTargetUseCase()(BodyProfile.Default)), onEvent = {})
    }
}
