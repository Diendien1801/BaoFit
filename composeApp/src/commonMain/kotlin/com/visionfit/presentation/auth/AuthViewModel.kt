package com.visionfit.presentation.auth

import androidx.lifecycle.viewModelScope
import com.visionfit.core.mvi.MviViewModel
import com.visionfit.core.log.AppLog
import com.visionfit.domain.model.AuthResult
import com.visionfit.domain.model.ProfileStatus
import com.visionfit.domain.repository.AuthRepository
import com.visionfit.domain.repository.ProfileRepository
import com.visionfit.domain.usecase.ValidateCredentialsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
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
            AppLog.d("AuthViewModel", "Submit ${state.mode} (email=$email)")
            val result = if (state.isRegister) {
                authRepository.register(email, state.password)
            } else {
                authRepository.login(email, state.password)
            }
            when (result) {
                is AuthResult.Success -> {
                    val next = if (state.isRegister) AuthEffect.NavigateToOnboarding else destinationAfterLogin()
                    AppLog.d("AuthViewModel", "${state.mode} succeeded, next: $next")
                    updateState { copy(isSubmitting = false) }
                    sendEffect(next)
                }
                is AuthResult.Failure -> {
                    AppLog.w("AuthViewModel", "${state.mode} failed: ${result.error} (details in the [Auth] lines above)")
                    updateState { copy(isSubmitting = false, submitError = result.error) }
                }
            }
        }
    }

    /**
     * Waits for the profile (the button keeps spinning meanwhile), so someone who never finished
     * onboarding goes back to it and the dashboard never flashes default targets.
     * A failed download still opens the dashboard, which offers a retry.
     */
    private suspend fun destinationAfterLogin(): AuthEffect =
        when (profileRepository.status.first { it != ProfileStatus.LOADING }) {
            ProfileStatus.MISSING -> AuthEffect.NavigateToOnboarding
            else -> AuthEffect.NavigateToDashboard
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
