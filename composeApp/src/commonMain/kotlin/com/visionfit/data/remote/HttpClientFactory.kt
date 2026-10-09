package com.visionfit.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The one HTTP client every remote repository shares. Requests use paths relative to [baseUrl]
 * (`"api/v1/auth/login"`, no leading slash) and get JSON bodies in both directions.
 *
 * Non-2xx responses do not throw: each repository reads the status code and maps it to its own
 * domain error. Only connection problems and timeouts throw.
 *
 * @param engine `null` picks the platform engine (OkHttp or Darwin); tests pass a `MockEngine`.
 */
internal fun createHttpClient(baseUrl: String, engine: HttpClientEngine? = null): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        expectSuccess = false
        install(ContentNegotiation) {
            // The server may add fields the app does not use yet (tokenType, expiresInMs…).
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
        }
        defaultRequest { url(baseUrl) }
    }
    return if (engine == null) HttpClient(config) else HttpClient(engine, config)
}
