package com.visionfit.presentation.auth

import com.visionfit.domain.model.AuthError
import com.visionfit.domain.usecase.CredentialError

enum class AuthMode { LOGIN, REGISTER }

enum class LegalDocument(val url: String) {
    TERMS("https://example.com/visionfit/terms"),
    PRIVACY("https://example.com/visionfit/privacy"),
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: CredentialError? = null,
    val passwordError: CredentialError? = null,
    val confirmPasswordError: CredentialError? = null,
    /** Error returned by the server for the last submit. */
    val submitError: AuthError? = null,
    val isSubmitting: Boolean = false,
    val isSendingReset: Boolean = false,
) {
    val isRegister: Boolean get() = mode == AuthMode.REGISTER
}

sealed interface AuthEvent {
    data class ModeSelected(val mode: AuthMode) : AuthEvent
    data class EmailChanged(val value: String) : AuthEvent
    data class PasswordChanged(val value: String) : AuthEvent
    data class ConfirmPasswordChanged(val value: String) : AuthEvent
    data object TogglePasswordVisibility : AuthEvent
    data object Submit : AuthEvent
    data object ForgotPassword : AuthEvent
    data class LegalLinkClicked(val document: LegalDocument) : AuthEvent
}

sealed interface AuthEffect {
    data object NavigateToDashboard : AuthEffect
    data object NavigateToOnboarding : AuthEffect
    data class OpenUrl(val url: String) : AuthEffect
    data class ResetLinkSent(val email: String) : AuthEffect
    data object ResetLinkFailed : AuthEffect
}
