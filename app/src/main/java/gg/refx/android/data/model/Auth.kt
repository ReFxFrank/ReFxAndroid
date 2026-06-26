package gg.refx.android.data.model

import kotlinx.serialization.Serializable

/**
 * Auth request/response DTOs.
 *
 * NOTE: field names follow the expected auth-endpoint contract (email+password login,
 * bearer + refresh tokens, optional TOTP step). Reconcile against the panel API
 * DTOs once that repo is available — keep the keys verbatim.
 */

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    // Present on the second step when a TOTP code is required.
    val totp: String? = null,
)

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class LoginResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    // When true, the server requires a TOTP code to complete sign-in (2FA).
    val twoFactorRequired: Boolean = false,
    // Optional short-lived token threading the 2FA challenge to the verify call.
    val twoFactorToken: String? = null,
) {
    val tokens: AuthTokens?
        get() = if (accessToken != null && refreshToken != null) {
            AuthTokens(accessToken, refreshToken)
        } else {
            null
        }
}

@Serializable
data class TwoFactorVerifyRequest(
    val totp: String,
    val twoFactorToken: String? = null,
)
