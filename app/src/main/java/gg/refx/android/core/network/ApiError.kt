package gg.refx.android.core.network

import java.io.IOException

/**
 * The single error type surfaced to call sites. Carries a [message] safe to show
 * the user, an optional backend [code], and the HTTP [status] when known.
 *
 * Extends [IOException] so Retrofit suspend functions surface it through the
 * normal call path rather than wrapping it.
 */
class ApiException(
    override val message: String,
    val code: String? = null,
    val status: Int? = null,
    cause: Throwable? = null,
) : IOException(message, cause) {

    /** True for auth failures the UI may treat as "session expired". */
    val isUnauthorized: Boolean get() = status == 401

    companion object {
        const val GENERIC_MESSAGE = "Something went wrong. Please try again."

        fun network(cause: Throwable): ApiException =
            ApiException(
                message = "Can't reach ReFx. Check your connection and try again.",
                code = "network_error",
                cause = cause,
            )
    }
}

/** Map any thrown error to a user-facing [ApiException]. */
fun Throwable.toApiException(): ApiException = when (this) {
    is ApiException -> this
    is IOException -> ApiException.network(this)
    else -> ApiException(message ?: ApiException.GENERIC_MESSAGE, cause = this)
}
