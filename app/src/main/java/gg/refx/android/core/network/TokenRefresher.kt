package gg.refx.android.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Synchronously exchanges a refresh token for a fresh [TokenPair]. Used by
 * [TokenAuthenticator] on a 401. Uses its own bare OkHttp client (no auth
 * interceptor / authenticator) so it can never recurse into itself.
 *
 * NOTE: the request/response keys (`refreshToken` → `accessToken`/`refreshToken`)
 * are the expected `auth/refresh` shape; reconcile against the panel API once the
 * backend repo is available.
 */
class TokenRefresher(
    private val originProvider: () -> ApiConfig,
) {
    private val client = OkHttpClient.Builder().build()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    @Serializable
    private data class RefreshRequest(val refreshToken: String)

    @Serializable
    private data class RefreshResponse(
        @SerialName("accessToken") val accessToken: String,
        @SerialName("refreshToken") val refreshToken: String? = null,
    )

    /** Returns the new tokens, or null if refresh failed (caller signs out). */
    fun refresh(refreshToken: String): TokenPair? {
        val config = originProvider()
        val body = RefxJson.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            .toRequestBody(jsonMedia)
        val request = Request.Builder()
            .url(config.restBaseUrl + "auth/refresh")
            .post(body)
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val raw = response.body?.string() ?: return null
                val envelope = RefxJson.decodeFromString(
                    ApiEnvelope.serializer(RefreshResponse.serializer()),
                    raw,
                )
                val data = envelope.data?.takeIf { envelope.success } ?: return null
                TokenPair(
                    accessToken = data.accessToken,
                    // Some servers rotate the refresh token; reuse the old one if absent.
                    refreshToken = data.refreshToken ?: refreshToken,
                )
            }
        }.getOrNull()
    }
}
