package com.visionfit.domain.model

data class UserSession(
    val userId: String,
    val email: String,
    val displayName: String,
) {
    /** "An" → "AN", "Trần Minh Thư" → "TT". */
    val initials: String
        get() {
            val words = displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            return when {
                words.isEmpty() -> "?"
                words.size == 1 -> words[0].take(2).uppercase()
                else -> "${words.first().first()}${words.last().first()}".uppercase()
            }
        }
}

enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_REGISTERED,
    ACCOUNT_LOCKED,
    NETWORK,
}

sealed interface AuthResult {
    data class Success(val session: UserSession) : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}
