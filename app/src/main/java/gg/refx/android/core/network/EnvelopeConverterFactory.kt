package gg.refx.android.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.serializer
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Retrofit converter that mirrors the iOS `APIClient.send` decode path (parity
 * spec §5):
 *
 *  - If the body is an object with a boolean `success` field → it's the detail
 *    envelope `{ success, data }`. On `success=true` return `data` decoded as `T`;
 *    otherwise throw an [ApiException] built from `error`.
 *  - Otherwise the body isn't enveloped — e.g. a paginated `{ data:[…], meta:{…} }`
 *    or a bare object — so decode the whole element directly as `T` (the iOS
 *    "fallback to raw T" behaviour, which also covers `Page<E>`).
 *
 * Request-body and string conversion delegate to [delegate] (the
 * kotlinx-serialization factory).
 */
class EnvelopeConverterFactory(
    private val json: Json,
    private val delegate: Converter.Factory,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val elementSerializer = json.serializersModule.serializer(type)

        return Converter<ResponseBody, Any?> { body ->
            val text = body.use { it.string() }
            if (text.isBlank()) return@Converter null

            val root = json.parseToJsonElement(text)
            val obj = root as? JsonObject
            val successField = (obj?.get("success") as? JsonPrimitive)?.booleanOrNull

            if (obj != null && successField != null) {
                if (successField) {
                    val data = obj["data"] ?: return@Converter null
                    json.decodeFromJsonElement(elementSerializer, data)
                } else {
                    val error = obj["error"]?.let {
                        runCatching { json.decodeFromJsonElement(ApiErrorBody.serializer(), it) }.getOrNull()
                    }
                    throw ApiException(
                        message = error?.message ?: ApiException.GENERIC_MESSAGE,
                        code = error?.code,
                    )
                }
            } else {
                // Not enveloped (paginated {data,meta} or bare) → decode whole element.
                json.decodeFromJsonElement(elementSerializer, root)
            }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, okhttp3.RequestBody>? =
        delegate.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)

    override fun stringConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, String>? =
        delegate.stringConverter(type, annotations, retrofit)
}
