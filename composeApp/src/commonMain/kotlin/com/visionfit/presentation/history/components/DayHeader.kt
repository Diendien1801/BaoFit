package com.visionfit.presentation.history.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.visionfit.core.format.VnFormat
import com.visionfit.domain.model.DaySummary
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.Macro
import com.visionfit.presentation.designsystem.components.MacroPills
import com.visionfit.presentation.designsystem.components.VfTextButton
import com.visionfit.presentation.designsystem.components.animatedProgress
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.layout.LocalWindowLayout
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/** Horizontally scrolling day chips: "Hôm nay", "CN 04/10", … */
@Composable
fun DateChipsRow(
    dates: List<LocalDate>,
    selected: LocalDate?,
    today: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = VfDimens.ScreenPadding,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected, dates) {
        val index = dates.indexOf(selected)
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 2.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(dates, key = { it.toString() }) { date ->
            DateChip(
                text = if (date == today) "Hôm nay" else VnFormat.shortDate(date),
                isSelected = date == selected,
                onClick = { onDateSelected(date) },
            )
        }
    }
}

@Composable
private fun DateChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = VisionFitTheme.colors
    val background by animateColorAsState(if (isSelected) colors.primary else colors.surface, label = "chipBg")
    BrutalSurface(
        modifier = Modifier.height(VfDimens.SmallButton).semantics { selected = isSelected },
        shape = RoundedCornerShape(VfRadius.L),
        color = background,
        shadowOffset = if (isSelected) VfDimens.ShadowS else 0.dp,
        onClick = onClick,
        role = Role.Tab,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (isSelected) VisionFitTheme.type.labelXL else VisionFitTheme.type.labelL.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) colors.surface else colors.ink,
            modifier = Modifier.padding(horizontal = if (isSelected) 16.dp else 14.dp),
        )
    }
}

/** Yellow card: confirmed calories vs target, progress bar and macro pills. */
@Composable
fun DaySummaryCard(summary: DaySummary, isToday: Boolean, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val progress = animatedProgress(summary.fraction.toFloat().coerceIn(0f, 1f), durationMillis = 1_100, delayMillis = 300)
    val over = buildSet {
        if (summary.protein.isOver) add(Macro.PROTEIN)
        if (summary.carbs.isOver) add(Macro.CARBS)
        if (summary.fat.isOver) add(Macro.FAT)
    }
    BrutalSurface(
        modifier = modifier.fillMaxWidth().popIn(durationMillis = 600),
        shape = RoundedCornerShape(VfRadius.CardL),
        color = colors.yellow,
        shadowOffset = VfDimens.ShadowL,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Đã xác nhận ${summary.eatenKcal} trên ${summary.targetKcal} kcal"
                        },
                ) {
                    Text(
                        text = if (isToday) "Đã xác nhận hôm nay" else "Đã xác nhận ngày ${VnFormat.dayMonth(summary.date)}",
                        style = VisionFitTheme.type.captionStrong,
                        color = colors.onYellow,
                    )
                    Text(
                        text = buildAnnotatedString {
                            append(VnFormat.thousands(summary.eatenKcal))
                            withStyle(SpanStyle(fontSize = 15.sp)) { append(" / ${VnFormat.thousands(summary.targetKcal)} kcal") }
                        },
                        style = VisionFitTheme.type.numberSummary,
                        maxLines = 1,
                    )
                }
                BrutalSurface(
                    modifier = Modifier.rotate(4f),
                    shape = CircleShape,
                    color = if (summary.isOverTarget) colors.coral else colors.surface,
                    shadowOffset = 0.dp,
                ) {
                    Text(
                        text = if (summary.isOverTarget) {
                            "Vượt ${VnFormat.thousands(abs(summary.remainingKcal))}"
                        } else {
                            "Còn ${VnFormat.thousands(summary.remainingKcal)}"
                        },
                        style = VisionFitTheme.type.labelM,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(VfDimens.Border, colors.ink, CircleShape),
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .graphicsLayer { transformOrigin = TransformOrigin(0f, 0.5f) }
                            .clip(CircleShape)
                            .background(if (summary.isOverTarget) colors.coral else colors.primary)
                            .border(VfDimens.Border, colors.ink, CircleShape),
                    )
                }
            }
            MacroPills(
                protein = "${summary.eatenMacros.proteinG.roundToInt()}g",
                carbs = "${summary.eatenMacros.carbsG.roundToInt()}g",
                fat = "${summary.eatenMacros.fatG.roundToInt()}g",
                bordered = true,
                over = over,
                style = VisionFitTheme.type.captionStrong,
                spacing = 6.dp,
            )
        }
    }
}

/**
 * Material 3 date picker limited to today and earlier. On a phone held sideways the calendar
 * grid does not fit, so it opens in text-input mode (the toggle still switches to the grid).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDatePickerDialog(
    initialDate: LocalDate,
    today: LocalDate,
    onDatePicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayMillis = today.toEpochMillis()
    val isShort = LocalWindowLayout.current.isShort
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochMillis(),
        initialDisplayMode = if (isShort) DisplayMode.Input else DisplayMode.Picker,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            VfTextButton(
                text = "Xem ngày này",
                onClick = { state.selectedDateMillis?.let { onDatePicked(it.toLocalDate()) } ?: onDismiss() },
            )
        },
        dismissButton = { VfTextButton(text = "Hủy", onClick = onDismiss, color = VisionFitTheme.colors.textSecondary) },
    ) {
        DatePicker(state = state, showModeToggle = isShort)
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

private fun LocalDate.toEpochMillis(): Long = toEpochDays() * MILLIS_PER_DAY

private fun Long.toLocalDate(): LocalDate = LocalDate.fromEpochDays(floorDiv(MILLIS_PER_DAY))
