package gg.refx.android

import gg.refx.android.core.network.RefxJson
import gg.refx.android.data.model.InvoiceState
import gg.refx.android.data.model.NodeState
import gg.refx.android.data.model.ServerState
import gg.refx.android.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract guard: every server enum decodes permissively — known raw values map
 * to their case, and **unknown** raw values map to `UNKNOWN` instead of throwing
 * (§3.3). Mirrors the iOS decoding tests.
 */
class EnumDecodingTest {

    @Test fun known_server_state_decodes() {
        assertEquals(ServerState.RUNNING, RefxJson.decodeFromString(ServerState.serializer(), "\"running\""))
    }

    @Test fun unknown_server_state_falls_back() {
        assertEquals(ServerState.UNKNOWN, RefxJson.decodeFromString(ServerState.serializer(), "\"brand_new_state\""))
    }

    @Test fun enum_decoding_is_case_insensitive() {
        assertEquals(ServerState.RUNNING, RefxJson.decodeFromString(ServerState.serializer(), "\"RUNNING\""))
    }

    @Test fun unknown_values_across_enums_never_throw() {
        assertEquals(NodeState.UNKNOWN, RefxJson.decodeFromString(NodeState.serializer(), "\"future\""))
        assertEquals(InvoiceState.UNKNOWN, RefxJson.decodeFromString(InvoiceState.serializer(), "\"future\""))
        assertEquals(UserRole.UNKNOWN, RefxJson.decodeFromString(UserRole.serializer(), "\"superadmin\""))
    }

    @Test fun user_role_staff_flag() {
        assertTrue(RefxJson.decodeFromString(UserRole.serializer(), "\"admin\"").isStaff)
        assertTrue(RefxJson.decodeFromString(UserRole.serializer(), "\"staff\"").isStaff)
        assertTrue(!RefxJson.decodeFromString(UserRole.serializer(), "\"user\"").isStaff)
    }

    @Test fun enum_round_trips_to_raw() {
        assertEquals("\"running\"", RefxJson.encodeToString(ServerState.serializer(), ServerState.RUNNING))
    }
}
