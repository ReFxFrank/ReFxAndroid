package gg.refx.android

import gg.refx.android.core.network.RefxJson
import gg.refx.android.data.model.CreditBalance
import gg.refx.android.data.model.Invoice
import gg.refx.android.data.model.InvoiceState
import gg.refx.android.data.model.SubscriptionListItem
import gg.refx.android.data.model.SubscriptionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Billing decoding + computed helpers (parity spec §3 / §9 customer-billing tests). */
class BillingDecodingTest {

    @Test fun invoice_outstanding_is_total_minus_paid_clamped() {
        val json = """{"id":"i","number":"INV-1","state":"OPEN","currency":"USD",
            "subtotalMinor":1000,"taxMinor":0,"totalMinor":1200,"amountPaidMinor":500}"""
        val inv = RefxJson.decodeFromString(Invoice.serializer(), json)
        assertEquals(700L, inv.outstanding.minorUnits)
        assertFalse(inv.isPaid)
    }

    @Test fun invoice_overpaid_outstanding_clamps_to_zero_and_is_paid() {
        val json = """{"id":"i","number":"INV-2","state":"PAID","currency":"USD",
            "totalMinor":1000,"amountPaidMinor":1500}"""
        val inv = RefxJson.decodeFromString(Invoice.serializer(), json)
        assertEquals(0L, inv.outstanding.minorUnits)
        assertTrue(inv.isPaid)
    }

    @Test fun invoice_unknown_state_does_not_throw() {
        val json = """{"id":"i","number":"INV-3","state":"FROZEN","currency":"USD","totalMinor":100}"""
        val inv = RefxJson.decodeFromString(Invoice.serializer(), json)
        assertEquals(InvoiceState.UNKNOWN, inv.state)
    }

    @Test fun subscription_renewal_and_resume_flag() {
        val json = """{"id":"s","interval":"MONTHLY","slots":1,"state":"ACTIVE",
            "cancelAtPeriodEnd":true,"renewalAmountMinor":1599,"currency":"USD",
            "product":{"id":"p","name":"Minecraft Plan","type":"GAME_SERVER","billingModel":"HARDWARE_TIER","perSlot":false}}"""
        val sub = RefxJson.decodeFromString(SubscriptionListItem.serializer(), json)
        assertEquals("Minecraft Plan", sub.productName)
        assertEquals(SubscriptionState.ACTIVE, sub.state)
        assertTrue(sub.canResume) // cancelAtPeriodEnd && not expired
        assertTrue(sub.renewalAmount.formatted.contains("15.99"))
    }

    @Test fun credit_transaction_signed_labels() {
        val json = """{"balanceMinor":2500,"currency":"USD","transactions":[
            {"id":"t1","amountMinor":500,"reason":"ADMIN_GRANT"},
            {"id":"t2","amountMinor":-250,"reason":"INVOICE_PAYMENT"}]}"""
        val cb = RefxJson.decodeFromString(CreditBalance.serializer(), json)
        assertEquals(2, cb.transactions.size)
        assertTrue(cb.transactions[0].isCredit)
        assertTrue(cb.transactions[0].signedLabel.startsWith("+"))
        assertFalse(cb.transactions[1].isCredit)
        assertTrue(cb.transactions[1].signedLabel.startsWith("-"))
    }
}
