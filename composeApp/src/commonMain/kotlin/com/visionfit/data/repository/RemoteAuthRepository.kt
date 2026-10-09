package com.visionfit.data.repository

import com.visionfit.data.remote.AuthTokens
import com.visionfit.data.remote.TokenStore
import com.visionfit.data.remote.dto.AuthResponse
import com.visionfit.data.remote.dto.CredentialsRequest
import com.visionfit.data.remote.dto.RefreshTokenRequest
import com.visionfit.domain.model.AuthError
import com.visionfit.domain.model.AuthResult
import com.visionfit.domain.model.UserSession
import com.visionfit.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64

/**
 * [AuthRepository] backed by identity-service (`/api/v1/auth`).
 *
 * The server answers with the tokens and the e-mail only, so the user id is read from the access
 * token's `uid` claim and the display name is derived from the e-mail, as the mock does.
 */
internal class RemoteAuthRepository(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
) : AuthRepository {

    private val _session = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = _session.asStateFlow()

    override suspend fun login(email: String, password: String): AuthResult =
        authenticate("api/v1/auth/login", email, password)

    override suspend fun register(email: String, password: String): AuthResult =
        authenticate("api/v1/auth/register", email, password)

    /** identity-service has no password-reset endpoint yet, so this always reports failure. */
    override suspend fun requestPasswordReset(email: String): Boolean = false

    override suspend fun logout() {
        val refreshToken = tokenStore.current()?.refreshToken
        tokenStore.clear()
        _session.value = null
        if (refreshToken == null) return
        // Best effort: the user is signed out on this device even if the server cannot revoke the token.
        try {
            client.post("api/v1/auth/logout") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun authenticate(path: String, email: String, password: String): AuthResult = try {
        val response = client.post(path) {
            contentType(ContentType.Application.Json)
            setBody(CredentialsRequest(email.trim(), password))
        }
        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> signIn(response.body())
            // 400 means the server's validation disagreed with the app's (e.g. a password over 72 characters).
            HttpStatusCode.Unauthorized, HttpStatusCode.BadRequest -> AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
            HttpStatusCode.Conflict -> AuthResult.Failure(AuthError.EMAIL_ALREADY_REGISTERED)
            HttpStatusCode.Locked -> AuthResult.Failure(AuthError.ACCOUNT_LOCKED)
            // AuthError has no "server error" case yet, so 5xx shows the same message as being offline.
            else -> AuthResult.Failure(AuthError.NETWORK)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // No connection, timeout, or a body that does not match AuthResponse.
        AuthResult.Failure(AuthError.NETWORK)
    }

    private fun signIn(response: AuthResponse): AuthResult {
        val userId = userIdFrom(response.accessToken) ?: return AuthResult.Failure(AuthError.NETWORK)
        tokenStore.save(AuthTokens(response.accessToken, response.refreshToken))
        val session = UserSession(userId, response.email, displayNameFromEmail(response.email))
        _session.value = session
        return AuthResult.Success(session)
    }

    /**
     * Reads the `uid` claim from the JWT payload (the middle, base64url-encoded part). The app only
     * reads it; checking the signature is the server's job.
     */
    private fun userIdFrom(accessToken: String): String? {
        val payload = accessToken.split('.').getOrNull(1) ?: return null
        val json = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(payload).decodeToString()
        return Json.parseToJsonElement(json).jsonObject["uid"]?.jsonPrimitive?.contentOrNull
    }
}
