package gg.refx.android.data.repo

import gg.refx.android.core.network.TokenPair
import gg.refx.android.core.network.TokenProvider
import gg.refx.android.core.network.apiCall
import gg.refx.android.core.session.SessionManager
import gg.refx.android.data.api.AuthApi
import gg.refx.android.data.model.Account
import gg.refx.android.data.model.LoginRequest
import gg.refx.android.data.model.LogoutRequest
import gg.refx.android.data.model.MFAMethod
import gg.refx.android.data.model.MfaVerifyRequest
import gg.refx.android.data.model.TokenResponse

/** Outcome of a login / MFA-verify attempt. */
sealed interface LoginResult {
    data class Success(val account: Account) : LoginResult
    /** The server requires an MFA code; carry the challenge token + offered methods. */
    data class MfaRequired(val mfaToken: String, val methods: List<MFAMethod>) : LoginResult
}

/**
 * Owns sign-in / MFA / sign-out and current-user loading. The API is supplied via a
 * provider so the repository keeps working after a Settings origin change rebuilds
 * Retrofit.
 */
class AuthRepository(
    private val authApiProvider: () -> AuthApi,
    private val tokens: TokenProvider,
    private val session: SessionManager,
) {

    suspend fun login(email: String, password: String): LoginResult = apiCall {
        val response = authApiProvider().login(
            LoginRequest(email = email.trim(), password = password),
        )
        val pair = response.tokens
        if (pair != null) {
            persistAndLoad(pair)
        } else {
            LoginResult.MfaRequired(
                mfaToken = response.mfaToken.orEmpty(),
                methods = response.methods,
            )
        }
    }

    suspend fun verifyMfa(code: String, mfaToken: String, method: MFAMethod = MFAMethod.TOTP): LoginResult =
        apiCall {
            val pair = authApiProvider().verifyMfa(
                MfaVerifyRequest(mfaToken = mfaToken, code = code.trim(), method = method.raw),
            )
            persistAndLoad(pair)
        }

    private suspend fun persistAndLoad(pair: TokenResponse): LoginResult {
        tokens.save(TokenPair(pair.accessToken, pair.refreshToken))
        val account = authApiProvider().me()
        session.onSignedIn(account)
        return LoginResult.Success(account)
    }

    /** Load (or refresh) the current user; updates the session. */
    suspend fun loadAccount(): Account = apiCall {
        val account = authApiProvider().me()
        session.updateAccount(account)
        account
    }

    /** Explicit sign-out: best-effort server logout, then clear local state. */
    suspend fun logout() {
        val refresh = tokens.refreshToken()
        if (refresh != null) {
            runCatching { authApiProvider().logout(LogoutRequest(refresh)) }
        }
        session.signOut()
    }
}
