package gg.refx.android.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Bearer <access>` to every request when signed in. Requests
 * tagged [NoAuth] (login/refresh, public catalog) are sent without the header.
 */
class AuthInterceptor(
    private val tokens: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.tag(NoAuth::class.java) != null) {
            return chain.proceed(request)
        }
        val access = tokens.accessToken()
        val authed = if (access != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $access")
                .build()
        } else {
            request
        }
        return chain.proceed(authed)
    }
}

/** Tag marking a request that must NOT carry the bearer token. */
class NoAuth
