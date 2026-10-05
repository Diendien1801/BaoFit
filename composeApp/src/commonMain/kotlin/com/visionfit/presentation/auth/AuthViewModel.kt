package com.visionfit.presentation.auth

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.domain.model.AuthResult
import com.visionfit.domain.repository.AuthRepository
import com.visionfit.domain.usecase.ValidateCredentialsUseCase
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val validateCredentials: ValidateCredentialsUseCase,
) : MviViewModel<AuthUiState, AuthEvent, AuthEffect>(AuthUiState()) {

    override fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.ModeSelected -> updateState {
                copy(
                    mode = event.mode,
                    confirmPassword = "",
                    passwordError = null,
                    confirmPasswordError = null,
                    submitError = null,
                )
            }
            is AuthEvent.EmailChanged -> updateState { copy(email = event.value, emailError = null, submitError = null) }
            is AuthEvent.PasswordChanged -> updateState { copy(password = event.value, passwordError = null, submitError = null) }
            is AuthEvent.ConfirmPasswordChanged -> updateState {
                copy(confirmPassword = event.value, confirmPasswordError = null, submitError = null)
            }
            AuthEvent.TogglePasswordVisibility -> updateState { copy(isPasswordVisible = !isPasswordVisible) }
            AuthEvent.Submit -> submit()
            AuthEvent.ForgotPassword -> requestPasswordReset()
            is AuthEvent.LegalLinkClicked -> sendEffect(AuthEffect.OpenUrl(event.document.url))
        }
    }

    private fun submit() {
        val state = currentState
        if (state.isSubmitting) return
        val validation = validateCredentials(
            email = state.email,
            password = state.password,
            confirmation = state.confirmPassword.takeIf { state.isRegister },
        )
        if (!validation.isValid) {
            updateState {
                copy(
                    emailError = validation.email,
                    passwordError = validation.password,
                    confirmPasswordError = validation.confirmation,
                )
            }
            return
        }
        updateState { copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val email = state.email.trim()
            val result = if (state.isRegister) {
                authRepository.register(email, state.password)
            } else {
                authRepository.login(email, state.password)
            }
            when (result) {
                is AuthResult.Success -> {
                    updateState { copy(isSubmitting = false) }
                    sendEffect(if (state.isRegister) AuthEffect.NavigateToOnboarding else AuthEffect.NavigateToDashboard)
                }
                is AuthResult.Failure -> updateState { copy(isSubmitting = false, submitError = result.error) }
            }
        }
    }

    private fun requestPasswordReset() {
        val state = currentState
        if (state.isSendingReset) return
        val emailError = validateCredentials(state.email, password = "", confirmation = null).email
        if (emailError != null) {
            updateState { copy(emailError = emailError) }
            return
        }
        updateState { copy(isSendingReset = true) }
        viewModelScope.launch {
            val email = state.email.trim()
            val sent = authRepository.requestPasswordReset(email)
            updateState { copy(isSendingReset = false) }
            sendEffect(if (sent) AuthEffect.ResetLinkSent(email) else AuthEffect.ResetLinkFailed)
        }
    }
}
