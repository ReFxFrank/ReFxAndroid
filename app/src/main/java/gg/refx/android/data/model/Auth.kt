package gg.refx.android.data.model

import kotlinx.serialization.Serializable

/**
 * Auth request/response DTOs (parity spec §5, `AuthAPI.swift`).
 *
 * Login may return tokens directly OR an MFA challenge `{ mfaToken, methods }`;
 * the client then calls `POST auth/mfa/verify { mfaToken, code, method }`.
 */

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val totp: String? = null,
    val rememberMe: Boolean? = null,
)

/** `{ accessToken, refreshToken }` — the rotated token pair (refresh rotates both). */
@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

/**
 * Union response from `POST auth/login`: either the token pair (success) or an MFA
 * challenge (`mfaToken` + available `methods`).
 */
@Serializable
data class LoginResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val mfaToken: String? = null,
    val methods: List<MFAMethod> = emptyList(),
) {
    val tokens: TokenResponse?
        get() = if (accessToken != null && refreshToken != null) {
            TokenResponse(accessToken, refreshToken)
        } else {
            null
        }

    val mfaRequired: Boolean get() = tokens == null && mfaToken != null
}

@Serializable
data class MfaVerifyRequest(
    val mfaToken: String,
    val code: String,
    val method: String, // "totp" | "recovery"
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)
