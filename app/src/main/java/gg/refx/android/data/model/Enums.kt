package gg.refx.android.data.model

import gg.refx.android.core.network.PermissiveEnumSerializer
import kotlinx.serialization.Serializable

/**
 * Server enums, decoded **permissively** — unknown raw values map to `UNKNOWN`
 * rather than throwing (§3.3). Each enum carries its wire `raw` value and exposes
 * a [PermissiveEnumSerializer].
 *
 * NOTE: this is the representative starter set wired into the contract tests. The
 * remaining enums from §3.3 (CouponKind, VariableType, DeployMethod, NodeOS,
 * ProductType, CreditReason, BillingModel, alert severity, …) follow the same
 * pattern and are added alongside their models. Reconcile the raw strings against
 * the panel DTOs once that repo is available.
 */

@Serializable(with = ServerState.Serializer::class)
enum class ServerState(val raw: String) {
    INSTALLING("installing"),
    STARTING("starting"),
    RUNNING("running"),
    STOPPING("stopping"),
    STOPPED("stopped"),
    OFFLINE("offline"),
    SUSPENDED("suspended"),
    ERROR("error"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<ServerState>("ServerState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = NodeState.Serializer::class)
enum class NodeState(val raw: String) {
    ONLINE("online"),
    OFFLINE("offline"),
    DEGRADED("degraded"),
    MAINTENANCE("maintenance"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<NodeState>("NodeState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = NodeOS.Serializer::class)
enum class NodeOS(val raw: String) {
    LINUX("linux"),
    WINDOWS("windows"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<NodeOS>("NodeOS", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = InvoiceState.Serializer::class)
enum class InvoiceState(val raw: String) {
    DRAFT("draft"),
    OPEN("open"),
    PAID("paid"),
    VOID("void"),
    UNCOLLECTIBLE("uncollectible"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<InvoiceState>("InvoiceState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = PaymentState.Serializer::class)
enum class PaymentState(val raw: String) {
    PENDING("pending"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    REFUNDED("refunded"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<PaymentState>("PaymentState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = SubscriptionState.Serializer::class)
enum class SubscriptionState(val raw: String) {
    ACTIVE("active"),
    PAST_DUE("past_due"),
    CANCELED("canceled"),
    PAUSED("paused"),
    TRIALING("trialing"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<SubscriptionState>("SubscriptionState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = BillingInterval.Serializer::class)
enum class BillingInterval(val raw: String) {
    MONTHLY("monthly"),
    QUARTERLY("quarterly"),
    YEARLY("yearly"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<BillingInterval>("BillingInterval", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = UserRole.Serializer::class)
enum class UserRole(val raw: String) {
    USER("user"),
    STAFF("staff"),
    ADMIN("admin"),
    UNKNOWN("unknown");

    val isStaff: Boolean get() = this == STAFF || this == ADMIN

    internal object Serializer :
        PermissiveEnumSerializer<UserRole>("UserRole", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = UserState.Serializer::class)
enum class UserState(val raw: String) {
    ACTIVE("active"),
    SUSPENDED("suspended"),
    BANNED("banned"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<UserState>("UserState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = TicketState.Serializer::class)
enum class TicketState(val raw: String) {
    OPEN("open"),
    PENDING("pending"),
    RESOLVED("resolved"),
    CLOSED("closed"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<TicketState>("TicketState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = TicketPriority.Serializer::class)
enum class TicketPriority(val raw: String) {
    LOW("low"),
    NORMAL("normal"),
    HIGH("high"),
    URGENT("urgent"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<TicketPriority>("TicketPriority", entries.toTypedArray(), UNKNOWN, { it.raw })
}
