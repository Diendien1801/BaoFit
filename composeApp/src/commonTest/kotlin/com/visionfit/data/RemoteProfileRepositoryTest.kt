package com.visionfit.data

import com.visionfit.data.remote.AuthTokens
import com.visionfit.data.remote.TokenStore
import com.visionfit.data.remote.createHttpClient
import com.visionfit.data.repository.RemoteProfileRepository
import com.visionfit.domain.model.ActivityLevel
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.FitnessGoal
import com.visionfit.domain.model.ProfileStatus
import com.visionfit.domain.model.Sex
import com.visionfit.domain.model.UserSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoteProfileRepositoryTest {

    private val tokenStore = TokenStore()
    private val session = MutableStateFlow<UserSession?>(null)
    private val requests = mutableListOf<HttpRequestData>()

    /**
     * The engine runs on the test scheduler, so `runCurrent()` also completes HTTP calls. Not
     * `advanceUntilIdle()`: it ignores `backgroundScope`, where the repository follows the session.
     */
    private fun TestScope.repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): RemoteProfileRepository {
        val engine = MockEngine(
            MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler { request ->
                    requests += request
                    handler(request)
                }
            },
        )
        return RemoteProfileRepository(createHttpClient("http://profile.test/", engine), tokenStore, session, backgroundScope)
    }

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private val HttpRequestData.bodyText: String
        get() = (body as OutgoingContent.ByteArrayContent).bytes().decodeToString()

    private fun signIn() {
        tokenStore.save(AuthTokens(accessToken = "access-1", refreshToken = "refresh-1"))
        session.value = UserSession("user-1", "an@visionfit.vn", "An")
    }

    @Test
    fun signingInDownloadsTheProfileWithTheAccessToken() = runTest {
        val profiles = repository { json(HttpStatusCode.OK, SAVED_JSON) }
        runCurrent()
        assertTrue(requests.isEmpty(), "nobody is signed in yet")
        assertEquals(ProfileStatus.LOADING, profiles.status.value)

        signIn()
        runCurrent()

        assertEquals(SAVED, profiles.profile.value)
        assertEquals(ProfileStatus.READY, profiles.status.value)
        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("http://profile.test/api/v1/profile/me", request.url.toString())
        assertEquals("Bearer access-1", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun noProfileIsMissingAndAnyOtherAnswerIsAFailure() = runTest {
        // null: the server cannot be reached. 200 here carries a body the app cannot read.
        val cases = listOf(
            HttpStatusCode.NotFound to ProfileStatus.MISSING,
            HttpStatusCode.Unauthorized to ProfileStatus.FAILED,
            HttpStatusCode.OK to ProfileStatus.FAILED,
            null to ProfileStatus.FAILED,
        )
        for ((status, expected) in cases) {
            session.value = null
            val profiles = repository { if (status == null) error("connection refused") else json(status, "{}") }

            signIn()
            runCurrent()

            assertEquals(expected, profiles.status.value)
            assertEquals(BodyProfile.Default, profiles.profile.value)
        }
    }

    @Test
    fun refreshRetriesAFailedDownload() = runTest {
        var online = false
        val profiles = repository { if (online) json(HttpStatusCode.OK, SAVED_JSON) else error("offline") }
        signIn()
        runCurrent()
        assertEquals(ProfileStatus.FAILED, profiles.status.value)

        online = true
        profiles.refresh()

        assertEquals(ProfileStatus.READY, profiles.status.value)
        assertEquals(SAVED, profiles.profile.value)
    }

    @Test
    fun signingOutForgetsTheProfile() = runTest {
        val profiles = repository { json(HttpStatusCode.OK, SAVED_JSON) }
        signIn()
        runCurrent()

        tokenStore.clear()
        session.value = null
        runCurrent()

        assertEquals(BodyProfile.Default, profiles.profile.value)
        assertEquals(ProfileStatus.LOADING, profiles.status.value)
    }

    @Test
    fun saveSendsTheProfileInTheServersFormat() = runTest {
        val profiles = repository { request ->
            if (request.method == HttpMethod.Put) json(HttpStatusCode.OK, SAVED_JSON) else respond("", HttpStatusCode.NotFound)
        }
        signIn()
        runCurrent()

        assertEquals(ProfileStatus.MISSING, profiles.status.value)
        assertTrue(profiles.saveProfile(SAVED))

        assertEquals(SAVED, profiles.profile.value)
        assertEquals(ProfileStatus.READY, profiles.status.value)
        val put = requests.last()
        assertEquals(HttpMethod.Put, put.method)
        assertEquals("Bearer access-1", put.headers[HttpHeaders.Authorization])
        assertEquals(SAVED_JSON, put.bodyText)
    }

    @Test
    fun failedSaveKeepsTheOldProfile() = runTest {
        var reachable = true
        val profiles = repository { request ->
            when {
                !reachable -> error("offline")
                request.method == HttpMethod.Put -> json(HttpStatusCode.BadRequest, """{"status":400}""")
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        signIn()
        runCurrent()

        assertFalse(profiles.saveProfile(SAVED))
        reachable = false
        assertFalse(profiles.saveProfile(SAVED))

        assertEquals(BodyProfile.Default, profiles.profile.value)
    }

    @Test
    fun saveWithoutSigningInSendsNothing() = runTest {
        val profiles = repository { json(HttpStatusCode.OK, SAVED_JSON) }

        assertFalse(profiles.saveProfile(SAVED))

        assertTrue(requests.isEmpty())
    }

    private companion object {
        val SAVED = BodyProfile(
            sex = Sex.FEMALE,
            ageYears = 30,
            heightCm = 165,
            weightKg = 80,
            activityLevel = ActivityLevel.LIGHT,
            goal = FitnessGoal.FAT_LOSS,
        )

        /** Exactly what profile-service sends and expects. */
        const val SAVED_JSON =
            """{"sex":"FEMALE","ageYears":30,"heightCm":165,"weightKg":80,"activityLevel":"LIGHT","goal":"FAT_LOSS"}"""
    }
}
