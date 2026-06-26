package gg.refx.android.data.model

import gg.refx.android.core.network.InstantIso8601Serializer
import gg.refx.android.core.network.Money
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Staff/Admin models (parity spec §3, `AdminModels.swift`). Permissive decoding;
 * raw String states where the iOS model uses raw strings.
 */

// ── Overview / metrics ──────────────────────────────────────────────────────

@Serializable
data class AdminMetrics(
    val totals: AdminTotals = AdminTotals(),
    val serversByState: Map<String, Int> = emptyMap(),
    val nodes: List<NodeMetric> = emptyList(),
)

@Serializable
data class AdminTotals(
    val users: Int = 0,
    val servers: Int = 0,
    val nodesOnline: Int = 0,
    val openTickets: Int = 0,
    val activeSubscriptions: Int = 0,
    val mrrMinor: Long = 0,
    val mrrCurrency: String? = null,
) {
    val mrr: Money get() = Money(mrrMinor, mrrCurrency ?: "USD")
}

@Serializable
data class NodeMetric(
    val id: String,
    val name: String,
    val cpuPct: Double = 0.0,
    val memPct: Double = 0.0,
    val diskPct: Double = 0.0,
)

@Serializable
data class BillingSummary(
    val currency: String = "USD",
    val revenueMinor: Long = 0,
    val outstandingMinor: Long = 0,
    val activeSubscriptions: Int = 0,
    val openInvoices: Int = 0,
    val paidInvoices: Int = 0,
) {
    val revenue: Money get() = Money(revenueMinor, currency)
    val outstanding: Money get() = Money(outstandingMinor, currency)
}

// ── Users ───────────────────────────────────────────────────────────────────

@Serializable
data class AdminUser(
    val id: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val globalRole: UserRole? = null,
    val state: String? = null,
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { email }
}

@Serializable
data class AdminUserDetail(
    val id: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val globalRole: UserRole? = null,
    val state: String? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val emailVerifiedAt: Instant? = null,
    val ownedServers: List<AdminServerRef> = emptyList(),
    val subscriptions: List<AdminSubscription> = emptyList(),
    val invoices: List<AdminInvoice> = emptyList(),
    @SerialName("_count") val counts: AdminUserCounts? = null,
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { email }
    val emailVerified: Boolean get() = emailVerifiedAt != null
}

@Serializable
data class AdminUserCounts(
    val ownedServers: Int? = null,
    val subscriptions: Int? = null,
    val tickets: Int? = null,
)

@Serializable
data class AdminServerRef(
    val id: String,
    val shortId: String? = null,
    val name: String,
    val state: ServerState = ServerState.UNKNOWN,
    val node: NodeNameRef? = null,
)

@Serializable
data class NodeNameRef(val name: String? = null)

@Serializable
data class AdminSubscription(
    val id: String,
    val state: String? = null,
    val interval: String? = null,
    val gateway: String? = null,
    @Serializable(with = InstantIso8601Serializer::class) val currentPeriodEnd: Instant? = null,
    val product: AdminSubProductRef? = null,
)

@Serializable
data class AdminSubProductRef(
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
)

@Serializable
data class AdminInvoice(
    val id: String,
    val number: String? = null,
    val state: String? = null,
    val currency: String = "USD",
    val totalMinor: Long = 0,
    val amountPaidMinor: Long? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val paidAt: Instant? = null,
) {
    val total: Money get() = Money(totalMinor, currency)
}

/** Body for `PATCH admin/users/{id}` (state: ACTIVE/SUSPENDED/BANNED). */
@Serializable
data class UpdateUserStateRequest(val state: String)

/** Body for `PATCH admin/users/{id}/role`. */
@Serializable
data class UpdateUserRoleRequest(val role: String)

/** Body for `POST admin/users/{id}/credit` (negative to deduct). */
@Serializable
data class GrantCreditBody(
    val amountMinor: Long,
    val reason: CreditReason? = null,
    val note: String? = null,
)

// ── Nodes ───────────────────────────────────────────────────────────────────

@Serializable
data class NodeAdmin(
    val id: String,
    val name: String,
    val fqdn: String? = null,
    val state: NodeState = NodeState.UNKNOWN,
    val agentVersion: String? = null,
    val maintenance: Boolean? = null,
    val region: NodeRegionRef? = null,
    val memoryMb: Int? = null,
    val diskMb: Int? = null,
)

@Serializable
data class NodeRegionRef(
    val name: String? = null,
    val code: String? = null,
)

@Serializable
data class NodePing(
    val ms: Double? = null,
    val reachable: Boolean = false,
    val heartbeatAgeMs: Double? = null,
)

@Serializable
data class AgentLatest(val latest: String? = null)

/** `POST admin/nodes` result — carries the one-time bootstrap token (§5). */
@Serializable
data class CreateNodeResult(
    val id: String? = null,
    val name: String? = null,
    val bootstrapToken: String,
)

/** Body for `POST admin/nodes` (parity spec §5 verbatim; os = "LINUX" | "WINDOWS"). */
@Serializable
data class CreateNodeBody(
    val name: String,
    val fqdn: String,
    val regionId: String,
    val os: String,
    val cpuCores: Int,
    val memoryMb: Int,
    val diskMb: Int,
    val allocationPortStart: Int,
    val allocationPortEnd: Int,
)

// ── Admin server provisioning ───────────────────────────────────────────────

/**
 * Body for `POST admin/servers` (parity spec §5 verbatim). Nil optionals are
 * omitted on the wire (RefxJson.explicitNulls = false) — matching the iOS
 * `AdminProvisioningTests` assertions.
 */
@Serializable
data class AdminCreateServerBody(
    val name: String,
    val ownerId: String,
    val nodeId: String,
    val templateId: String,
    val cpuCores: Double? = null,
    val memoryMb: Int? = null,
    val diskMb: Int? = null,
    val slots: Int? = null,
    val swapMb: Int? = null,
    val environment: Map<String, String>? = null,
)

// ── Audit / alerts ──────────────────────────────────────────────────────────

@Serializable
data class AuditEntry(
    val id: String,
    val actorId: String? = null,
    val action: String,
    val targetType: String? = null,
    val targetId: String? = null,
    val ip: String? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
)

@Serializable
data class AdminAlert(
    val id: String,
    val severity: AlertSeverity? = null,
    val title: String,
    val body: String,
    val isActive: Boolean = true,
    @Serializable(with = InstantIso8601Serializer::class) val startsAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val endsAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
)

@Serializable
data class CreateAlertRequest(
    val severity: String,
    val title: String,
    val body: String,
    val isActive: Boolean = true,
)

@Serializable
data class UpdateAlertRequest(val isActive: Boolean)
