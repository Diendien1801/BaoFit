package com.visionfit.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.visionfit.core.format.VnFormat
import com.visionfit.domain.model.DayStatus
import com.visionfit.presentation.dashboard.WeekDay
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.bouncing
import com.visionfit.presentation.designsystem.components.wiggle
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/** "Tuần này": streak chip and one cell per day with a within/over-target dot. */
@Composable
fun WeekStrip(days: List<WeekDay>, streakDays: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Tuần này", style = VisionFitTheme.type.sectionTitle, modifier = Modifier.semantics { heading() })
            if (streakDays > 0) StreakChip(streakDays)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { day -> DayCell(day, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun StreakChip(days: Int) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surface)
            .border(VfDimens.Border, colors.ink, CircleShape)
            .padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .wiggle(fromDegrees = -6f, toDegrees = 5f, periodMillis = 1_400)
                .size(22.dp)
                .clip(CircleShape)
                .background(colors.coral),
            contentAlignment = Alignment.Center,
        ) {
            VfIcon(VfIcons.Flame, contentDescription = null, size = 13.dp, tint = colors.ink, fill = colors.yellow)
        }
        Text(text = "Chuỗi $days ngày", style = VisionFitTheme.type.labelM.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun DayCell(day: WeekDay, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val dotColor = when {
        day.isToday -> colors.yellow
        day.status == DayStatus.WITHIN_TARGET -> colors.mint
        day.status == DayStatus.OVER_TARGET -> colors.coral
        else -> Color.Transparent
    }
    val description = buildString {
        append("${VnFormat.weekdayLong(day.date.dayOfWeek)} ${VnFormat.dayMonth(day.date)}")
        append(
            when {
                day.isToday -> ", hôm nay"
                day.status == DayStatus.WITHIN_TARGET -> ", trong mục tiêu"
                day.status == DayStatus.OVER_TARGET -> ", vượt mục tiêu"
                else -> ", chưa ghi bữa nào"
            },
        )
    }
    BrutalSurface(
        modifier = modifier
            .then(if (day.isToday) Modifier.bouncing(periodMillis = 1_200, delayMillis = 600, iterations = 2) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = RoundedCornerShape(16.dp),
        color = if (day.isToday) colors.primary else colors.surface,
        shadowOffset = if (day.isToday) VfDimens.ShadowS else 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = VnFormat.weekdayShort(day.date.dayOfWeek),
                style = VisionFitTheme.type.chip.copy(fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.SemiBold),
                color = if (day.isToday) colors.onPrimaryMuted else colors.textMuted,
            )
            Text(
                text = day.date.day.toString().padStart(2, '0'),
                style = VisionFitTheme.type.buttonSmall,
                color = if (day.isToday) colors.surface else colors.ink,
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(1.5.dp, if (dotColor == Color.Transparent) colors.outlineMuted else colors.ink, CircleShape),
            )
        }
    }
}
