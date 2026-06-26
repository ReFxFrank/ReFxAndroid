package gg.refx.android.core.network

import kotlinx.serialization.Serializable

/**
 * Success/error envelope wrapping every REST response:
 * `{ "success": true, "data": <T> }` or
 * `{ "success": false, "error": { "message", "code", ... } }` (§3.2).
 *
 * Call sites never see this — [EnvelopeConverterFactory] auto-unwraps `data`.
 */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = true,
    val data: T? = null,
    val error: ApiErrorBody? = null,
)

@Serializable
data class ApiErrorBody(
    val message: String? = null,
    val code: String? = null,
)
