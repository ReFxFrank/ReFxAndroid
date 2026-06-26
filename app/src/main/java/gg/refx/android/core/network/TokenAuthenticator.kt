package gg.refx.android.core.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * On a 401, transparently refresh the access token **once** and retry the request
 * with the new token. On refresh failure, sign out (clear tokens, notify) and give
 * up by returning null (§3.2).
 */
class TokenAuthenticator(
    private val tokens: TokenProvider,
    private val refresher: TokenRefresher,
    private val onSignedOut: () -> Unit,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Never try to refresh the refresh call itself, or unauthenticated requests.
        if (response.request.tag(NoAuth::class.java) != null) return null
        // Already retried once (two Authorization attempts in the chain) → give up.
        if (responseCount(response) >= 2) return null

        synchronized(lock) {
            val current = tokens.current() ?: return null
            val attemptedWith = response.request.header("Authorization")

            // Another thread may have already refreshed; if so, just retry with the new token.
            val latest = tokens.current()?.accessToken
            if (latest != null && "Bearer $latest" != attemptedWith) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latest")
                    .build()
            }

            val refreshed = refresher.refresh(current.refreshToken)
            if (refreshed == null) {
                tokens.clear()
                onSignedOut()
                return null
            }
            tokens.save(refreshed)
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
