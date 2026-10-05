package com.visionfit.presentation.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.visionfit.resources.Res
import com.visionfit.resources.baloo2_bold
import com.visionfit.resources.baloo2_extrabold
import com.visionfit.resources.baloo2_medium
import com.visionfit.resources.baloo2_semibold
import com.visionfit.resources.bevietnampro_bold
import com.visionfit.resources.bevietnampro_medium
import com.visionfit.resources.bevietnampro_regular
import com.visionfit.resources.bevietnampro_semibold
import org.jetbrains.compose.resources.Font

/**
 * Two families, as in the design: *Baloo 2* (rounded, extra bold) for headings, numbers and
 * buttons, *Be Vietnam Pro* for body copy, labels and inputs.
 */
@Immutable
data class VisionFitTypography(
    // Big numbers (Baloo 2, line height 1)
    val numberHero: TextStyle,
    val numberXL: TextStyle,
    val numberL: TextStyle,
    val numberStat: TextStyle,
    val numberSummary: TextStyle,
    val numberMeal: TextStyle,
    val numberM: TextStyle,
    val numberS: TextStyle,

    // Headings (Baloo 2)
    val displayL: TextStyle,
    val headlineL: TextStyle,
    val headlineM: TextStyle,
    val headlineS: TextStyle,
    val titleXL: TextStyle,
    val sectionTitle: TextStyle,
    val sectionTitleSmall: TextStyle,
    val button: TextStyle,
    val buttonSmall: TextStyle,
    val cardTitle: TextStyle,
    val labelXL: TextStyle,
    val labelL: TextStyle,
    val labelM: TextStyle,
    val labelS: TextStyle,
    val overline: TextStyle,
    val badge: TextStyle,

    // Body (Be Vietnam Pro)
    val input: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val captionStrong: TextStyle,
    val chip: TextStyle,
    val micro: TextStyle,
)

/**
 * Center glyphs in the line box without trimming: on Skia, trimming makes lines shorter than
 * Baloo's tall natural metrics grow back to full height.
 */
private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

@Composable
private fun balooFamily() = FontFamily(
    Font(Res.font.baloo2_medium, FontWeight.Medium),
    Font(Res.font.baloo2_semibold, FontWeight.SemiBold),
    Font(Res.font.baloo2_bold, FontWeight.Bold),
    Font(Res.font.baloo2_extrabold, FontWeight.ExtraBold),
)

@Composable
private fun beVietnamFamily() = FontFamily(
    Font(Res.font.bevietnampro_regular, FontWeight.Normal),
    Font(Res.font.bevietnampro_medium, FontWeight.Medium),
    Font(Res.font.bevietnampro_semibold, FontWeight.SemiBold),
    Font(Res.font.bevietnampro_bold, FontWeight.Bold),
)

@Composable
internal fun rememberVisionFitTypography(): VisionFitTypography {
    val display = balooFamily()
    val text = beVietnamFamily()
    return remember(display, text) { buildTypography(display, text) }
}

private fun buildTypography(display: FontFamily, text: FontFamily): VisionFitTypography {
    fun baloo(size: Int, lineHeight: Double = 1.15, tracking: Double = 0.0, weight: FontWeight = FontWeight.ExtraBold) =
        TextStyle(
            fontFamily = display,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = lineHeight.em,
            letterSpacing = tracking.em,
            lineHeightStyle = TightLineHeight,
        )

    fun body(size: Int, weight: FontWeight = FontWeight.Normal, lineHeight: Double = 1.4, tracking: Double = 0.0) =
        TextStyle(
            fontFamily = text,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = lineHeight.em,
            letterSpacing = tracking.em,
        )

    // Big numbers use 1.02em, not 1.0em: with a multiplier of exactly 1 the Skia text engine
    // (iOS / desktop) falls back to the font's own, much taller line box.
    return VisionFitTypography(
        numberHero = baloo(76, lineHeight = 1.02, tracking = -0.03),
        numberXL = baloo(48, lineHeight = 1.02, tracking = -0.02),
        numberL = baloo(46, lineHeight = 1.02, tracking = -0.02),
        numberStat = baloo(38, lineHeight = 1.02),
        numberSummary = baloo(34, lineHeight = 1.05),
        numberMeal = baloo(26, lineHeight = 1.02),
        numberM = baloo(22, lineHeight = 1.1),
        numberS = baloo(19, lineHeight = 1.15),

        displayL = baloo(32, lineHeight = 1.05, tracking = -0.01),
        headlineL = baloo(30, lineHeight = 1.1, tracking = -0.01),
        headlineM = baloo(28, lineHeight = 1.15, tracking = -0.01),
        headlineS = baloo(26, lineHeight = 1.1, tracking = -0.01),
        titleXL = baloo(24, lineHeight = 1.1, tracking = -0.01),
        sectionTitle = baloo(20, lineHeight = 1.2),
        sectionTitleSmall = baloo(19, lineHeight = 1.2),
        button = baloo(19, lineHeight = 1.2),
        buttonSmall = baloo(18, lineHeight = 1.2),
        cardTitle = baloo(17, lineHeight = 1.15),
        labelXL = baloo(16, lineHeight = 1.2),
        labelL = baloo(15, lineHeight = 1.2),
        labelM = baloo(14, lineHeight = 1.2),
        labelS = baloo(13, lineHeight = 1.2),
        overline = baloo(13, lineHeight = 1.2, tracking = 0.1),
        badge = baloo(11, lineHeight = 1.2),

        input = body(15),
        body = body(14, lineHeight = 1.5),
        bodySmall = body(13, lineHeight = 1.45),
        bodyStrong = body(14, FontWeight.Bold, lineHeight = 1.25),
        label = body(13, FontWeight.Bold, lineHeight = 1.3),
        caption = body(12, FontWeight.SemiBold, lineHeight = 1.4),
        captionStrong = body(12, FontWeight.Bold, lineHeight = 1.4),
        chip = body(11, FontWeight.Bold, lineHeight = 1.35),
        micro = body(10, FontWeight.Bold, lineHeight = 1.3, tracking = 0.08),
    )
}

internal val LocalVisionFitTypography = staticCompositionLocalOf<VisionFitTypography> {
    error("VisionFitTypography not provided. Wrap the UI in VisionFitTheme.")
}
