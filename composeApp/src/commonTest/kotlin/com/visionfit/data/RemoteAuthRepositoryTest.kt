package com.visionfit.data

import com.visionfit.data.remote.TokenStore
import com.visionfit.data.remote.createHttpClient
import com.visionfit.data.repository.RemoteAuthRepository
import com.visionfit.domain.model.AuthError
import com.visionfit.domain.model.AuthResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class RemoteAuthRepositoryTest {

    private val tokenStore = TokenStore()
    private val requests = mutableListOf<HttpRequestData>()

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): RemoteAuthRepository {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return RemoteAuthRepository(createHttpClient("http://backend.test/", engine), tokenStore)
    }

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private val HttpRequestData.bodyText: String
        get() = (body as OutgoingContent.ByteArrayContent).bytes().decodeToString()

    @Test
    fun loginStoresTokensAndPublishesTheSession() = runTest {
        val auth = repository { json(HttpStatusCode.OK, authResponse(email = "minh.tran@visionfit.vn")) }

        val result = auth.login("  minh.tran@visionfit.vn ", "visionfit123")

        val session = assertIs<AuthResult.Success>(result).session
        assertEquals(USER_ID, session.userId)
        assertEquals("minh.tran@visionfit.vn", session.email)
        assertEquals("Minh", session.displayName)
        assertEquals(session, auth.session.value)
        assertEquals(ACCESS_TOKEN, tokenStore.current()?.accessToken)
        assertEquals("refresh-1", tokenStore.current()?.refreshToken)

        val request = requests.single()
        assertEquals("http://backend.test/api/v1/auth/login", request.url.toString())
        assertEquals("""{"email":"minh.tran@visionfit.vn","password":"visionfit123"}""", request.bodyText)
    }

    @Test
    fun registerAcceptsCreated() = runTest {
        val auth = repository { json(HttpStatusCode.Created, authResponse()) }

        assertIs<AuthResult.Success>(auth.register("an@visionfit.vn", "visionfit123"))
        assertEquals("/api/v1/auth/register", requests.single().url.encodedPath)
    }

    @Test
    fun serverErrorsMapToAuthErrors() = runTest {
        val cases = listOf(
            HttpStatusCode.Unauthorized to AuthError.INVALID_CREDENTIALS,
            HttpStatusCode.BadRequest to AuthError.INVALID_CREDENTIALS,
            HttpStatusCode.Conflict to AuthError.EMAIL_ALREADY_REGISTERED,
            HttpStatusCode.Locked to AuthError.ACCOUNT_LOCKED,
            HttpStatusCode.InternalServerError to AuthError.NETWORK,
        )
        for ((status, expected) in cases) {
            val auth = repository { json(status, """{"status":${status.value},"message":"nope"}""") }

            val result = auth.login("an@visionfit.vn", "visionfit123")

            assertEquals(expected, assertIs<AuthResult.Failure>(result).error, "HTTP ${status.value}")
            assertNull(auth.session.value)
            assertNull(tokenStore.current())
        }
    }

    @Test
    fun unreachableServerIsANetworkError() = runTest {
        val auth = repository { error("connection refused") }

        val result = auth.login("an@visionfit.vn", "visionfit123")

        assertEquals(AuthError.NETWORK, assertIs<AuthResult.Failure>(result).error)
    }

    @Test
    fun logoutRevokesTheRefreshTokenAndClearsTheSession() = runTest {
        val auth = repository { request ->
            if (request.url.encodedPath.endsWith("/logout")) {
                respond("", HttpStatusCode.NoContent)
            } else {
                json(HttpStatusCode.OK, authResponse())
            }
        }
        auth.login("an@visionfit.vn", "visionfit123")

        auth.logout()

        assertNull(auth.session.value)
        assertNull(tokenStore.current())
        assertEquals("/api/v1/auth/logout", requests.last().url.encodedPath)
        assertEquals("""{"refreshToken":"refresh-1"}""", requests.last().bodyText)
    }

    @Test
    fun logoutSignsOutLocallyEvenWhenTheServerIsDown() = runTest {
        var online = true
        val auth = repository { if (online) json(HttpStatusCode.OK, authResponse()) else error("offline") }
        auth.login("an@visionfit.vn", "visionfit123")
        online = false

        auth.logout()

        assertNull(auth.session.value)
        assertNull(tokenStore.current())
    }

    private fun authResponse(email: String = "an@visionfit.vn") = """
        {
          "accessToken": "$ACCESS_TOKEN",
          "refreshToken": "refresh-1",
          "tokenType": "Bearer",
          "expiresInMs": 3600000,
          "refreshExpiresInMs": 604800000,
          "email": "$email"
        }
    """.trimIndent()

    private companion object {
        const val USER_ID = "0b0f6c1e-3f7a-4c39-9d7e-2f1c5a8b9e10"

        /** A JWT shaped like identity-service's: the payload carries `sub` and `uid`. Never verified by the app. */
        val ACCESS_TOKEN: String = listOf(
            """{"alg":"HS256"}""",
            """{"sub":"an@visionfit.vn","uid":"$USER_ID","iat":1760000000,"exp":1760003600}""",
        ).joinToString(".") { Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(it.encodeToByteArray()) } +
            ".signature"
    }
}
