package gg.refx.android.core.network

import retrofit2.HttpException
import java.io.IOException

/**
 * Wraps a Retrofit suspend call so every failure surfaces as a user-facing
 * [ApiException]. Non-2xx responses (which bypass the envelope converter) have
 * their error body parsed for the `{ success:false, error:{...} }` message.
 */
suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: ApiException) {
        throw e
    } catch (e: HttpException) {
        throw e.toApiExceptionFromBody()
    } catch (e: IOException) {
        throw ApiException.network(e)
    } catch (e: Throwable) {
        throw ApiException(e.message ?: ApiException.GENERIC_MESSAGE, cause = e)
    }

private fun HttpException.toApiExceptionFromBody(): ApiException {
    val raw = response()?.errorBody()?.string()
    val parsed = raw?.let {
        runCatching {
            RefxJson.decodeFromString(
                ApiEnvelope.serializer(kotlinx.serialization.json.JsonElement.serializer()),
                it,
            ).error
        }.getOrNull()
    }
    return ApiException(
        message = parsed?.message ?: defaultMessageForStatus(code()),
        code = parsed?.code,
        status = code(),
        cause = this,
    )
}

private fun defaultMessageForStatus(status: Int): String = when (status) {
    401 -> "Your session has expired. Please sign in again."
    403 -> "You don't have permission to do that."
    404 -> "Not found."
    in 500..599 -> "ReFx is having trouble right now. Please try again shortly."
    else -> ApiException.GENERIC_MESSAGE
}
