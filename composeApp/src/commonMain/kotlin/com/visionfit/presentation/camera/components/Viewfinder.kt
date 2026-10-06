package com.visionfit.presentation.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.visionfit.domain.model.MealPhoto
import com.visionfit.presentation.camera.CameraLens
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.CheckBadge
import com.visionfit.presentation.designsystem.components.MealPhotoImage
import com.visionfit.presentation.designsystem.components.VfLoadingIndicator
import com.visionfit.presentation.designsystem.components.breathing
import com.visionfit.presentation.designsystem.components.floating
import com.visionfit.presentation.designsystem.components.hardShadow
import com.visionfit.presentation.designsystem.components.wiggle
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

private const val VIEWFINDER_RATIO = 358f / 477f

/** 3:4 live preview with framing corners and the "whole tray" hint. */
@Composable
fun CameraViewfinder(
    frame: MealPhoto?,
    lens: CameraLens,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier,
) {
    ViewfinderFrame(modifier) {
        MealPhotoImage(
            photo = frame,
            contentDescription = "Khung ngắm camera: mâm cơm gồm thịt kho, đậu phụ sốt cà chua, khoai tây xào và canh dưa chuột",
            modifier = Modifier
                .fillMaxSize()
                // The front camera preview is mirrored, like every selfie camera.
                .graphicsLayer { scaleX = if (lens == CameraLens.FRONT) -1f else 1f },
        )
        FrameCorners()
        // Side padding lets the hint wrap instead of touching the frame when the frame is narrow.
        HintChip(Modifier.align(Alignment.TopCenter).padding(start = 16.dp, end = 16.dp, top = 24.dp))
        UploadingOverlay(visible = isSubmitting)
    }
}

/** Library mode: recent photos in a 2 × 2 grid inside the same frame. */
@Composable
fun LibraryPicker(
    photos: List<MealPhoto>,
    selected: MealPhoto?,
    isLoading: Boolean,
    isSubmitting: Boolean,
    onPhotoSelected: (MealPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    ViewfinderFrame(modifier) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VfLoadingIndicator(label = "Đang mở thư viện…")
            }
            photos.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Thư viện chưa có ảnh nào. Chuyển sang Chụp ảnh nhé!",
                    style = VisionFitTheme.type.body,
                    color = VisionFitTheme.colors.cameraText,
                    textAlign = TextAlign.Center,
                )
            }
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                photos.take(4).chunked(2).forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { photo ->
                            LibraryTile(photo, isSelected = photo == selected, onClick = { onPhotoSelected(photo) }, Modifier.weight(1f))
                        }
                        if (row.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }
        }
        UploadingOverlay(visible = isSubmitting)
    }
}

@Composable
private fun ViewfinderFrame(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = VisionFitTheme.colors
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .aspectRatio(VIEWFINDER_RATIO)
            .hardShadow(shape, VfDimens.ShadowXL, colors.primary)
            .clip(shape)
            .background(colors.inkSoft)
            .border(3.dp, colors.surface, shape),
        content = content,
    )
}

@Composable
private fun LibraryTile(photo: MealPhoto, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = VisionFitTheme.colors
    Box(modifier = modifier.fillMaxSize()) {
        BrutalSurface(
            modifier = Modifier.fillMaxSize().semantics { selected = isSelected },
            shape = RoundedCornerShape(18.dp),
            color = colors.inkSoft,
            borderColor = if (isSelected) colors.yellow else colors.surface,
            borderWidth = if (isSelected) 4.dp else VfDimens.Border,
            shadowOffset = if (isSelected) VfDimens.ShadowS else 0.dp,
            shadowColor = colors.yellow,
            onClick = onClick,
            role = Role.RadioButton,
            onClickLabel = "Chọn ảnh này",
        ) {
            MealPhotoImage(photo = photo, contentDescription = "Ảnh trong thư viện", modifier = Modifier.fillMaxSize())
        }
        if (isSelected) CheckBadge(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp))
    }
}

@Composable
private fun FrameCorners() {
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        FrameCorner(0f, TransformOrigin(0f, 0f), Modifier.align(Alignment.TopStart))
        FrameCorner(90f, TransformOrigin(1f, 0f), Modifier.align(Alignment.TopEnd))
        FrameCorner(270f, TransformOrigin(0f, 1f), Modifier.align(Alignment.BottomStart))
        FrameCorner(180f, TransformOrigin(1f, 1f), Modifier.align(Alignment.BottomEnd))
    }
}

/** One rounded "L" bracket, drawn for the top-left corner and rotated for the others. */
@Composable
private fun FrameCorner(rotation: Float, origin: TransformOrigin, modifier: Modifier, size: Dp = 44.dp) {
    val color = VisionFitTheme.colors.yellow
    Canvas(
        modifier = modifier
            .breathing(origin)
            .size(size)
            .graphicsLayer { rotationZ = rotation },
    ) {
        val stroke = 6.dp.toPx()
        val half = stroke / 2
        val radius = 22.dp.toPx() - half
        val path = Path().apply {
            moveTo(half, this@Canvas.size.height)
            lineTo(half, half + radius)
            arcTo(Rect(Offset(half, half), Size(radius * 2, radius * 2)), 180f, 90f, forceMoveTo = false)
            lineTo(this@Canvas.size.width, half)
        }
        drawPath(path, color, style = Stroke(width = stroke))
    }
}

@Composable
private fun HintChip(modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    BrutalSurface(
        modifier = modifier
            .floating(amplitude = 6.dp, periodMillis = 2_600)
            .wiggle(fromDegrees = -3f, toDegrees = -1f, periodMillis = 2_600),
        shape = CircleShape,
        color = colors.yellow,
        shadowOffset = VfDimens.ShadowS,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(VfDimens.Border, colors.ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                VfIcon(VfIcons.Sparkle, contentDescription = null, size = 13.dp, tint = colors.ink, fill = colors.coral, strokeWidth = 1.8f)
            }
            Text(text = "Đưa cả mâm cơm vào khung nhé!", style = VisionFitTheme.type.labelL)
        }
    }
}

@Composable
private fun UploadingOverlay(visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            VfLoadingIndicator(label = "Đang nén & tải ảnh lên…")
        }
    }
}
