package gg.refx.android

import gg.refx.android.core.network.RefxJson
import gg.refx.android.data.model.LiveStats
import gg.refx.android.data.model.Server
import gg.refx.android.data.model.ServerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Contract guard for Server / Allocation / LiveStats decoding (parity spec §3). */
class ServerDecodingTest {

    @Test fun server_decodes_with_computed_connection_string() {
        val json = """
            {"id":"srv_1","shortId":"abcd","name":"My SMP","state":"RUNNING",
             "cpuCores":2.0,"memoryMb":4096,"diskMb":20480,
             "template":{"id":"t1","name":"Minecraft","slug":"minecraft-java","supportsWorkshop":false},
             "node":{"name":"node-1","fqdn":"n1.refx.gg"},
             "primaryAllocation":{"id":"a1","ip":"1.2.3.4","port":25565,"alias":"play.example.com","isPrimary":true}}
        """.trimIndent()
        val server = RefxJson.decodeFromString(Server.serializer(), json)
        assertEquals(ServerState.RUNNING, server.state)
        assertEquals("Minecraft", server.gameName)
        assertEquals("play.example.com:25565", server.connectionString)
        assertNull(server.suspendedAt)
    }

    @Test fun allocation_falls_back_to_ip_without_alias() {
        val json = """
            {"id":"s","shortId":"s","name":"n","state":"OFFLINE",
             "primaryAllocation":{"id":"a","ip":"10.0.0.5","port":27015,"isPrimary":true}}
        """.trimIndent()
        val server = RefxJson.decodeFromString(Server.serializer(), json)
        assertEquals("10.0.0.5:27015", server.connectionString)
    }

    @Test fun server_with_unknown_state_does_not_throw() {
        val json = """{"id":"s","shortId":"s","name":"n","state":"WARP_DRIVE"}"""
        val server = RefxJson.decodeFromString(Server.serializer(), json)
        assertEquals(ServerState.UNKNOWN, server.state)
        assertNull(server.connectionString)
    }

    @Test fun live_stats_decode() {
        val json = """{"state":"RUNNING","cpuPct":42.5,"memUsedMb":1024.0,"memTotalMb":4096.0,
                       "diskUsedMb":5000.0,"netRxBytes":10.0,"netTxBytes":20.0,"players":3,"uptimeMs":1000.0}"""
        val stats = RefxJson.decodeFromString(LiveStats.serializer(), json)
        assertEquals(42.5, stats.cpuPct, 0.0001)
        assertEquals(4096.0, stats.memTotalMb!!, 0.0001)
        assertEquals(3, stats.players)
        assertEquals(ServerState.RUNNING, stats.state)
    }
}
