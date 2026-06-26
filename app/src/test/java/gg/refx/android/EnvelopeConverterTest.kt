package gg.refx.android

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import gg.refx.android.core.network.EnvelopeConverterFactory
import gg.refx.android.core.network.RefxJson
import gg.refx.android.data.model.ServerState
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit

@Serializable
private data class ConvDummy(val state: ServerState, val name: String)

/**
 * Contract guard for the actual Retrofit response-body decode path (not just the
 * serializer): a success envelope whose `data` is explicitly null — or absent —
 * must decode to null rather than throwing, so void/no-content success responses
 * succeed (parity with the iOS `send` path, §5).
 */
class EnvelopeConverterTest {

    private val contentType = "application/json".toMediaType()
    private val factory = EnvelopeConverterFactory(RefxJson, RefxJson.asConverterFactory(contentType))
    private val retrofit = Retrofit.Builder().baseUrl("https://example.com/").build()

    private fun convert(body: String): Any? {
        val converter = factory.responseBodyConverter(ConvDummy::class.java, emptyArray(), retrofit)!!
        return converter.convert(body.toResponseBody(contentType))
    }

    @Test fun success_with_explicit_null_data_returns_null_not_throws() {
        assertNull(convert("""{"success":true,"data":null}"""))
    }

    @Test fun success_with_absent_data_returns_null() {
        assertNull(convert("""{"success":true}"""))
    }

    @Test fun success_with_data_decodes() {
        val result = convert("""{"success":true,"data":{"state":"RUNNING","name":"alpha"}}""")
        assertEquals(ConvDummy(ServerState.RUNNING, "alpha"), result)
    }
}
