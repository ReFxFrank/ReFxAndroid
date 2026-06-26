package gg.refx.android.core.network

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Retrofit converter that transparently unwraps the success envelope so call
 * sites receive `<T>` directly (§3.2). A declared return type `T` is decoded as
 * `ApiEnvelope<T>`; on `success=true` we return `data`, otherwise we throw an
 * [ApiException] built from the error body.
 *
 * Request-body and string conversion are delegated to [delegate] (the
 * kotlinx-serialization factory) by returning null here.
 */
class EnvelopeConverterFactory(
    private val delegate: Converter.Factory,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val envelopeType: Type = EnvelopeParameterizedType(type)
        val envelopeConverter = delegate.responseBodyConverter(envelopeType, annotations, retrofit)
            ?: error("No delegate converter for $envelopeType")

        return Converter<ResponseBody, Any?> { body ->
            @Suppress("UNCHECKED_CAST")
            val envelope = envelopeConverter.convert(body) as? ApiEnvelope<Any?>
                ?: return@Converter null
            if (envelope.success) {
                envelope.data
            } else {
                throw ApiException(
                    message = envelope.error?.message ?: ApiException.GENERIC_MESSAGE,
                    code = envelope.error?.code,
                )
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

    /** `ApiEnvelope<elementType>` as a runtime [ParameterizedType]. */
    private class EnvelopeParameterizedType(private val element: Type) : ParameterizedType {
        override fun getActualTypeArguments(): Array<Type> = arrayOf(element)
        override fun getRawType(): Type = ApiEnvelope::class.java
        override fun getOwnerType(): Type? = null
    }
}
