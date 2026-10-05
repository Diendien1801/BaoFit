package com.visionfit.domain.usecase

enum class CredentialError {
    EMAIL_BLANK,
    EMAIL_INVALID,
    PASSWORD_BLANK,
    PASSWORD_TOO_SHORT,
    CONFIRMATION_MISMATCH,
}

data class CredentialValidation(
    val email: CredentialError? = null,
    val password: CredentialError? = null,
    val confirmation: CredentialError? = null,
) {
    val isValid: Boolean get() = email == null && password == null && confirmation == null
}

class ValidateCredentialsUseCase {

    /** Pass [confirmation] only when registering. */
    operator fun invoke(email: String, password: String, confirmation: String? = null): CredentialValidation {
        val trimmedEmail = email.trim()
        return CredentialValidation(
            email = when {
                trimmedEmail.isEmpty() -> CredentialError.EMAIL_BLANK
                !EMAIL_REGEX.matches(trimmedEmail) -> CredentialError.EMAIL_INVALID
                else -> null
            },
            password = when {
                password.isEmpty() -> CredentialError.PASSWORD_BLANK
                password.length < MIN_PASSWORD_LENGTH -> CredentialError.PASSWORD_TOO_SHORT
                else -> null
            },
            confirmation = when {
                confirmation == null -> null
                confirmation != password -> CredentialError.CONFIRMATION_MISMATCH
                else -> null
            },
        )
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
