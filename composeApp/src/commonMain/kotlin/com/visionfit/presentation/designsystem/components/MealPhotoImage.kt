package com.visionfit.presentation.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.visionfit.domain.model.DetectionRegion
import com.visionfit.domain.model.MealPhoto
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.resources.Res
import com.visionfit.resources.meal_banh_mi
import com.visionfit.resources.meal_com_tam
import com.visionfit.resources.meal_family_tray
import com.visionfit.resources.meal_milk_tea
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private const val BUNDLED_SCHEME = "bundled://"

private fun MealPhoto.bundledResource(): DrawableResource? = when (uri.removePrefix(BUNDLED_SCHEME)) {
    "family_tray" -> Res.drawable.meal_family_tray
    "com_tam" -> Res.drawable.meal_com_tam
    "milk_tea" -> Res.drawable.meal_milk_tea
    "banh_mi" -> Res.drawable.meal_banh_mi
    else -> null
}

/** Like CSS `object-position`: [x] and [y] are 0..1 fractions of the overflow. */
data class PhotoFocus(val x: Float = 0.5f, val y: Float = 0.5f) {
    val alignment: Alignment get() = BiasAlignment(horizontalBias = x * 2 - 1, verticalBias = y * 2 - 1)
}

@Composable
fun rememberMealPhotoPainter(photo: MealPhoto?): Painter? = photo?.bundledResource()?.let { painterResource(it) }

/**
 * Cropped meal photo (`object-fit: cover`). Unknown or missing photos fall back to a neutral
 * placeholder, so diary rows never collapse.
 */
@Composable
fun MealPhotoImage(
    photo: MealPhoto?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    focus: PhotoFocus = PhotoFocus(),
) {
    val painter = rememberMealPhotoPainter(photo)
    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alignment = focus.alignment,
        )
    } else {
        Box(modifier = modifier.background(VisionFitTheme.colors.photoPlaceholder), contentAlignment = Alignment.Center) {
            VfIcon(
                icon = VfIcons.Plate,
                contentDescription = contentDescription,
                size = 28.dp,
                tint = VisionFitTheme.colors.ink.copy(alpha = 0.55f),
                fill = VisionFitTheme.colors.surface.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * Maps normalized [DetectionRegion]s onto a cropped photo so markers line up with the dishes
 * whatever the container size or crop.
 */
class PhotoCropMapping(container: Size, image: Size, focus: PhotoFocus) {
    private val scale = maxOf(container.width / image.width, container.height / image.height)
    private val drawnWidth = image.width * scale
    private val drawnHeight = image.height * scale
    private val originX = (container.width - drawnWidth) * focus.x
    private val originY = (container.height - drawnHeight) * focus.y

    fun center(region: DetectionRegion) = Offset(
        x = originX + region.centerX * drawnWidth,
        y = originY + region.centerY * drawnHeight,
    )

    fun radius(region: DetectionRegion): Float = region.radius * drawnWidth
}

/**
 * Lays out [marker] composables over a cropped [photo]: each is centered on the point returned
 * by [anchor] (in px, inside the container). Draws nothing until the photo is known.
 */
@Composable
fun PhotoMarkersLayer(
    photo: MealPhoto?,
    regions: List<DetectionRegion>,
    focus: PhotoFocus,
    markerSize: Dp,
    modifier: Modifier = Modifier,
    anchor: (mapping: PhotoCropMapping, region: DetectionRegion, container: Size) -> Offset = { mapping, region, _ ->
        mapping.center(region)
    },
    marker: @Composable (index: Int) -> Unit,
) {
    val painter = rememberMealPhotoPainter(photo) ?: return
    val intrinsic = painter.intrinsicSize
    if (intrinsic.isUnspecifiedOrEmpty()) return
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val container = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val mapping = PhotoCropMapping(container, intrinsic, focus)
        val half = with(density) { markerSize.toPx() } / 2
        regions.forEachIndexed { index, region ->
            val point = anchor(mapping, region, container)
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (point.x - half).toDp() },
                    y = with(density) { (point.y - half).toDp() },
                ),
            ) { marker(index) }
        }
    }
}

private fun Size.isUnspecifiedOrEmpty(): Boolean = this == Size.Unspecified || width <= 0f || height <= 0f
