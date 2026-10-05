package com.visionfit.presentation.review.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.visionfit.core.format.VnFormat
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.MacroPills
import com.visionfit.presentation.designsystem.components.VfPill
import com.visionfit.presentation.designsystem.components.dashedBorder
import com.visionfit.presentation.designsystem.components.hardShadow
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.review.EditableFood

/**
 * One editable dish: name, weight and calories are inline fields. Low-confidence guesses get
 * a coral shadow and a tilted "check again" chip; invalid rows after a save attempt too.
 */
@Composable
fun FoodItemEditor(
    item: EditableFood,
    number: Int,
    isInvalid: Boolean,
    onNameChange: (String) -> Unit,
    onGramsChange: (String) -> Unit,
    onKcalChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VisionFitTheme.colors
    BrutalSurface(
        modifier = modifier.fillMaxWidth().popIn(durationMillis = 500),
        shape = RoundedCornerShape(VfRadius.Card),
        shadowOffset = VfDimens.ShadowM,
        shadowColor = if (item.isLowConfidence || isInvalid) colors.coral else colors.ink,
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.dishMarkers[item.colorIndex % colors.dishMarkers.size])
                        .border(VfDimens.Border, colors.ink, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = number.toString(), style = VisionFitTheme.type.labelXL)
                }
                NameField(
                    value = item.name,
                    onValueChange = onNameChange,
                    label = "Tên món $number",
                    isInvalid = isInvalid && item.name.isBlank(),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(VfDimens.SmallButton)
                        .clip(RoundedCornerShape(VfRadius.M))
                        .background(colors.coralContainer)
                        .clickable(role = Role.Button, onClickLabel = "Xóa món $number", onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    VfIcon(VfIcons.Trash, contentDescription = "Xóa món $number", size = 18.dp, tint = colors.onCoralContainer)
                }
            }
            Row(
                modifier = Modifier.padding(end = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NumberField(
                    label = "Khối lượng",
                    value = item.gramsText,
                    unit = "g",
                    onValueChange = onGramsChange,
                    background = colors.background,
                    labelColor = colors.textSecondary,
                    isInvalid = isInvalid && item.grams <= 0,
                    keyboardType = KeyboardType.Number,
                    accessibilityLabel = "Khối lượng món $number, gam",
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    label = "Năng lượng",
                    value = item.kcalText,
                    unit = "kcal",
                    onValueChange = onKcalChange,
                    background = colors.yellowContainer,
                    labelColor = colors.onYellowContainer,
                    isInvalid = false,
                    keyboardType = KeyboardType.Decimal,
                    accessibilityLabel = "Năng lượng món $number, kcal",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val macros = item.macros
                MacroPills(
                    protein = "${VnFormat.oneDecimal(macros.proteinG)}g",
                    carbs = "${VnFormat.oneDecimal(macros.carbsG)}g",
                    fat = "${VnFormat.oneDecimal(macros.fatG)}g",
                    modifier = Modifier.weight(1f, fill = false),
                )
                ConfidenceChip(item, Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ConfidenceChip(item: EditableFood, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val (text, background) = when {
        item.isManual -> "Tự thêm" to colors.primaryContainer
        item.isLowConfidence -> "Kiểm tra lại · ${item.confidence}%" to colors.yellow
        else -> "AI chắc ${item.confidence}%" to colors.mintContainer
    }
    VfPill(
        text = text,
        background = background,
        contentColor = colors.ink,
        bordered = true,
        style = VisionFitTheme.type.chip.copy(fontWeight = FontWeight.ExtraBold),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
        modifier = modifier.rotate(if (item.isLowConfidence) -3f else 0f),
    )
}

/** Borderless Baloo name input with a dashed underline that turns solid purple on focus. */
@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isInvalid: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = VisionFitTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val underline = when {
        isFocused -> colors.primary
        isInvalid -> colors.coral
        else -> colors.underline
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(40.dp)
            .semantics { contentDescription = label }
            .drawBehind {
                val stroke = 2.dp.toPx()
                val y = size.height - stroke / 2
                drawLine(
                    color = underline,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke,
                    pathEffect = if (isFocused) null else PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                )
            }
            .padding(horizontal = 2.dp),
        singleLine = true,
        textStyle = VisionFitTheme.type.button.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        interactionSource = interactionSource,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(text = "Tên món", style = VisionFitTheme.type.button, color = colors.placeholder)
                }
                inner()
            }
        },
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    background: Color,
    labelColor: Color,
    isInvalid: Boolean,
    keyboardType: KeyboardType,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = VisionFitTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shadow by animateDpAsState(if (isFocused) VfDimens.ShadowM else VfDimens.ShadowXs, label = "numberFieldShadow")
    val lift = with(LocalDensity.current) { if (isFocused) 1.dp.toPx() else 0f }
    val shape = RoundedCornerShape(VfRadius.L)
    Column(
        modifier = modifier
            .graphicsLayer {
                translationX = -lift
                translationY = -lift
            }
            .hardShadow(shape, shadow, if (isFocused) colors.primary else if (isInvalid) colors.coral else colors.ink)
            .clip(shape)
            .background(background)
            .border(VfDimens.Border, colors.ink, shape)
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 5.dp),
    ) {
        Text(text = label, style = VisionFitTheme.type.chip, color = labelColor)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .semantics { contentDescription = accessibilityLabel },
                singleLine = true,
                textStyle = VisionFitTheme.type.numberM.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
                interactionSource = interactionSource,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) Text(text = "0", style = VisionFitTheme.type.numberM, color = colors.placeholder)
                        inner()
                    }
                },
            )
            Text(text = unit, style = VisionFitTheme.type.label)
        }
    }
}

/** Dashed "add a dish the AI missed" button. */
@Composable
fun AddFoodButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    BrutalSurface(
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(VfRadius.XL),
        dashed = true,
        shadowOffset = 0.dp,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.mint)
                    .border(VfDimens.Border, colors.ink, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                VfIcon(VfIcons.Plus, contentDescription = null, size = 14.dp, tint = colors.ink, strokeWidth = 3.4f)
            }
            Text(text = "Thêm món AI bỏ sót (vd. cơm trắng)", style = VisionFitTheme.type.labelXL)
        }
    }
}

/** Shown when every dish was removed (or manual entry starts empty). */
@Composable
fun EmptyTrayCard(modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val shape = RoundedCornerShape(VfRadius.Card)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .dashedBorder(VfDimens.Border, colors.ink, shape, dash = 7.dp, gap = 5.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Mâm trống trơn! Thêm món hoặc chụp lại nhé.",
            style = VisionFitTheme.type.cardTitle.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EditHint(modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VfIcon(VfIcons.Pencil, contentDescription = null, size = 16.dp, tint = colors.textSecondary, strokeWidth = 2.2f)
        Text(
            text = "AI đoán sai? Chạm vào tên món, số gram hoặc calo để sửa.",
            style = VisionFitTheme.type.bodySmall,
            color = colors.textSecondary,
        )
    }
}
