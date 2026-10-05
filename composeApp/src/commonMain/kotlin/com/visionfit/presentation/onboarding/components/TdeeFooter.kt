package com.visionfit.presentation.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.visionfit.core.format.VnFormat
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/** "TDEE ước tính → Mục tiêu mỗi ngày", updated live as the user edits. */
@Composable
fun TdeeSummaryRow(tdeeKcal: Int, targetKcal: Int, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(VfRadius.M)
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .background(colors.background)
                .border(VfDimens.Border, colors.ink, shape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(text = "TDEE ước tính", style = VisionFitTheme.type.chip, color = colors.textSecondary)
            KcalValue(tdeeKcal)
        }
        VfIcon(VfIcons.ArrowRight, contentDescription = null, size = 22.dp, tint = colors.ink)
        BrutalSurface(
            modifier = Modifier.weight(1f).rotate(-2f),
            shape = shape,
            color = colors.yellow,
            shadowOffset = VfDimens.ShadowS,
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(text = "Mục tiêu mỗi ngày", style = VisionFitTheme.type.chip, color = colors.onYellow)
                KcalValue(targetKcal)
            }
        }
    }
}

@Composable
private fun KcalValue(kcal: Int, color: Color = VisionFitTheme.colors.ink) {
    Text(
        text = buildAnnotatedString {
            append(VnFormat.thousands(kcal))
            withStyle(SpanStyle(fontSize = 12.sp)) { append(" kcal") }
        },
        style = VisionFitTheme.type.numberS,
        color = color,
    )
}
