package gg.refx.android.data.repo

import gg.refx.android.core.network.TokenPair
import gg.refx.android.core.network.TokenProvider
import gg.refx.android.core.network.apiCall
import gg.refx.android.core.session.SessionManager
import gg.refx.android.data.api.AccountApi
import gg.refx.android.data.api.AuthApi
import gg.refx.android.data.model.Account
import gg.refx.android.data.model.LoginRequest
import gg.refx.android.data.model.LoginResponse
import gg.refx.android.data.model.TwoFactorVerifyRequest

/** Outcome of a login attempt. */
sealed interface LoginResult {
    data class Success(val account: Account) : LoginResult
    /** The server requires a TOTP code; carry the challenge token to the verify step. */
    data class TwoFactorRequired(val twoFactorToken: String?) : LoginResult
}

/**
 * Owns sign-in / sign-out and account loading. APIs are supplied via providers so
 * the repository keeps working after a Settings origin change rebuilds Retrofit.
 */
class AuthRepository(
    private val authApiProvider: () -> AuthApi,
    private val accountApiProvider: () -> AccountApi,
    private val tokens: TokenProvider,
    private val session: SessionManager,
) {

    suspend fun login(email: String, password: String): LoginResult = apiCall {
        val response = authApiProvider().login(LoginRequest(email = email.trim(), password = password))
        completeLogin(response)
    }

    suspend fun verifyTotp(code: String, twoFactorToken: String?): LoginResult = apiCall {
        val response = authApiProvider().verifyTotp(
            TwoFactorVerifyRequest(totp = code.trim(), twoFactorToken = twoFactorToken),
        )
        completeLogin(response)
    }

    private suspend fun completeLogin(response: LoginResponse): LoginResult {
        val pair = response.tokens
        if (pair == null) {
            // No tokens yet → a 2FA step is required.
            return LoginResult.TwoFactorRequired(response.twoFactorToken)
        }
        tokens.save(TokenPair(pair.accessToken, pair.refreshToken))
        val account = accountApiProvider().getAccount()
        session.onSignedIn(account)
        return LoginResult.Success(account)
    }

    /** Load (or refresh) the current account; updates the session. */
    suspend fun loadAccount(): Account = apiCall {
        val account = accountApiProvider().getAccount()
        session.updateAccount(account)
        account
    }

    /** Explicit sign-out: best-effort server logout, then clear local state. */
    suspend fun logout() {
        runCatching { authApiProvider().logout() }
        session.signOut()
    }
}
