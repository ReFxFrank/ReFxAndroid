package gg.refx.android

import gg.refx.android.core.network.RefxJson
import gg.refx.android.data.model.AdminCreateServerBody
import gg.refx.android.data.model.AdminGameTemplate
import gg.refx.android.data.model.CreateNodeBody
import gg.refx.android.data.model.CreateNodeResult
import gg.refx.android.data.model.CreditReason
import gg.refx.android.data.model.DeployMethod
import gg.refx.android.data.model.GrantCreditBody
import gg.refx.android.data.model.NodeAdmin
import gg.refx.android.data.model.NodeState
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract guards for admin provisioning request bodies + decode tolerance,
 * mirroring the iOS `AdminProvisioningTests`. These lock the exact wire shape of
 * the create-server / create-node / credit calls (parity spec §5, §9).
 */
class AdminProvisioningTest {

    private fun keys(encoded: String): Set<String> =
        RefxJson.parseToJsonElement(encoded).jsonObject.keys

    @Test fun create_server_minimal_drops_null_optionals() {
        val body = AdminCreateServerBody(name = "alpha", ownerId = "u1", nodeId = "n1", templateId = "t1")
        val encoded = RefxJson.encodeToString(AdminCreateServerBody.serializer(), body)
        // Exactly the four required keys — slots/swapMb/cpuCores/… omitted.
        assertEquals(setOf("name", "ownerId", "nodeId", "templateId"), keys(encoded))
    }

    @Test fun create_server_with_resources_includes_only_set_fields() {
        val body = AdminCreateServerBody(
            name = "beta", ownerId = "u1", nodeId = "n1", templateId = "t1",
            cpuCores = 2.0, memoryMb = 4096, diskMb = 20480,
        )
        val encoded = RefxJson.encodeToString(AdminCreateServerBody.serializer(), body)
        assertEquals(setOf("name", "ownerId", "nodeId", "templateId", "cpuCores", "memoryMb", "diskMb"), keys(encoded))
        assertFalse(keys(encoded).contains("slots"))
        assertFalse(keys(encoded).contains("swapMb"))
    }

    @Test fun create_node_body_has_exact_keys_and_os() {
        val body = CreateNodeBody(
            name = "edge-1", fqdn = "edge1.refx.gg", regionId = "r1", os = "LINUX",
            cpuCores = 8, memoryMb = 16384, diskMb = 200000,
            allocationPortStart = 25000, allocationPortEnd = 25500,
        )
        val encoded = RefxJson.encodeToString(CreateNodeBody.serializer(), body)
        assertEquals(
            setOf("name", "fqdn", "regionId", "os", "cpuCores", "memoryMb", "diskMb", "allocationPortStart", "allocationPortEnd"),
            keys(encoded),
        )
        assertTrue(encoded.contains("\"os\":\"LINUX\""))
    }

    @Test fun create_node_result_reads_bootstrap_token() {
        val json = """{"id":"n9","name":"edge-1","bootstrapToken":"BOOTSTRAP-XYZ"}"""
        val result = RefxJson.decodeFromString(CreateNodeResult.serializer(), json)
        assertEquals("BOOTSTRAP-XYZ", result.bootstrapToken)
    }

    @Test fun grant_credit_body_drops_null_reason_and_note() {
        val encoded = RefxJson.encodeToString(GrantCreditBody.serializer(), GrantCreditBody(amountMinor = -250))
        assertEquals(setOf("amountMinor"), keys(encoded))
        assertTrue(encoded.contains("\"amountMinor\":-250"))
    }

    @Test fun admin_game_template_unknown_deploy_method_falls_back() {
        val json = """{"id":"t","name":"Warp","slug":"warp","deployMethods":["DOCKER","WARP_DRIVE"]}"""
        val template = RefxJson.decodeFromString(AdminGameTemplate.serializer(), json)
        assertEquals(listOf(DeployMethod.DOCKER, DeployMethod.UNKNOWN), template.deployMethods)
    }

    @Test fun node_admin_decodes_permissively() {
        val json = """{"id":"n","name":"edge","state":"REBOOTING"}"""
        val node = RefxJson.decodeFromString(NodeAdmin.serializer(), json)
        assertEquals(NodeState.UNKNOWN, node.state)
    }

    @Test fun credit_reason_unknown_falls_back() {
        assertEquals(CreditReason.UNKNOWN, RefxJson.decodeFromString(CreditReason.serializer(), "\"LOYALTY_BONUS\""))
    }
}
