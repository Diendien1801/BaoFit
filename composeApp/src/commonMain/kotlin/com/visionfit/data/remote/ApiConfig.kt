package com.visionfit.data.remote

/**
 * Root URL of the backend, ending with `/`. Until the API gateway exists this is identity-service
 * itself (port 8081); afterwards every platform should point at the gateway instead.
 *
 * Each platform reaches the developer's machine differently, hence one value per platform.
 * A physical phone cannot use `localhost`: point it at the computer's LAN IP.
 */
internal expect val defaultApiBaseUrl: String
