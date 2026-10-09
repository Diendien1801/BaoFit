package com.visionfit.data.repository

/**
 * The name the app greets the user with until they can set one: "minh.tran@x.vn" → "Minh".
 * Registration only asks for an e-mail, so neither the mock nor the server knows a real name.
 */
internal fun displayNameFromEmail(email: String): String =
    email.substringBefore('@')
        .split('.', '_', '-', '+')
        .firstOrNull { it.isNotBlank() }
        ?.replaceFirstChar { it.uppercase() }
        ?: "Bạn"
