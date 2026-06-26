package gg.refx.android.core.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Top-level tab a push deep-link targets. Billing lives under the Account tab. */
enum class PushTab { SERVERS, BILLING, SUPPORT }

/**
 * A pending deep-link from a tapped notification (parity spec §7). Carries the
 * target [tab] plus the entity id to open (server / invoice / ticket).
 */
data class PendingRoute(
    val tab: PushTab,
    val serverId: String? = null,
    val invoiceId: String? = null,
    val ticketId: String? = null,
)

/**
 * Holds the [pending] route until the nav host is ready to apply it. This survives
 * **cold launch**: MainActivity seeds it from the launch intent before the UI
 * exists, and the shell/tab consume it on first composition — not only on a live
 * "changed" signal (the iOS `PushRouter` pattern).
 */
class PushRouter {
    private val _pending = MutableStateFlow<PendingRoute?>(null)
    val pending: StateFlow<PendingRoute?> = _pending.asStateFlow()

    fun submit(route: PendingRoute?) {
        if (route != null) _pending.value = route
    }

    /** Clear once consumed by the nav host. */
    fun consume() {
        _pending.value = null
    }

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_SERVER_ID = "serverId"
        const val KEY_INVOICE_ID = "invoiceId"
        const val KEY_TICKET_ID = "ticketId"

        /**
         * Build a route from notification data. `type` is matched by lowercased
         * substring (iOS behaviour): server* → Servers, invoice/billing/payment →
         * Billing, ticket/support → Support; a bare serverId also routes to Servers.
         */
        fun fromData(data: Map<String, String?>): PendingRoute? {
            val type = data[KEY_TYPE]?.lowercase().orEmpty()
            val serverId = data[KEY_SERVER_ID]
            val invoiceId = data[KEY_INVOICE_ID]
            val ticketId = data[KEY_TICKET_ID]

            val tab = when {
                type.contains("server") -> PushTab.SERVERS
                type.contains("invoice") || type.contains("billing") || type.contains("payment") -> PushTab.BILLING
                type.contains("ticket") || type.contains("support") -> PushTab.SUPPORT
                serverId != null -> PushTab.SERVERS
                else -> return null
            }
            return PendingRoute(tab, serverId, invoiceId, ticketId)
        }
    }
}
