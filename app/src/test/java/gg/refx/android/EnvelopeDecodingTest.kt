package gg.refx.android

import gg.refx.android.core.network.ApiEnvelope
import gg.refx.android.core.network.Page
import gg.refx.android.core.network.RefxJson
import gg.refx.android.data.model.ServerState
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@Serializable
private data class Dummy(val state: ServerState, val name: String)

/**
 * Contract guard for the success/error envelope and pagination shapes (§3.2):
 * `data` decodes to `<T>`, unknown keys are ignored, errors surface a message,
 * and `Page<E>` carries items + hasMore.
 */
class EnvelopeDecodingTest {

    @Test fun success_envelope_unwraps_to_data() {
        val json = """{"success":true,"data":{"state":"running","name":"alpha"}}"""
        val env = RefxJson.decodeFromString(ApiEnvelope.serializer(Dummy.serializer()), json)
        assertTrue(env.success)
        assertEquals("alpha", env.data?.name)
        assertEquals(ServerState.RUNNING, env.data?.state)
    }

    @Test fun unknown_keys_are_ignored() {
        val json = """{"success":true,"data":{"state":"running","name":"alpha","newField":42}}"""
        val env = RefxJson.decodeFromString(ApiEnvelope.serializer(Dummy.serializer()), json)
        assertEquals("alpha", env.data?.name)
    }

    @Test fun error_envelope_carries_message() {
        val json = """{"success":false,"error":{"message":"Nope","code":"forbidden"}}"""
        val env = RefxJson.decodeFromString(ApiEnvelope.serializer(Dummy.serializer()), json)
        assertFalse(env.success)
        assertNull(env.data)
        assertEquals("Nope", env.error?.message)
        assertEquals("forbidden", env.error?.code)
    }

    @Test fun page_decodes_items_and_hasMore() {
        val json = """
            {"items":[{"state":"running","name":"a"},{"state":"stopped","name":"b"}],
             "meta":{"total":2,"page":1},"hasMore":true}
        """.trimIndent()
        val page = RefxJson.decodeFromString(Page.serializer(Dummy.serializer()), json)
        assertEquals(2, page.items.size)
        assertTrue(page.hasMore)
        assertEquals(2, page.meta?.total)
    }
}
