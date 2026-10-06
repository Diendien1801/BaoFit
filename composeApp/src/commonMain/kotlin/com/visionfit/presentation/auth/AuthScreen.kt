package com.visionfit.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionfit.core.mvi.CollectEffects
import com.visionfit.domain.model.AuthError
import com.visionfit.domain.usecase.CredentialError
import com.visionfit.presentation.auth.components.AuthHero
import com.visionfit.presentation.auth.components.AuthModeSwitch
import com.visionfit.presentation.auth.components.AuthTextField
import com.visionfit.presentation.common.LocalAppMessenger
import com.visionfit.presentation.common.containerViewModel
import com.visionfit.presentation.designsystem.components.BrutalSurface
import com.visionfit.presentation.designsystem.components.VfPrimaryButton
import com.visionfit.presentation.designsystem.components.VfTextButton
import com.visionfit.presentation.designsystem.components.popIn
import com.visionfit.presentation.designsystem.components.topRoundedBorder
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.layout.LocalWindowLayout
import com.visionfit.presentation.designsystem.layout.VfContentWidth
import com.visionfit.presentation.designsystem.layout.WidthClass
import com.visionfit.presentation.designsystem.layout.maxContentWidth
import com.visionfit.presentation.designsystem.layout.safeArea
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VfRadius
import com.visionfit.presentation.designsystem.theme.VisionFitTheme
import com.visionfit.presentation.preview.VisionFitPreview

/** Stateful entry point: wires the ViewModel, one-shot effects and navigation. */
@Composable
fun AuthRoute(
    onNavigateToDashboard: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: AuthViewModel = containerViewModel { AuthViewModel(authRepository, validateCredentials) },
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val messenger = LocalAppMessenger.current
    val uriHandler = LocalUriHandler.current
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            AuthEffect.NavigateToDashboard -> onNavigateToDashboard()
            AuthEffect.NavigateToOnboarding -> onNavigateToOnboarding()
            is AuthEffect.OpenUrl -> runCatching { uriHandler.openUri(effect.url) }
                .onFailure { messenger.show("Không mở được liên kết, thử lại sau nhé.") }
            is AuthEffect.ResetLinkSent -> messenger.show("Đã gửi link đặt lại mật khẩu tới ${effect.email}")
            AuthEffect.ResetLinkFailed -> messenger.show("Chưa gửi được email, thử lại sau nhé.")
        }
    }
    AuthScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun AuthScreen(state: AuthUiState, onEvent: (AuthEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.primary)) {
        Box(
            Modifier
                .offset(x = maxWidth - 150.dp, y = 120.dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(colors.primaryBlob),
        )
        Box(
            Modifier
                .offset(x = (-60).dp, y = (-60).dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(colors.primaryBlob),
        )
        val layout = LocalWindowLayout.current
        when {
            layout.usesTwoPanes -> AuthTwoPanes(state, onEvent)
            layout.widthClass != WidthClass.COMPACT -> AuthCentered(state, onEvent)
            else -> AuthStacked(state, onEvent, viewportHeight = maxHeight)
        }
    }
}

/**
 * Phones: the hero on top, the form as a sheet reaching the bottom edge.
 *
 * The whole form, button included, must be on screen, and most phones are shorter than the
 * 844 dp design once the system bars are taken off. So the sheet keeps its natural height and
 * the hero gets what is left: its collage shrinks, then steps aside. Only when brand, headline
 * and form together are taller than the screen (or the keyboard is up) does the page scroll.
 */
@Composable
private fun AuthStacked(state: AuthUiState, onEvent: (AuthEvent) -> Unit, viewportHeight: Dp) {
    Layout(
        contents = listOf<@Composable () -> Unit>(
            {
                AuthHero(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .maxContentWidth(SheetMaxWidth),
                    shrinkToFit = true,
                )
            },
            { AuthSheet(state = state, onEvent = onEvent, modifier = Modifier.maxContentWidth(SheetMaxWidth)) },
        ),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) { (heroMeasurables, sheetMeasurables), constraints ->
        val width = constraints.maxWidth
        val viewport = viewportHeight.roundToPx()
        val hero = heroMeasurables.single()
        val heroNatural = hero.maxIntrinsicHeight(width)
        val heroMin = hero.minIntrinsicHeight(width)
        // Down to the bottom edge when there is room to spare, its natural height otherwise.
        val sheet = sheetMeasurables.single().measure(
            Constraints(minWidth = width, maxWidth = width, minHeight = (viewport - heroNatural).coerceAtLeast(0)),
        )
        val heroHeight = (viewport - sheet.height).coerceIn(heroMin, maxOf(heroMin, heroNatural))
        val heroPlaceable = hero.measure(Constraints.fixed(width, heroHeight))
        layout(width, heroHeight + sheet.height) {
            heroPlaceable.place(0, 0)
            sheet.place(0, heroHeight)
        }
    }
}

/** Wide and landscape windows: the hero on the left, the form as a floating card on the right. */
@Composable
private fun AuthTwoPanes(state: AuthUiState, onEvent: (AuthEvent) -> Unit) {
    val gutter = LocalWindowLayout.current.gutter
    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeArea)
            .maxContentWidth(VfContentWidth.TwoPane)
            .padding(horizontal = gutter),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            AuthHero(Modifier.verticalScroll(rememberScrollState()))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.ime),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                // Room below and to the right for the card's hard shadow.
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 22.dp, end = VfDimens.ShadowXL),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AuthCard(state = state, onEvent = onEvent, modifier = Modifier.widthIn(max = VfContentWidth.Form))
            }
        }
    }
}

/**
 * Portrait tablets: hero and form card centered together. A sheet stretched down a tall
 * screen would leave the button far away from the fields.
 */
@Composable
private fun AuthCentered(state: AuthUiState, onEvent: (AuthEvent) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeArea.union(WindowInsets.ime)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalWindowLayout.current.gutter, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuthHero(Modifier.widthIn(max = VfContentWidth.Form))
            AuthCard(state = state, onEvent = onEvent, modifier = Modifier.widthIn(max = VfContentWidth.Form))
        }
    }
}

/** Sheet width cap; a little wider than the card so the phone layout keeps its proportions. */
private val SheetMaxWidth = 560.dp

@Composable
private fun AuthSheet(state: AuthUiState, onEvent: (AuthEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = VisionFitTheme.colors
    val sheetShape = RoundedCornerShape(topStart = VfRadius.Sheet, topEnd = VfRadius.Sheet)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .topRoundedBorder(VfDimens.Border, colors.ink, VfRadius.Sheet)
            .clip(sheetShape)
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AuthFields(state, onEvent)
        Spacer(Modifier.weight(1f))
        AuthActions(state = state, onEvent = onEvent)
    }
}

@Composable
private fun AuthCard(state: AuthUiState, onEvent: (AuthEvent) -> Unit, modifier: Modifier = Modifier) {
    BrutalSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VfRadius.Sheet),
        shadowOffset = VfDimens.ShadowXL,
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuthFields(state, onEvent)
            AuthActions(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun ColumnScope.AuthFields(state: AuthUiState, onEvent: (AuthEvent) -> Unit) {
    val colors = VisionFitTheme.colors
    val focusManager = LocalFocusManager.current
    AuthModeSwitch(mode = state.mode, onModeSelected = { onEvent(AuthEvent.ModeSelected(it)) })

    AuthTextField(
        value = state.email,
        onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
        label = "Email",
        placeholder = "ban@email.com",
        leadingIcon = VfIcons.Mail,
        leadingContainer = colors.primaryContainer,
        error = state.emailError?.message(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
    )
    AuthTextField(
        value = state.password,
        onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
        label = "Mật khẩu",
        placeholder = "Tối thiểu 8 ký tự",
        leadingIcon = VfIcons.Lock,
        leadingContainer = colors.yellowContainer,
        error = state.passwordError?.message(),
        isPassword = true,
        isPasswordVisible = state.isPasswordVisible,
        onTogglePasswordVisibility = { onEvent(AuthEvent.TogglePasswordVisibility) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (state.isRegister) ImeAction.Next else ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = {
                focusManager.clearFocus()
                onEvent(AuthEvent.Submit)
            },
        ),
    )
    AnimatedVisibility(
        visible = state.isRegister,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        AuthTextField(
            value = state.confirmPassword,
            onValueChange = { onEvent(AuthEvent.ConfirmPasswordChanged(it)) },
            label = "Nhập lại mật khẩu",
            placeholder = "Nhập lại để xác nhận",
            leadingIcon = VfIcons.ShieldCheck,
            leadingContainer = colors.mintContainer,
            error = state.confirmPasswordError?.message(),
            isPassword = true,
            isPasswordVisible = state.isPasswordVisible,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onEvent(AuthEvent.Submit)
            }),
            modifier = Modifier.popIn(durationMillis = 450),
        )
    }
    if (!state.isRegister) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            VfTextButton(
                text = if (state.isSendingReset) "Đang gửi…" else "Quên mật khẩu?",
                onClick = { onEvent(AuthEvent.ForgotPassword) },
                contentPadding = PaddingValues(start = 8.dp),
            )
        }
    }
}

@Composable
private fun AuthActions(state: AuthUiState, onEvent: (AuthEvent) -> Unit) {
    val colors = VisionFitTheme.colors
    state.submitError?.let { SubmitErrorBanner(it.message()) }
    VfPrimaryButton(
        text = if (state.isRegister) "Tạo tài khoản" else "Đăng nhập",
        onClick = { onEvent(AuthEvent.Submit) },
        loading = state.isSubmitting,
    )
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = colors.ink, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
    )
    val legal = buildAnnotatedString {
        append("Bằng việc tiếp tục, bạn đồng ý với ")
        withLink(LinkAnnotation.Clickable("terms", linkStyles) { onEvent(AuthEvent.LegalLinkClicked(LegalDocument.TERMS)) }) {
            append("Điều khoản")
        }
        append(" và ")
        withLink(LinkAnnotation.Clickable("privacy", linkStyles) { onEvent(AuthEvent.LegalLinkClicked(LegalDocument.PRIVACY)) }) {
            append("Chính sách quyền riêng tư")
        }
        append(".")
    }
    Text(
        text = legal,
        style = VisionFitTheme.type.caption.copy(fontWeight = FontWeight.Normal, lineHeight = VisionFitTheme.type.body.lineHeight),
        color = colors.textMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SubmitErrorBanner(message: String) {
    BrutalSurface(
        modifier = Modifier.fillMaxWidth().popIn(durationMillis = 400),
        shape = RoundedCornerShape(VfRadius.M),
        color = VisionFitTheme.colors.coralContainer,
        shadowOffset = VfDimens.ShadowXs,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VfIcon(VfIcons.Alert, contentDescription = null, size = 18.dp, tint = VisionFitTheme.colors.error)
            Text(text = message, style = VisionFitTheme.type.bodySmall, color = VisionFitTheme.colors.onCoralContainer)
        }
    }
}

private fun CredentialError.message(): String = when (this) {
    CredentialError.EMAIL_BLANK -> "Nhập email của bạn nhé"
    CredentialError.EMAIL_INVALID -> "Email chưa đúng định dạng"
    CredentialError.PASSWORD_BLANK -> "Nhập mật khẩu nhé"
    CredentialError.PASSWORD_TOO_SHORT -> "Mật khẩu cần tối thiểu 8 ký tự"
    CredentialError.CONFIRMATION_MISMATCH -> "Mật khẩu nhập lại chưa khớp"
}

private fun AuthError.message(): String = when (this) {
    AuthError.INVALID_CREDENTIALS -> "Email hoặc mật khẩu chưa đúng. Kiểm tra lại giúp mình nhé!"
    AuthError.EMAIL_ALREADY_REGISTERED -> "Email này đã có tài khoản rồi. Chuyển sang Đăng nhập nhé?"
    AuthError.ACCOUNT_LOCKED -> "Tài khoản đang tạm khóa. Liên hệ hỗ trợ để mở lại nha."
    AuthError.NETWORK -> "Không kết nối được. Kiểm tra mạng rồi thử lại nhé."
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AuthLoginPreview() {
    VisionFitPreview { AuthScreen(state = AuthUiState(), onEvent = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AuthRegisterErrorsPreview() {
    VisionFitPreview {
        AuthScreen(
            state = AuthUiState(
                mode = AuthMode.REGISTER,
                email = "an@visionfit",
                password = "123",
                confirmPassword = "1234",
                emailError = CredentialError.EMAIL_INVALID,
                passwordError = CredentialError.PASSWORD_TOO_SHORT,
                confirmPasswordError = CredentialError.CONFIRMATION_MISMATCH,
                submitError = AuthError.EMAIL_ALREADY_REGISTERED,
            ),
            onEvent = {},
        )
    }
}

@Preview(widthDp = 844, heightDp = 390)
@Composable
private fun AuthLandscapePreview() {
    VisionFitPreview { AuthScreen(state = AuthUiState(), onEvent = {}) }
}
