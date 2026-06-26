package gg.refx.android.data.model

import androidx.compose.ui.graphics.Color
import gg.refx.android.core.design.DesignTokens

/**
 * Maps server/node/billing states to a label + color for StatePill/StatusChip.
 * Mappings verified against the iOS views (parity spec §2 "state → token maps").
 */
object StateColors {

    fun server(state: ServerState): Pair<String, Color> = when (state) {
        ServerState.RUNNING -> "Running" to DesignTokens.AppSuccess
        ServerState.STARTING -> "Starting" to DesignTokens.AppWarning
        ServerState.STOPPING -> "Stopping" to DesignTokens.AppWarning
        ServerState.INSTALLING -> "Installing" to DesignTokens.AppWarning
        ServerState.REINSTALLING -> "Reinstalling" to DesignTokens.AppWarning
        ServerState.SWITCHING_GAME -> "Switching game" to DesignTokens.AppWarning
        ServerState.TRANSFERRING -> "Transferring" to DesignTokens.AppWarning
        ServerState.OFFLINE -> "Offline" to DesignTokens.AppMuted
        ServerState.CRASHED -> "Crashed" to DesignTokens.AppDestructive
        ServerState.SUSPENDED -> "Suspended" to DesignTokens.AppDestructive
        ServerState.PENDING_PAYMENT -> "Pending payment" to DesignTokens.AppDestructive
        ServerState.UNKNOWN -> "Unknown" to DesignTokens.AppMuted
    }

    fun node(state: NodeState): Pair<String, Color> = when (state) {
        NodeState.ONLINE -> "Online" to DesignTokens.AppSuccess
        NodeState.DEGRADED -> "Degraded" to DesignTokens.AppWarning
        NodeState.MAINTENANCE -> "Maintenance" to DesignTokens.AppWarning
        NodeState.PROVISIONING -> "Provisioning" to DesignTokens.AppWarning
        NodeState.OFFLINE -> "Offline" to DesignTokens.AppDestructive
        NodeState.UNKNOWN -> "Unknown" to DesignTokens.AppMuted
    }

    fun invoice(state: InvoiceState): Pair<String, Color> = when (state) {
        InvoiceState.PAID -> "Paid" to DesignTokens.AppSuccess
        InvoiceState.OPEN -> "Open" to DesignTokens.AppWarning
        InvoiceState.VOID -> "Void" to DesignTokens.AppDestructive
        InvoiceState.UNCOLLECTIBLE -> "Uncollectible" to DesignTokens.AppDestructive
        InvoiceState.DRAFT -> "Draft" to DesignTokens.AppMuted
        InvoiceState.REFUNDED -> "Refunded" to DesignTokens.AppMuted
        InvoiceState.UNKNOWN -> "Unknown" to DesignTokens.AppMuted
    }

    fun subscription(state: SubscriptionState): Pair<String, Color> = when (state) {
        SubscriptionState.ACTIVE -> "Active" to DesignTokens.AppSuccess
        SubscriptionState.TRIALING -> "Trialing" to DesignTokens.AppSuccess
        SubscriptionState.PAST_DUE -> "Past due" to DesignTokens.AppWarning
        SubscriptionState.SUSPENDED -> "Suspended" to DesignTokens.AppWarning
        SubscriptionState.CANCELED -> "Canceled" to DesignTokens.AppMuted
        SubscriptionState.EXPIRED -> "Expired" to DesignTokens.AppMuted
        SubscriptionState.UNKNOWN -> "Unknown" to DesignTokens.AppMuted
    }
}
