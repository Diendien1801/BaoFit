package com.visionfit.data.remote

/**
 * Root URL of each backend service, ending with `/`. Once the API gateway exists both point at it
 * and only the gateway knows which service answers which path.
 */
data class ApiUrls(
    val identity: String,
    val profile: String,
)

internal val defaultApiUrls = ApiUrls(
    identity = "$devMachineUrl:8081/",
    profile = "$devMachineUrl:8082/",
)

/**
 * How the app reaches the developer's machine, which differs per platform.
 * A physical phone cannot use `localhost`: point it at the computer's LAN IP.
 */
internal expect val devMachineUrl: String
