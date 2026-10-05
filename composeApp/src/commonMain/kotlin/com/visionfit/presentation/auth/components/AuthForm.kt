package com.visionfit.presentation.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.visionfit.presentation.auth.AuthMode
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.hardShadow
import com.visionfit.presentation.designsystem.icons.IconSpec
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

/** "Đăng nhập | Đăng ký" segmented control. */
@Composable
fun AuthModeSwitch(
    mode: AuthMode,
    onModeSelected: (AuthMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(VfRadius.XL)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VisionFitTheme.colors.background)
            .border(VfDimens.Border, VisionFitTheme.colors.ink, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeTab("Đăng nhập", selected = mode == AuthMode.LOGIN, onClick = { onModeSelected(AuthMode.LOGIN) }, Modifier.weight(1f))
        ModeTab("Đăng ký", selected = mode == AuthMode.REGISTER, onClick = { onModeSelected(AuthMode.REGISTER) }, Modifier.weight(1f))
    }
}

@Composable
private fun ModeTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val background by animateColorAsState(if (selected) VisionFitTheme.colors.yellow else Color.Transparent, label = "tabBg")
    val border by animateColorAsState(if (selected) VisionFitTheme.colors.ink else Color.Transparent, label = "tabBorder")
    BrutalSurface(
        modifier = modifier.height(VfDimens.SmallButton).semantics { this.selected = selected },
        shape = RoundedCornerShape(13.dp),
        color = background,
        borderColor = border,
        shadowOffset = if (selected) VfDimens.ShadowXs else 0.dp,
        onClick = onClick,
        role = Role.Tab,
        pressDepth = if (selected) 0.dp else 2.dp,
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = VisionFitTheme.type.cardTitle)
    }
}

/**
 * Labelled input with a tinted icon tile on the left. Focus lifts the field with a purple
 * shadow; an error turns the shadow coral and shows the message below.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: IconSpec,
    leadingContainer: Color,
    modifier: Modifier = Modifier,
    error: String? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
) {
    val colors = VisionFitTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shadowColor by animateColorAsState(
        when {
            isFocused -> colors.primary
            error != null -> colors.coral
            else -> colors.ink
        },
        label = "fieldShadowColor",
    )
    val shadowOffset by animateDpAsState(if (isFocused || error != null) VfDimens.ShadowM else VfDimens.ShadowXs, label = "fieldShadow")
    val shape = RoundedCornerShape(VfRadius.L)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = VisionFitTheme.type.label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(VfDimens.InputHeight)
                .hardShadow(shape, shadowOffset, shadowColor)
                .clip(shape)
                .background(colors.surface)
                .border(VfDimens.Border, colors.ink, shape)
                // Taps on the icon tile or padding also focus the field.
                .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } },
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .semantics { if (error != null) error(error) }
                    .padding(start = 48.dp, end = if (onTogglePasswordVisibility != null) 54.dp else 16.dp),
                enabled = enabled,
                singleLine = true,
                textStyle = VisionFitTheme.type.input.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.primary),
                visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(text = placeholder, style = VisionFitTheme.type.input, color = colors.placeholder, maxLines = 1)
                        }
                        innerTextField()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(VfRadius.Xs))
                    .background(leadingContainer),
                contentAlignment = Alignment.Center,
            ) {
                VfIcon(leadingIcon, contentDescription = null, size = 18.dp, tint = colors.ink)
            }
            if (onTogglePasswordVisibility != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 5.dp)
                        .size(VfDimens.SmallButton)
                        .clip(RoundedCornerShape(VfRadius.S))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = if (isPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                            onClick = onTogglePasswordVisibility,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    VfIcon(
                        icon = if (isPasswordVisible) VfIcons.EyeOff else VfIcons.Eye,
                        contentDescription = if (isPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                        tint = colors.textSecondary,
                    )
                }
            }
        }
        if (error != null) {
            Text(text = error, style = VisionFitTheme.type.captionStrong, color = colors.error)
        }
    }
}
