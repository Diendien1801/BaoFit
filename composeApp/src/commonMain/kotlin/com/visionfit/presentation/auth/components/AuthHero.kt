package com.visionfit.presentation.auth.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.floating
import com.visionfit.presentation.designsystem.components.spinning
import com.visionfit.presentation.designsystem.components.wiggle
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.layout.ScaleDownToFit
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.resources.Res
import com.visionfit.resources.meal_family_tray
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Brand row, the floating "recognized dishes" collage and the headline.
 *
 * With [shrinkToFit] the hero accepts whatever height its parent gives it: the collage scales
 * down into the space left between brand and headline, and is left out once it would drop
 * below [MinCollageScale]. Brand and headline always stay.
 */
@Composable
fun AuthHero(modifier: Modifier = Modifier, shrinkToFit: Boolean = false) {
    Column(
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 14.dp),
        // Only matters once the collage is left out: brand and headline then share the spare
        // room evenly instead of leaving a hole where the collage was.
        verticalArrangement = if (shrinkToFit) Arrangement.SpaceEvenly else Arrangement.Top,
    ) {
        BrandRow()
        HeroCollage(
            modifier = Modifier
                .then(if (shrinkToFit) Modifier.weight(1f, fill = false) else Modifier)
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally),
            minScale = if (shrinkToFit) MinCollageScale else 0f,
        )
        HeroHeadline(Modifier.padding(top = 4.dp))
    }
}

/**
 * Below this the collage is too small to be worth its space. Down to here it still reads as
 * "a tray with dish stickers", which matters more than the sticker text itself.
 */
private const val MinCollageScale = 0.4f

@Composable
private fun BrandRow() {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BrutalSurface(
            modifier = Modifier.size(40.dp).rotate(-6f),
            shape = RoundedCornerShape(13.dp),
            color = VisionFitTheme.colors.yellow,
            shadowOffset = VfDimens.ShadowS,
            contentAlignment = Alignment.Center,
        ) {
            VfIcon(VfIcons.ScanLeaf, contentDescription = null, size = 22.dp, tint = VisionFitTheme.colors.ink)
        }
        Text(text = "VisionFit", style = VisionFitTheme.type.titleXL, color = VisionFitTheme.colors.surface)
        Box(Modifier.weight(1f))
        Text(
            text = "AI dinh dưỡng",
            style = VisionFitTheme.type.chip,
            color = VisionFitTheme.colors.surface,
            modifier = Modifier
                .border(VfDimens.Border, Color.White.copy(alpha = 0.4f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * 342 × 196 composition positioned like the design. It never reflows: on phones narrower than
 * the design it scales down as a whole, so the stickers keep their places around the photo.
 */
@Composable
private fun HeroCollage(modifier: Modifier = Modifier, minScale: Float = 0f) {
    val colors = VisionFitTheme.colors
    ScaleDownToFit(designWidth = 342.dp, designHeight = 196.dp, modifier = modifier, minScale = minScale) {
        BrutalSurface(
            modifier = Modifier
                .offset(x = 92.dp, y = 14.dp)
                .wiggle(fromDegrees = -5f, toDegrees = -2f, periodMillis = 5_000)
                .width(164.dp),
            shape = RoundedCornerShape(26.dp),
            shadowOffset = VfDimens.ShadowXL,
        ) {
            Image(
                painter = painterResource(Res.drawable.meal_family_tray),
                contentDescription = "Mâm cơm gia đình Việt chụp từ trên xuống",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(7.dp)
                    .fillMaxWidth()
                    .height(146.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(colors.inkSoft),
            )
        }
        DishSticker(
            name = "Thịt kho",
            kcal = "345 kcal",
            background = colors.surface,
            kcalColor = colors.primary,
            rotation = -6f,
            floatPeriod = 3_000,
            floatDelay = 0,
            modifier = Modifier.offset(x = 0.dp, y = 4.dp),
        )
        DishSticker(
            name = "Canh dưa chuột",
            kcal = "50 kcal",
            background = colors.mint,
            kcalColor = colors.onMint,
            rotation = 6f,
            floatPeriod = 3_400,
            floatDelay = 500,
            modifier = Modifier.align(Alignment.TopEnd).offset(y = 16.dp),
        )
        DishSticker(
            name = "Đậu phụ sốt cà",
            kcal = "190 kcal",
            background = colors.yellow,
            kcalColor = colors.onYellowContainer,
            rotation = 4f,
            floatPeriod = 3_200,
            floatDelay = 1_000,
            modifier = Modifier.offset(x = 4.dp, y = 118.dp),
        )
        SpinningStamp(
            text = "CHỤP • NHẬN DIỆN • TÍNH CALO •",
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 98.dp),
        )
    }
}

@Composable
private fun DishSticker(
    name: String,
    kcal: String,
    background: Color,
    kcalColor: Color,
    rotation: Float,
    floatPeriod: Int,
    floatDelay: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.rotate(rotation)) {
        BrutalSurface(
            modifier = Modifier.floating(amplitude = 7.dp, periodMillis = floatPeriod, delayMillis = floatDelay),
            shape = RoundedCornerShape(14.dp),
            color = background,
            shadowOffset = VfDimens.ShadowS,
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(text = name, style = VisionFitTheme.type.labelM)
                Text(text = kcal, style = VisionFitTheme.type.chip, color = kcalColor)
            }
        }
    }
}

/** Coral seal with text running around its edge, slowly rotating. */
@Composable
private fun SpinningStamp(text: String, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val measurer = rememberTextMeasurer()
    val style = VisionFitTheme.type.badge.copy(fontSize = 10.sp, color = colors.ink)
    val glyphs = remember(text, style) { text.map { measurer.measure(it.toString(), style) } }
    Box(modifier = modifier.size(94.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(94.dp).spinning(14_000)) {
            drawCircle(colors.coral, radius = 45.dp.toPx())
            drawCircle(colors.ink, radius = 45.dp.toPx(), style = Stroke(VfDimens.Border.toPx()))
            val radius = 33.dp.toPx()
            val spacing = 1.2.dp.toPx()
            var angle = PI
            glyphs.forEach { glyph ->
                val width = glyph.size.width
                val mid = angle + (width / 2f) / radius
                val anchor = Offset(center.x + radius * cos(mid).toFloat(), center.y + radius * sin(mid).toFloat())
                rotate(degrees = (mid * 180 / PI).toFloat() + 90f, pivot = anchor) {
                    drawText(glyph, topLeft = Offset(anchor.x - width / 2f, anchor.y - glyph.firstBaseline))
                }
                angle += (width + spacing) / radius
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(VfDimens.Border, colors.ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            VfIcon(VfIcons.Camera, contentDescription = null, size = 20.dp, tint = colors.ink)
        }
    }
}

@Composable
private fun HeroHeadline(modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val style = VisionFitTheme.type.displayL
    Column(modifier = modifier.semantics(mergeDescendants = true) { heading() }) {
        Text(text = "Chụp một tấm,", style = style, color = colors.surface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "biết ngay ", style = style, color = colors.surface)
            BrutalSurface(
                modifier = Modifier.rotate(-3f),
                shape = RoundedCornerShape(12.dp),
                color = colors.yellow,
                shadowOffset = VfDimens.ShadowS,
            ) {
                Text(
                    text = "calo!",
                    style = style,
                    color = colors.ink,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
        }
    }
}
