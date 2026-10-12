package com.visionfit.data.repository

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
        val token = tokenStore.current()?.accessToken ?: return false
        val saved = try {
            client.put(PATH) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(profile.toDto())
            }.status == HttpStatusCode.OK
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
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
            val response = client.get(PATH) { bearerAuth(token) }
            when (response.status) {
                HttpStatusCode.OK -> {
                    _profile.value = response.body<ProfileDto>().toDomain()
                    ProfileStatus.READY
                }
                HttpStatusCode.NotFound -> ProfileStatus.MISSING
                else -> ProfileStatus.FAILED
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline, or a body the app cannot read.
            ProfileStatus.FAILED
        }
    }

    private companion object {
        const val PATH = "api/v1/profile/me"
    }
}
