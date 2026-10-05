package com.visionfit.data.repository

import com.visionfit.data.mock.InMemoryVisionFitStore
import com.visionfit.data.mock.MockAccount
import com.visionfit.domain.model.AuthError
import com.visionfit.domain.model.AuthResult
import com.visionfit.domain.model.UserSession
import com.visionfit.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Accounts live in memory. Demo login: `an@visionfit.vn` / `visionfit123`.
 * `locked@visionfit.vn` exercises the locked-account error.
 */
internal class MockAuthRepository(
    private val store: InMemoryVisionFitStore,
    private val latencyMillis: Long = 900,
) : AuthRepository {

    private val _session = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = _session.asStateFlow()

    override suspend fun login(email: String, password: String): AuthResult {
        delay(latencyMillis)
        val account = store.accounts.value.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
        return when {
            account == null || account.password != password -> AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
            account.isLocked -> AuthResult.Failure(AuthError.ACCOUNT_LOCKED)
            else -> signIn(account)
        }
    }

    override suspend fun register(email: String, password: String): AuthResult {
        delay(latencyMillis)
        val normalized = email.trim().lowercase()
        if (store.accounts.value.any { it.email == normalized }) {
            return AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED)
        }
        val account = MockAccount(
            userId = "user-${store.accounts.value.size + 1}",
            email = normalized,
            password = password,
            displayName = displayNameFrom(normalized),
        )
        store.accounts.update { it + account }
        return signIn(account)
    }

    override suspend fun requestPasswordReset(email: String): Boolean {
        delay(latencyMillis / 2)
        return true
    }

    override suspend fun logout() {
        _session.value = null
    }

    private fun signIn(account: MockAccount): AuthResult.Success {
        val session = UserSession(account.userId, account.email, account.displayName)
        _session.value = session
        return AuthResult.Success(session)
    }

    /** "minh.tran@x.vn" → "Minh". */
    private fun displayNameFrom(email: String): String =
        email.substringBefore('@')
            .split('.', '_', '-', '+')
            .firstOrNull { it.isNotBlank() }
            ?.replaceFirstChar { it.uppercase() }
            ?: "Bạn"
}
