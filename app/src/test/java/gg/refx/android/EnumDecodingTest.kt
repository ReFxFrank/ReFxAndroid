package gg.refx.android

import gg.refx.android.core.network.RefxJson
import gg.refx.android.data.model.InvoiceState
import gg.refx.android.data.model.NodeState
import gg.refx.android.data.model.ServerState
import gg.refx.android.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract guard: every server enum decodes permissively — known raw values
 * (UPPERCASE SCREAMING_SNAKE_CASE) map to their case, and **unknown** raw values
 * map to `UNKNOWN` instead of throwing (§3.3 / parity spec §4).
 */
class EnumDecodingTest {

    @Test fun known_server_state_decodes() {
        assertEquals(ServerState.RUNNING, RefxJson.decodeFromString(ServerState.serializer(), "\"RUNNING\""))
        assertEquals(ServerState.SWITCHING_GAME, RefxJson.decodeFromString(ServerState.serializer(), "\"SWITCHING_GAME\""))
        assertEquals(ServerState.PENDING_PAYMENT, RefxJson.decodeFromString(ServerState.serializer(), "\"PENDING_PAYMENT\""))
    }

    @Test fun unknown_server_state_falls_back() {
        assertEquals(ServerState.UNKNOWN, RefxJson.decodeFromString(ServerState.serializer(), "\"WARP_DRIVE\""))
    }

    @Test fun enum_decoding_is_case_insensitive() {
        assertEquals(ServerState.RUNNING, RefxJson.decodeFromString(ServerState.serializer(), "\"running\""))
    }

    @Test fun unknown_values_across_enums_never_throw() {
        assertEquals(NodeState.UNKNOWN, RefxJson.decodeFromString(NodeState.serializer(), "\"FUTURE\""))
        assertEquals(InvoiceState.UNKNOWN, RefxJson.decodeFromString(InvoiceState.serializer(), "\"FUTURE\""))
        assertEquals(UserRole.UNKNOWN, RefxJson.decodeFromString(UserRole.serializer(), "\"SUPERADMIN\""))
    }

    @Test fun user_role_staff_flag() {
        assertTrue(RefxJson.decodeFromString(UserRole.serializer(), "\"ADMIN\"").isStaff)
        assertTrue(RefxJson.decodeFromString(UserRole.serializer(), "\"OWNER\"").isStaff)
        assertTrue(RefxJson.decodeFromString(UserRole.serializer(), "\"SUPPORT\"").isStaff)
        assertFalse(RefxJson.decodeFromString(UserRole.serializer(), "\"CUSTOMER\"").isStaff)
    }

    @Test fun enum_round_trips_to_raw() {
        assertEquals("\"RUNNING\"", RefxJson.encodeToString(ServerState.serializer(), ServerState.RUNNING))
    }
}
