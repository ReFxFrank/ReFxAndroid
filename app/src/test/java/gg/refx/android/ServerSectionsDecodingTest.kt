package gg.refx.android

import gg.refx.android.core.network.RefxJson
import gg.refx.android.data.model.Backup
import gg.refx.android.data.model.BackupState
import gg.refx.android.data.model.PlanChangeResult
import gg.refx.android.data.model.PlanChangeStatus
import gg.refx.android.data.model.ServerPermissionCatalog
import gg.refx.android.data.model.ServerVariable
import gg.refx.android.data.model.SubUser
import gg.refx.android.data.model.UpgradeTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Decode guards for the M4 server-section models (parity spec §3/§4/§5). */
class ServerSectionsDecodingTest {

    @Test fun backup_size_formatting_and_state() {
        val json = """{"id":"b1","name":"daily","state":"COMPLETED","bytes":1610612736}"""
        val backup = RefxJson.decodeFromString(Backup.serializer(), json)
        assertEquals(BackupState.COMPLETED, backup.state)
        assertEquals("daily", backup.displayName)
        assertTrue(backup.sizeLabel!!.endsWith("GB"))
    }

    @Test fun subuser_email_from_account() {
        val json = """{"id":"su1","permissions":["control.start","files.read"],"user":{"id":"u1","email":"helper@x.com"}}"""
        val su = RefxJson.decodeFromString(SubUser.serializer(), json)
        assertEquals("helper@x.com", su.email)
        assertEquals(2, su.permissions.size)
    }

    @Test fun permission_catalog_is_complete_and_labeled() {
        assertTrue(ServerPermissionCatalog.all.contains("control.start"))
        assertTrue(ServerPermissionCatalog.all.contains("settings.update"))
        assertEquals("Start", ServerPermissionCatalog.label("control.start"))
    }

    @Test fun server_variable_label_and_editable_default() {
        val json = """{"envName":"SERVER_MEMORY","displayName":"Max memory","value":"2048"}"""
        val v = RefxJson.decodeFromString(ServerVariable.serializer(), json)
        assertEquals("Max memory", v.label)
        assertTrue(v.userEditable) // defaults true
    }

    @Test fun plan_change_status_is_permissive_lowercase() {
        assertEquals(PlanChangeStatus.SCHEDULED, RefxJson.decodeFromString(PlanChangeStatus.serializer(), "\"scheduled\""))
        assertEquals(PlanChangeStatus.UNKNOWN, RefxJson.decodeFromString(PlanChangeStatus.serializer(), "\"deferred\""))
        val result = RefxJson.decodeFromString(PlanChangeResult.serializer(), """{"status":"applied"}""")
        assertEquals(PlanChangeStatus.APPLIED, result.status)
        assertNull(result.invoiceId)
    }

    @Test fun upgrade_tier_specs_and_price() {
        val json = """{"id":"t","name":"Pro","cpuCores":4.0,"memoryMb":8192,"diskMb":51200,"priceMinor":2999,"currency":"USD"}"""
        val tier = RefxJson.decodeFromString(UpgradeTier.serializer(), json)
        assertTrue(tier.specs.contains("8192"))
        assertTrue(tier.price!!.formatted.contains("29.99"))
        assertFalse(tier.specs.isEmpty())
    }
}
