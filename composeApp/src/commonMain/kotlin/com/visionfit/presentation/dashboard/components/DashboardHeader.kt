package com.visionfit.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.visionfit.core.format.VnFormat
import com.visionfit.presentation.dashboard.Greeting
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.VfIconTileButton
import com.visionfit.presentation.designsystem.components.bouncing
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Composable
fun DashboardHeader(
    initials: String,
    userName: String,
    today: LocalDate?,
    greeting: Greeting,
    notificationCount: Int,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrutalSurface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = colors.yellow,
            shadowOffset = VfDimens.ShadowS,
            contentAlignment = Alignment.Center,
        ) {
            Text(text = initials, style = VisionFitTheme.type.buttonSmall)
        }
        Column(Modifier.weight(1f)) {
            today?.let {
                Text(text = VnFormat.longDate(it), style = VisionFitTheme.type.caption, color = colors.textMuted)
            }
            Text(
                text = if (userName.isBlank()) "${greeting.text}!" else "${greeting.text}, $userName!",
                style = VisionFitTheme.type.headlineS,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        Box {
            VfIconTileButton(
                icon = VfIcons.Bell,
                contentDescription = if (notificationCount > 0) "Thông báo, $notificationCount kết quả mới" else "Thông báo",
                onClick = onNotificationsClick,
            )
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .bouncing()
                        .height(20.dp)
                        .defaultMinSize(minWidth = 20.dp)
                        .clip(CircleShape)
                        .background(colors.coral)
                        .border(VfDimens.Border, colors.ink, CircleShape)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = if (notificationCount > 9) "9+" else notificationCount.toString(), style = VisionFitTheme.type.badge)
                }
            }
        }
    }
}

private val Greeting.text: String
    get() = when (this) {
        Greeting.MORNING -> "Chào buổi sáng"
        Greeting.NOON -> "Chào buổi trưa"
        Greeting.AFTERNOON -> "Chào buổi chiều"
        Greeting.EVENING -> "Chào buổi tối"
    }

/** Yellow notice shown while the device is offline and the dashboard shows cached numbers. */
@Composable
fun OfflineBanner(lastSyncedAt: LocalTime?, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    BrutalSurface(
        modifier = modifier.fillMaxWidth().popIn(durationMillis = 500),
        shape = RoundedCornerShape(20.dp),
        color = colors.yellowPale,
        shadowOffset = VfDimens.ShadowM,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(VfRadius.S))
                    .background(colors.surface)
                    .border(VfDimens.Border, colors.ink, RoundedCornerShape(VfRadius.S)),
                contentAlignment = Alignment.Center,
            ) {
                VfIcon(VfIcons.WifiOff, contentDescription = null, size = 18.dp, tint = colors.ink)
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(text = "Bạn đang ngoại tuyến", style = VisionFitTheme.type.labelXL)
                Text(
                    text = lastSyncedAt?.let { "Đang xem số liệu đã lưu lúc ${VnFormat.time(it)} hôm nay." }
                        ?: "Đang xem số liệu đã lưu trên máy.",
                    style = VisionFitTheme.type.caption.copy(fontWeight = FontWeight.Normal),
                    color = colors.onYellowPale,
                )
            }
        }
    }
}
