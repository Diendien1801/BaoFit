package com.visionfit.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.visionfit.core.format.VnFormat
import com.visionfit.domain.model.ConfirmedMeal
import com.visionfit.domain.model.MealPhoto
import com.visionfit.domain.model.PendingMeal
import com.visionfit.domain.model.PendingStatus
import com.visionfit.presentation.common.copy
import com.visionfit.presentation.common.effective
import com.visionfit.presentation.common.label
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.MacroPills
import com.visionfit.presentation.designsystem.components.MealPhotoImage
import com.visionfit.presentation.designsystem.components.PhotoFocus
import com.visionfit.presentation.designsystem.components.VfMessageCard
import com.visionfit.presentation.designsystem.components.VfTextButton
import com.visionfit.presentation.designsystem.components.VfPill
import com.visionfit.presentation.designsystem.components.spinning
import com.visionfit.presentation.designsystem.components.stripes
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import kotlin.math.roundToInt

/** Confirmed meal: photo, time, name (one line), macro pills and the calorie total. */
@Composable
fun ConfirmedMealRow(meal: ConfirmedMeal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val macros = meal.macros
    BrutalSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VfRadius.Card),
        shadowOffset = VfDimens.ShadowM,
        onClick = onClick,
        onClickLabel = "Xem trong nhật ký",
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MealThumbnail(meal.photo, contentDescription = meal.title)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${meal.type.label} · ${VnFormat.time(meal.loggedAt.time)}",
                    style = VisionFitTheme.type.caption,
                    color = colors.textMuted,
                )
                Text(text = meal.title, style = VisionFitTheme.type.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MacroPills(
                    protein = "${macros.proteinG.roundToInt()}g",
                    carbs = "${macros.carbsG.roundToInt()}g",
                    fat = "${macros.fatG.roundToInt()}g",
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = VnFormat.thousands(meal.kcal), style = VisionFitTheme.type.numberMeal)
                Text(text = "kcal", style = VisionFitTheme.type.chip.copy(fontWeight = VisionFitTheme.type.caption.fontWeight), color = colors.textMuted)
            }
        }
    }
}

/**
 * A photo still going through the AI pipeline. The dashed card and coral shadow set it
 * apart; the copy and overlay change with [PendingMeal.status] and connectivity.
 */
@Composable
fun PendingMealCard(meal: PendingMeal, isOffline: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val status = meal.status.effective(isOffline)
    val copy = status.copy()
    BrutalSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VfRadius.Card),
        dashed = status != PendingStatus.READY_FOR_REVIEW,
        shadowOffset = VfDimens.ShadowM,
        shadowColor = if (status == PendingStatus.READY_FOR_REVIEW) colors.mint else colors.coral,
        onClick = onClick,
        onClickLabel = copy.actionLabel,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                MealThumbnail(meal.photo, contentDescription = null, focus = PhotoFocus(y = 0.45f))
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(VfDimens.Border)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.scrim),
                    contentAlignment = Alignment.Center,
                ) {
                    StatusGlyph(status)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                VfPill(
                    text = "${meal.type.label} · ${VnFormat.time(meal.loggedAt.time)}",
                    background = colors.coralContainer,
                    contentColor = colors.onCoralContainer,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                )
                Text(text = copy.title, style = VisionFitTheme.type.cardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (status == PendingStatus.ANALYZING) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .stripes(base = colors.coral, periodMillis = 700)
                            .border(VfDimens.Border, colors.ink, CircleShape),
                    )
                }
                Text(text = copy.subtitle, style = VisionFitTheme.type.caption.copy(fontWeight = VisionFitTheme.type.body.fontWeight), color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun StatusGlyph(status: PendingStatus) {
    val colors = VisionFitTheme.colors
    when (status) {
        PendingStatus.ANALYZING -> VfIcon(
            VfIcons.Sparkle,
            contentDescription = null,
            size = 26.dp,
            tint = colors.ink,
            fill = colors.yellow,
            strokeWidth = 1.6f,
            modifier = Modifier.spinning(2_400),
        )
        PendingStatus.WAITING_FOR_NETWORK -> VfIcon(VfIcons.WifiOff, contentDescription = null, size = 24.dp, tint = colors.surface)
        PendingStatus.READY_FOR_REVIEW -> GlyphDisc(colors.mint) { VfIcon(VfIcons.Check, null, size = 14.dp, tint = colors.ink) }
        PendingStatus.FAILED -> GlyphDisc(colors.coral) { VfIcon(VfIcons.Alert, null, size = 16.dp, tint = colors.ink) }
    }
}

@Composable
private fun GlyphDisc(color: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(VfDimens.Border, VisionFitTheme.colors.ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun MealThumbnail(
    photo: MealPhoto?,
    contentDescription: String?,
    focus: PhotoFocus = PhotoFocus(),
) {
    val shape = RoundedCornerShape(VfRadius.L)
    MealPhotoImage(
        photo = photo,
        contentDescription = contentDescription,
        focus = focus,
        modifier = Modifier
            .size(68.dp)
            .clip(shape)
            .background(VisionFitTheme.colors.photoPlaceholder)
            .border(VfDimens.Border, VisionFitTheme.colors.ink, shape),
    )
}

/** Empty day: invites the first capture. */
@Composable
fun NoMealsCard(onCaptureClick: () -> Unit, modifier: Modifier = Modifier) {
    VfMessageCard(
        title = "Chưa có bữa nào hôm nay",
        message = "Chụp bữa đầu tiên, AI sẽ tính calo giúp bạn trong vài giây.",
        icon = VfIcons.Camera,
        iconContainer = VisionFitTheme.colors.yellow,
        actionLabel = "Chụp bữa ăn",
        onAction = onCaptureClick,
        modifier = modifier,
    )
}

@Composable
fun SectionHeaderLink(title: String, linkText: String, onLinkClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = VisionFitTheme.type.sectionTitle,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        VfTextButton(
            text = linkText,
            onClick = onLinkClick,
            trailingIcon = VfIcons.ChevronRight,
            contentPadding = PaddingValues(start = 8.dp),
        )
    }
}
