package gg.refx.android

import gg.refx.android.core.push.PushRouter
import gg.refx.android.core.push.PushTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Push deep-link routing (parity spec §7): type matched by lowercased substring. */
class PushRouterTest {

    @Test fun server_state_routes_to_servers_tab() {
        val route = PushRouter.fromData(mapOf("type" to "server.state", "serverId" to "srv_1"))!!
        assertEquals(PushTab.SERVERS, route.tab)
        assertEquals("srv_1", route.serverId)
    }

    @Test fun billing_invoice_routes_to_billing_tab() {
        val route = PushRouter.fromData(mapOf("type" to "billing.invoice", "invoiceId" to "inv_9"))!!
        assertEquals(PushTab.BILLING, route.tab)
        assertEquals("inv_9", route.invoiceId)
    }

    @Test fun payment_type_also_routes_to_billing() {
        assertEquals(PushTab.BILLING, PushRouter.fromData(mapOf("type" to "payment.failed"))!!.tab)
    }

    @Test fun support_reply_routes_to_support_tab() {
        val route = PushRouter.fromData(mapOf("type" to "support.reply", "ticketId" to "t_3"))!!
        assertEquals(PushTab.SUPPORT, route.tab)
        assertEquals("t_3", route.ticketId)
    }

    @Test fun bare_server_id_without_type_routes_to_servers() {
        val route = PushRouter.fromData(mapOf("serverId" to "srv_2"))!!
        assertEquals(PushTab.SERVERS, route.tab)
    }

    @Test fun matching_is_case_insensitive() {
        assertEquals(PushTab.SUPPORT, PushRouter.fromData(mapOf("type" to "SUPPORT.NEW"))!!.tab)
    }

    @Test fun unrecognized_payload_returns_null() {
        assertNull(PushRouter.fromData(mapOf("type" to "marketing.promo")))
        assertNull(PushRouter.fromData(emptyMap()))
    }
}
