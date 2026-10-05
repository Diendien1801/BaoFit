package com.visionfit.presentation.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionfit.core.format.VnFormat
import com.visionfit.core.mvi.CollectEffects
import com.visionfit.domain.model.MealType
import com.visionfit.presentation.camera.components.CameraViewfinder
import com.visionfit.presentation.camera.components.CaptureButtonsRow
import com.visionfit.presentation.camera.components.CaptureModeSwitch
import com.visionfit.presentation.camera.components.LibraryPicker
import com.visionfit.presentation.common.LocalAppMessenger
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.common.label
import com.visionfit.presentation.designsystem.components.VfIconTileButton
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.preview.PreviewFixtures
import kotlinx.datetime.LocalTime

@Composable
fun CameraRoute(
    onNavigateToAnalysis: (jobId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CameraViewModel = containerViewModel { CameraViewModel(cameraSource, mealAnalysisRepository, timeProvider) },
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val messenger = LocalAppMessenger.current
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is CameraEffect.NavigateToAnalysis -> onNavigateToAnalysis(effect.jobId)
            CameraEffect.NavigateBack -> onNavigateBack()
            CameraEffect.SubmitFailed -> messenger.show("Chưa tải ảnh lên được. Kiểm tra mạng rồi thử lại nhé.")
        }
    }
    CameraScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun CameraScreen(state: CameraUiState, onEvent: (CameraEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        CameraTopBar(state, onEvent)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (state.mode) {
                CaptureMode.CAMERA -> CameraViewfinder(
                    frame = state.previewFrame,
                    lens = state.lens,
                    isSubmitting = state.isSubmitting,
                )
                CaptureMode.LIBRARY -> LibraryPicker(
                    photos = state.libraryPhotos,
                    selected = state.selectedLibraryPhoto,
                    isLoading = state.isLoadingLibrary,
                    isSubmitting = state.isSubmitting,
                    onPhotoSelected = { onEvent(CameraEvent.LibraryPhotoSelected(it)) },
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 21.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (state.mode == CaptureMode.CAMERA) {
                    "Chụp từ trên xuống, đủ sáng để AI đoán chuẩn hơn"
                } else {
                    "Chọn một ảnh bữa ăn rồi bấm nút giữa để phân tích"
                },
                style = VisionFitTheme.type.caption.copy(fontWeight = VisionFitTheme.type.body.fontWeight),
                color = colors.cameraText,
                textAlign = TextAlign.Center,
            )
            CaptureModeSwitch(mode = state.mode, onModeSelected = { onEvent(CameraEvent.ModeSelected(it)) })
            CaptureButtonsRow(
                libraryPreview = state.libraryPhotos.firstOrNull(),
                shutterEnabled = state.canSubmit,
                mode = state.mode,
                onLibraryClick = { onEvent(CameraEvent.LibraryShortcutClicked) },
                onShutterClick = { onEvent(CameraEvent.ShutterClicked) },
                onSwitchCameraClick = { onEvent(CameraEvent.SwitchLens) },
            )
            Text(
                text = "JPG, PNG · tối đa 5 MB · tự nén trước khi tải lên",
                style = VisionFitTheme.type.caption.copy(fontWeight = VisionFitTheme.type.body.fontWeight),
                color = colors.cameraTextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CameraTopBar(state: CameraUiState, onEvent: (CameraEvent) -> Unit) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VfIconTileButton(
            icon = VfIcons.Close,
            contentDescription = "Đóng",
            onClick = { onEvent(CameraEvent.Close) },
            shadowColor = colors.coral,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
            Text(text = "Ghi bữa ăn", style = VisionFitTheme.type.sectionTitle, color = colors.surface)
            Text(
                text = "${state.mealType.label} · ${VnFormat.time(state.time)}",
                style = VisionFitTheme.type.caption,
                color = colors.cameraText,
            )
        }
        VfIconTileButton(
            icon = VfIcons.Bolt,
            contentDescription = if (state.isFlashOn) "Tắt đèn flash" else "Bật đèn flash",
            onClick = { onEvent(CameraEvent.ToggleFlash) },
            containerColor = if (state.isFlashOn) colors.surface else colors.yellow,
            iconFill = if (state.isFlashOn) colors.yellow else colors.surface,
            shadowColor = colors.coral,
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CameraPreview() {
    VisionFitTheme {
        CameraScreen(
            state = CameraUiState(
                previewFrame = PreviewFixtures.trayPhoto,
                libraryPhotos = listOf(PreviewFixtures.comTamPhoto, PreviewFixtures.trayPhoto),
                mealType = MealType.DINNER,
                time = LocalTime(18, 40),
            ),
            onEvent = {},
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CameraLibraryPreview() {
    VisionFitTheme {
        val photos = listOf(PreviewFixtures.comTamPhoto, PreviewFixtures.trayPhoto, PreviewFixtures.milkTeaPhoto, PreviewFixtures.banhMiPhoto)
        CameraScreen(
            state = CameraUiState(
                mode = CaptureMode.LIBRARY,
                libraryPhotos = photos,
                selectedLibraryPhoto = photos[1],
                mealType = MealType.DINNER,
                time = LocalTime(18, 40),
            ),
            onEvent = {},
        )
    }
}
