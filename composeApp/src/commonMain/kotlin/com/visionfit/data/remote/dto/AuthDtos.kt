package com.visionfit.data.remote.dto

import kotlinx.serialization.Serializable

// Request and response bodies of identity-service's `/api/v1/auth` endpoints.

@Serializable
internal data class CredentialsRequest(
    val email: String,
    val password: String,
)

@Serializable
internal data class RefreshTokenRequest(
    val refreshToken: String,
)

/** Only the fields the app reads; the rest of the server's response is ignored. */
@Serializable
internal data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val email: String,
)
