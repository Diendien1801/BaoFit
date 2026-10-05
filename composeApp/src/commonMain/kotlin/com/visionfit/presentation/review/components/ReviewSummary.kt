package com.visionfit.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.visionfit.core.format.VnFormat
import com.visionfit.presentation.common.label
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.DishNumberBadge
import com.visionfit.presentation.designsystem.components.MacroPills
import com.visionfit.presentation.designsystem.components.MealPhotoImage
import com.visionfit.presentation.designsystem.components.PhotoFocus
import com.visionfit.presentation.designsystem.components.PhotoMarkersLayer
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.components.wiggle
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.review.ReviewUiState

private val ThumbnailFocus = PhotoFocus(x = 0.5f, y = 0.38f)

/** Purple card: polaroid with numbered dishes, AI sticker and live meal totals. */
@Composable
fun ReviewSummaryCard(state: ReviewUiState, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val totals = state.totalMacros
    BrutalSurface(
        modifier = modifier.fillMaxWidth().popIn(durationMillis = 600),
        shape = RoundedCornerShape(VfRadius.CardXL),
        color = colors.primary,
        shadowOffset = VfDimens.ShadowXL,
    ) {
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 50.dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(colors.primaryBlob),
        )
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Polaroid(state)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AiSticker(isManual = state.isManualEntry, count = state.detectedCount)
                state.loggedAt?.let {
                    Text(
                        text = "${state.mealType.label} · ${VnFormat.time(it.time)}",
                        style = VisionFitTheme.type.caption,
                        color = colors.onPrimaryMuted,
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = VnFormat.thousands(state.totalKcal),
                        style = VisionFitTheme.type.numberL,
                        color = colors.surface,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = "kcal",
                        style = VisionFitTheme.type.labelXL.copy(fontWeight = FontWeight.Bold),
                        color = colors.surface,
                        modifier = Modifier.alignByBaseline().padding(start = 4.dp),
                    )
                }
                MacroPills(
                    protein = "${VnFormat.oneDecimal(totals.proteinG)}g",
                    carbs = "${VnFormat.oneDecimal(totals.carbsG)}g",
                    fat = "${VnFormat.oneDecimal(totals.fatG)}g",
                    bordered = true,
                )
            }
        }
    }
}

@Composable
private fun Polaroid(state: ReviewUiState) {
    val colors = VisionFitTheme.colors
    val regions = state.items.mapNotNull { item -> item.region?.let { item to it } }
    BrutalSurface(
        modifier = Modifier
            .wiggle(fromDegrees = -5f, toDegrees = -2f, periodMillis = 5_000)
            .width(140.dp),
        shape = RoundedCornerShape(VfRadius.XL),
        shadowOffset = VfDimens.ShadowS,
    ) {
        Box(
            modifier = Modifier
                .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 18.dp)
                .size(124.dp)
                .clip(RoundedCornerShape(VfRadius.S))
                .background(colors.inkSoft),
        ) {
            MealPhotoImage(
                photo = state.photo,
                contentDescription = "Ảnh bữa ăn đã phân tích",
                focus = ThumbnailFocus,
                modifier = Modifier.size(124.dp),
            )
            PhotoMarkersLayer(
                photo = state.photo,
                regions = regions.map { it.second },
                focus = ThumbnailFocus,
                markerSize = 20.dp,
                modifier = Modifier.size(124.dp),
            ) { index ->
                val item = regions[index].first
                DishNumberBadge(
                    number = state.items.indexOf(item) + 1,
                    color = colors.dishMarkers[item.colorIndex % colors.dishMarkers.size],
                    size = 20.dp,
                    style = VisionFitTheme.type.badge,
                )
            }
        }
    }
}

@Composable
private fun AiSticker(isManual: Boolean, count: Int) {
    val colors = VisionFitTheme.colors
    BrutalSurface(
        modifier = Modifier.rotate(-3f),
        shape = CircleShape,
        color = colors.yellow,
        shadowOffset = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 1.dp, bottom = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VfIcon(
                icon = if (isManual) VfIcons.Pencil else VfIcons.Sparkle,
                contentDescription = null,
                size = 13.dp,
                tint = colors.ink,
                fill = colors.coral,
            )
            Text(text = if (isManual) "Tự nhập món" else "AI thấy $count món", style = VisionFitTheme.type.labelS)
        }
    }
}

/** How this meal changes the day: still within target, or by how much it goes over. */
@Composable
fun ImpactBanner(remainingBeforeKcal: Int, remainingAfterKcal: Int, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val isOver = remainingAfterKcal < 0
    val title = if (isOver) "Ối, hơi quá tay rồi!" else "Vẫn trong mục tiêu!"
    val message = when {
        !isOver -> "Sau bữa này bạn còn ${VnFormat.thousands(remainingAfterKcal)} kcal cho hôm nay."
        remainingBeforeKcal >= 0 ->
            "Bữa này sẽ vượt mục tiêu hôm nay ${VnFormat.thousands(-remainingAfterKcal)} kcal " +
                "(trước bữa còn ${VnFormat.thousands(remainingBeforeKcal)} kcal)."
        else -> "Hôm nay đã vượt mục tiêu, bữa này sẽ nâng tổng vượt lên ${VnFormat.thousands(-remainingAfterKcal)} kcal."
    }
    BrutalSurface(
        modifier = modifier
            .fillMaxWidth()
            .popIn(delayMillis = 100, durationMillis = 600)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$title $message"
            },
        shape = RoundedCornerShape(VfRadius.XL),
        color = if (isOver) colors.coralContainer else colors.mintContainer,
        shadowOffset = VfDimens.ShadowS,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrutalSurface(
                modifier = Modifier.size(34.dp).wiggle(fromDegrees = -2f, toDegrees = 2f, periodMillis = 1_200),
                shape = RoundedCornerShape(11.dp),
                shadowOffset = 0.dp,
                contentAlignment = Alignment.Center,
            ) {
                if (isOver) {
                    VfIcon(VfIcons.Flame, contentDescription = null, size = 18.dp, tint = colors.ink, fill = colors.coral)
                } else {
                    VfIcon(VfIcons.Check, contentDescription = null, size = 18.dp, tint = colors.ink, strokeWidth = 3f)
                }
            }
            Column {
                Text(text = title, style = VisionFitTheme.type.labelL)
                Text(text = message, style = VisionFitTheme.type.bodySmall)
            }
        }
    }
}
