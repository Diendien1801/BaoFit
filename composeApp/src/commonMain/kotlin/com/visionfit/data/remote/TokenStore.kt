package com.visionfit.data.remote

import kotlinx.coroutines.flow.MutableStateFlow

internal data class AuthTokens(
    /** Short-lived JWT sent as `Authorization: Bearer …` to protected endpoints. */
    val accessToken: String,
    /** Long-lived opaque token traded for a new pair at `/auth/refresh`, revoked at `/auth/logout`. */
    val refreshToken: String,
)

/**
 * The signed-in user's tokens. Kept in memory only, so closing the app signs the user out,
 * exactly like the mock did. Persisting them (Keychain / EncryptedSharedPreferences) comes later.
 */
internal class TokenStore {
    private val tokens = MutableStateFlow<AuthTokens?>(null)

    fun current(): AuthTokens? = tokens.value

    fun save(value: AuthTokens) {
        tokens.value = value
    }

    fun clear() {
        tokens.value = null
    }
}
