package com.visionfit.presentation.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.visionfit.core.format.VnFormat
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.MealEntry
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.PendingStatus
import com.visionfit.presentation.common.colors
import com.visionfit.presentation.common.copy
import com.visionfit.presentation.common.effective
import com.visionfit.presentation.common.label
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.MacroPills
import com.visionfit.presentation.designsystem.components.MealPhotoImage
import com.visionfit.presentation.designsystem.components.PhotoFocus
import com.visionfit.presentation.designsystem.components.dashedBorder
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.components.spinning
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.layout.proportionalHeight
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import kotlin.math.roundToInt

private val CardTilts = listOf(1f, -1.2f, 1f, -1f)
private val BadgeTilts = listOf(-6f, 5f, -4f, 6f)
private val TapeTilts = listOf(-4f, 3f, -3f, 4f)

/**
 * One diary entry as a taped polaroid next to its time badge. Consecutive entries alternate
 * their tilt; a dotted line links the badges down the day.
 */
@Composable
fun TimelineEntry(
    meal: MealEntry,
    index: Int,
    isLast: Boolean,
    isOffline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TimeColumn(meal, index, isLast)
        PolaroidCard(
            meal = meal,
            index = index,
            isOffline = isOffline,
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp)
                .popIn(delayMillis = 100 + index * 100, durationMillis = 600),
        )
    }
}

@Composable
private fun TimeColumn(meal: MealEntry, index: Int, isLast: Boolean) {
    val colors = VisionFitTheme.colors
    val lineColor = colors.outlineMuted
    Column(
        modifier = Modifier.width(58.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrutalSurface(
            modifier = Modifier.rotate(BadgeTilts[index % BadgeTilts.size]),
            shape = RoundedCornerShape(VfRadius.S),
            color = meal.type.colors().badge,
            shadowOffset = VfDimens.ShadowXs,
        ) {
            Text(
                text = VnFormat.time(meal.loggedAt.time),
                style = VisionFitTheme.type.labelM,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        if (!isLast) {
            Box(
                Modifier
                    .weight(1f)
                    .width(3.dp)
                    // Drawn past the bottom edge so it bridges the gap to the next entry's badge.
                    .drawBehind {
                        drawLine(
                            color = lineColor,
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height + 22.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 6.dp.toPx())),
                        )
                    },
            )
        }
    }
}

@Composable
private fun PolaroidCard(
    meal: MealEntry,
    index: Int,
    isOffline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VisionFitTheme.colors
    val pending = meal as? PendingMeal
    val status = pending?.status?.effective(isOffline)
    Box(modifier = modifier) {
        BrutalSurface(
            modifier = Modifier.fillMaxWidth().rotate(CardTilts[index % CardTilts.size]),
            shape = RoundedCornerShape(VfRadius.Card),
            dashed = pending != null && status != PendingStatus.READY_FOR_REVIEW,
            shadowOffset = VfDimens.ShadowL,
            shadowColor = when (status) {
                null -> colors.ink
                PendingStatus.READY_FOR_REVIEW -> colors.mint
                else -> colors.coral
            },
            onClick = if (pending != null) onClick else null,
            onClickLabel = status?.copy()?.actionLabel,
        ) {
            Column(Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .proportionalHeight(ratio = 2.2f, minHeight = 120.dp, maxHeight = 220.dp)
                        .clip(RoundedCornerShape(15.dp)),
                ) {
                    MealPhotoImage(
                        photo = meal.photo,
                        contentDescription = (meal as? ConfirmedMeal)?.title,
                        focus = PhotoFocus(y = 0.45f),
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (status != null) PendingPhotoOverlay(status)
                    if ((meal as? ConfirmedMeal)?.isEdited == true) EditedChip(Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
                Row(
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(text = meal.type.label, style = VisionFitTheme.type.captionStrong, color = meal.type.colors().text)
                        when (meal) {
                            is ConfirmedMeal -> {
                                Text(text = meal.title, style = VisionFitTheme.type.sectionTitleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                MacroPills(
                                    protein = "${meal.macros.proteinG.roundToInt()}g",
                                    carbs = "${meal.macros.carbsG.roundToInt()}g",
                                    fat = "${meal.macros.fatG.roundToInt()}g",
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            is PendingMeal -> {
                                Text(text = "Mâm ${meal.type.label.lowercase()}", style = VisionFitTheme.type.sectionTitleSmall)
                                Text(
                                    text = if (status == PendingStatus.ANALYZING) "Kết quả sẽ có sau vài giây" else status?.copy()?.subtitle.orEmpty(),
                                    style = VisionFitTheme.type.caption.copy(fontWeight = FontWeight.Normal),
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                    when (meal) {
                        is ConfirmedMeal -> KcalBox(meal.kcal)
                        is PendingMeal -> Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(VfRadius.S))
                                .background(colors.coralContainer)
                                .border(VfDimens.Border, colors.ink, RoundedCornerShape(VfRadius.S)),
                            contentAlignment = Alignment.Center,
                        ) {
                            VfIcon(VfIcons.ChevronRight, contentDescription = null, size = 18.dp, tint = colors.ink, strokeWidth = 2.8f)
                        }
                    }
                }
            }
        }
        WashiTape(meal, index, Modifier.align(Alignment.TopCenter).offset(y = (-12).dp))
    }
}

@Composable
private fun WashiTape(meal: MealEntry, index: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .rotate(TapeTilts[index % TapeTilts.size])
            .size(width = 76.dp, height = 22.dp)
            .background(meal.type.colors().tape, shape)
            .dashedBorder(1.5.dp, VisionFitTheme.colors.ink, shape, dash = 4.dp, gap = 3.dp),
    )
}

@Composable
private fun KcalBox(kcal: Int) {
    val colors = VisionFitTheme.colors
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(VfRadius.M))
            .background(colors.background)
            .border(VfDimens.Border, colors.ink, RoundedCornerShape(VfRadius.M))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = VnFormat.thousands(kcal), style = VisionFitTheme.type.titleXL)
        Text(text = "kcal", style = VisionFitTheme.type.micro.copy(letterSpacing = VisionFitTheme.type.chip.letterSpacing))
    }
}

@Composable
private fun PendingPhotoOverlay(status: PendingStatus) {
    val colors = VisionFitTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(colors.scrim),
        contentAlignment = Alignment.Center,
    ) {
        BrutalSurface(shape = CircleShape, shadowOffset = VfDimens.ShadowXs) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (status) {
                    PendingStatus.ANALYZING -> VfIcon(
                        VfIcons.Sparkle, contentDescription = null, size = 18.dp,
                        tint = colors.ink, fill = colors.yellow, modifier = Modifier.spinning(1_800),
                    )
                    PendingStatus.WAITING_FOR_NETWORK -> VfIcon(VfIcons.WifiOff, contentDescription = null, size = 18.dp, tint = colors.ink)
                    PendingStatus.READY_FOR_REVIEW -> VfIcon(VfIcons.Check, contentDescription = null, size = 16.dp, tint = colors.mintText)
                    PendingStatus.FAILED -> VfIcon(VfIcons.Alert, contentDescription = null, size = 16.dp, tint = colors.error)
                }
                Text(text = status.copy().title, style = VisionFitTheme.type.labelL, maxLines = 1)
            }
        }
    }
}

@Composable
private fun EditedChip(modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = modifier
            .rotate(4f)
            .clip(CircleShape)
            .background(colors.surface)
            .border(VfDimens.Border, colors.ink, CircleShape)
            .padding(start = 6.dp, end = 9.dp, top = 1.dp, bottom = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VfIcon(VfIcons.Pencil, contentDescription = null, size = 12.dp, tint = colors.ink, strokeWidth = 2.6f)
        Text(text = "Đã chỉnh sửa", style = VisionFitTheme.type.chip.copy(fontWeight = FontWeight.ExtraBold))
    }
}

@Composable
fun OfflineCacheNote(isOffline: Boolean, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VfIcon(if (isOffline) VfIcons.WifiOff else VfIcons.CloudCheck, contentDescription = null, size = 16.dp, tint = colors.textSecondary)
        Text(
            text = if (isOffline) "Đang ngoại tuyến · hiển thị dữ liệu đã lưu trên máy" else "Đã lưu trên máy, mất mạng vẫn xem được",
            style = VisionFitTheme.type.caption,
            color = colors.textSecondary,
        )
    }
}
