package com.visionfit.presentation.camera.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.visionfit.domain.model.MealPhoto
import com.visionfit.presentation.camera.CaptureMode
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.MealPhotoImage
import com.visionfit.presentation.designsystem.components.pinging
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/** "Chụp ảnh | Thư viện" pill switch on the dark camera surface. */
@Composable
fun CaptureModeSwitch(mode: CaptureMode, onModeSelected: (CaptureMode) -> Unit, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.cameraControl)
            .border(VfDimens.Border, colors.cameraOutline, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeOption("Chụp ảnh", mode == CaptureMode.CAMERA) { onModeSelected(CaptureMode.CAMERA) }
        ModeOption("Thư viện", mode == CaptureMode.LIBRARY) { onModeSelected(CaptureMode.LIBRARY) }
    }
}

@Composable
private fun ModeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = VisionFitTheme.colors
    val background by animateColorAsState(if (selected) colors.surface else Color.Transparent, label = "modeBg")
    BrutalSurface(
        modifier = Modifier.height(40.dp).semantics { this.selected = selected },
        shape = CircleShape,
        color = background,
        borderColor = if (selected) colors.ink else Color.Transparent,
        shadowOffset = 0.dp,
        onClick = onClick,
        role = Role.Tab,
        pressDepth = 1.dp,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = VisionFitTheme.type.labelL.copy(fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold),
            color = if (selected) colors.ink else colors.onPrimaryMuted,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}

/** Library shortcut · shutter · switch camera. */
@Composable
fun CaptureButtonsRow(
    libraryPreview: MealPhoto?,
    shutterEnabled: Boolean,
    mode: CaptureMode,
    onLibraryClick: () -> Unit,
    onShutterClick: () -> Unit,
    onSwitchCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrutalSurface(
            modifier = Modifier.size(58.dp).rotate(-8f),
            shape = RoundedCornerShape(16.dp),
            color = colors.inkSoft,
            borderColor = colors.surface,
            shadowOffset = VfDimens.ShadowS,
            shadowColor = colors.yellow,
            onClick = onLibraryClick,
            onClickLabel = "Chọn ảnh từ thư viện",
        ) {
            MealPhotoImage(photo = libraryPreview, contentDescription = "Chọn ảnh từ thư viện", modifier = Modifier.size(58.dp))
        }
        ShutterButton(enabled = shutterEnabled, mode = mode, onClick = onShutterClick)
        BrutalSurface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = colors.primary,
            borderColor = colors.surface,
            shadowOffset = VfDimens.ShadowS,
            shadowColor = colors.yellow,
            onClick = onSwitchCameraClick,
            enabled = mode == CaptureMode.CAMERA,
            onClickLabel = "Đổi camera",
            contentAlignment = Alignment.Center,
        ) {
            VfIcon(VfIcons.SwitchCamera, contentDescription = "Đổi camera", size = 24.dp, tint = colors.surface)
        }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, mode: CaptureMode, onClick: () -> Unit) {
    val colors = VisionFitTheme.colors
    val label = if (mode == CaptureMode.CAMERA) "Chụp ảnh" else "Dùng ảnh đã chọn"
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        if (enabled) {
            Box(
                Modifier
                    .size(92.dp)
                    .pinging(maxScale = 1.5f, startAlpha = 0.55f)
                    .clip(CircleShape)
                    .background(colors.coral),
            )
        }
        BrutalSurface(
            modifier = Modifier.size(92.dp),
            shape = CircleShape,
            color = colors.coral,
            borderColor = colors.surface,
            borderWidth = 3.dp,
            shadowOffset = VfDimens.ShadowM,
            shadowColor = colors.primary,
            onClick = onClick,
            enabled = enabled,
            onClickLabel = label,
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(3.dp, colors.ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                VfIcon(
                    icon = if (mode == CaptureMode.CAMERA) VfIcons.Camera else VfIcons.Check,
                    contentDescription = label,
                    size = 26.dp,
                    tint = colors.ink,
                    fill = colors.yellow,
                    accent = colors.surface,
                    strokeWidth = if (mode == CaptureMode.CAMERA) 2.2f else 3.2f,
                )
            }
        }
    }
}
