package com.visionfit.data.repository

import com.visionfit.core.log.AppLog
import com.visionfit.data.remote.TokenStore
import com.visionfit.data.remote.dto.ProfileDto
import com.visionfit.data.remote.dto.toDomain
import com.visionfit.data.remote.dto.toDto
import com.visionfit.domain.model.BodyProfile
import com.visionfit.domain.model.ProfileStatus
import com.visionfit.domain.model.UserSession
import com.visionfit.domain.repository.ProfileRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * [ProfileRepository] backed by profile-service (`/api/v1/profile/me`).
 *
 * It follows [session]: every sign-in or sign-out resets it to [ProfileStatus.LOADING] and
 * [BodyProfile.Default], and a sign-in then downloads the user's profile.
 */
internal class RemoteProfileRepository(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val session: StateFlow<UserSession?>,
    scope: CoroutineScope,
) : ProfileRepository {

    private val _profile = MutableStateFlow(BodyProfile.Default)
    override val profile: StateFlow<BodyProfile> = _profile.asStateFlow()

    private val _status = MutableStateFlow(ProfileStatus.LOADING)
    override val status: StateFlow<ProfileStatus> = _status.asStateFlow()

    init {
        // collectLatest: signing out (or in as someone else) cancels a download still in flight.
        scope.launch {
            session.collectLatest { user ->
                _profile.value = BodyProfile.Default
                _status.value = ProfileStatus.LOADING
                if (user != null) download()
            }
        }
    }

    override suspend fun saveProfile(profile: BodyProfile): Boolean {
        val token = tokenStore.current()?.accessToken ?: run {
            AppLog.w(LOG_AREA, "PUT $PATH skipped: no access token (not signed in)")
            return false
        }
        val saved = try {
            AppLog.d(LOG_AREA, "PUT $PATH")
            val response = client.put(PATH) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(profile.toDto())
            }
            AppLog.d(LOG_AREA, "PUT ${response.request.url} -> ${response.status}")
            if (response.status != HttpStatusCode.OK) {
                AppLog.w(LOG_AREA, "PUT $PATH error body: ${response.bodyAsText().take(MAX_LOGGED_BODY)}")
            }
            response.status == HttpStatusCode.OK
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(LOG_AREA, "PUT $PATH failed: $e", e)
            false
        }
        if (saved) {
            _profile.value = profile
            _status.value = ProfileStatus.READY
        }
        return saved
    }

    override suspend fun refresh() {
        if (session.value == null) return
        _status.value = ProfileStatus.LOADING
        download()
    }

    private suspend fun download() {
        val token = tokenStore.current()?.accessToken
        _status.value = try {
            if (token == null) error("signed in without an access token")
            AppLog.d(LOG_AREA, "GET $PATH")
            val response = client.get(PATH) { bearerAuth(token) }
            AppLog.d(LOG_AREA, "GET ${response.request.url} -> ${response.status}")
            when (response.status) {
                HttpStatusCode.OK -> {
                    _profile.value = response.body<ProfileDto>().toDomain()
                    ProfileStatus.READY
                }
                HttpStatusCode.NotFound -> ProfileStatus.MISSING
                else -> {
                    AppLog.w(LOG_AREA, "GET $PATH error body: ${response.bodyAsText().take(MAX_LOGGED_BODY)}")
                    ProfileStatus.FAILED
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline, or a body the app cannot read.
            AppLog.e(LOG_AREA, "GET $PATH failed: $e", e)
            ProfileStatus.FAILED
        }
    }

    private companion object {
        const val PATH = "api/v1/profile/me"
        const val LOG_AREA = "Profile"

        /** Error bodies are short JSON; this only guards against an HTML error page. */
        const val MAX_LOGGED_BODY = 500
    }
}
