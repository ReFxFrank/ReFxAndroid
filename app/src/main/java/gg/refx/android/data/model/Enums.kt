package gg.refx.android.data.model

import gg.refx.android.core.network.PermissiveEnumSerializer
import kotlinx.serialization.Serializable

/**
 * Server enums, decoded **permissively** — unknown raw values map to `UNKNOWN`
 * rather than throwing (§3.3 / parity spec §4). Raw wire values are
 * **UPPERCASE SCREAMING_SNAKE_CASE** (verified against the iOS data models), with a
 * handful of lowercase exceptions (MFAMethod, EmailTheme, PayPalMode) noted below.
 *
 * Source citations are to `ReFxFrank/ReFxHostingApp` per the parity spec.
 */

@Serializable(with = ServerState.Serializer::class)
enum class ServerState(val raw: String) {
    INSTALLING("INSTALLING"),
    OFFLINE("OFFLINE"),
    STARTING("STARTING"),
    RUNNING("RUNNING"),
    STOPPING("STOPPING"),
    CRASHED("CRASHED"),
    SUSPENDED("SUSPENDED"),
    REINSTALLING("REINSTALLING"),
    SWITCHING_GAME("SWITCHING_GAME"),
    TRANSFERRING("TRANSFERRING"),
    PENDING_PAYMENT("PENDING_PAYMENT"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<ServerState>("ServerState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = UserRole.Serializer::class)
enum class UserRole(val raw: String) {
    PENDING_CUSTOMER("PENDING_CUSTOMER"),
    CUSTOMER("CUSTOMER"),
    SUPPORT("SUPPORT"),
    ADMIN("ADMIN"),
    OWNER("OWNER"),
    UNKNOWN("UNKNOWN");

    /** SUPPORT/ADMIN/OWNER see the Staff tab (StaffHomeView role-router). */
    val isStaff: Boolean get() = this == SUPPORT || this == ADMIN || this == OWNER

    internal object Serializer :
        PermissiveEnumSerializer<UserRole>("UserRole", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = TicketState.Serializer::class)
enum class TicketState(val raw: String) {
    OPEN("OPEN"),
    PENDING_CUSTOMER("PENDING_CUSTOMER"),
    PENDING_AGENT("PENDING_AGENT"),
    RESOLVED("RESOLVED"),
    CLOSED("CLOSED"),
    ARCHIVED("ARCHIVED"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<TicketState>("TicketState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = TicketPriority.Serializer::class)
enum class TicketPriority(val raw: String) {
    LOW("LOW"),
    NORMAL("NORMAL"),
    HIGH("HIGH"),
    URGENT("URGENT"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<TicketPriority>("TicketPriority", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = ProductType.Serializer::class)
enum class ProductType(val raw: String) {
    GAME_SERVER("GAME_SERVER"),
    VOICE_SERVER("VOICE_SERVER"),
    VPS("VPS"),
    DEDICATED("DEDICATED"),
    ADDON("ADDON"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<ProductType>("ProductType", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = BillingModel.Serializer::class)
enum class BillingModel(val raw: String) {
    HARDWARE_TIER("HARDWARE_TIER"),
    PER_SLOT("PER_SLOT"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<BillingModel>("BillingModel", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = BillingInterval.Serializer::class)
enum class BillingInterval(val raw: String) {
    WEEKLY("WEEKLY"),
    BIWEEKLY("BIWEEKLY"),
    MONTHLY("MONTHLY"),
    QUARTERLY("QUARTERLY"),
    SEMIANNUAL("SEMIANNUAL"),
    ANNUAL("ANNUAL"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<BillingInterval>("BillingInterval", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = DeployMethod.Serializer::class)
enum class DeployMethod(val raw: String) {
    DOCKER("DOCKER"),
    NATIVE_PROCESS("NATIVE_PROCESS"),
    WINDOWS_CONTAINER("WINDOWS_CONTAINER"),
    SANDBOX("SANDBOX"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<DeployMethod>("DeployMethod", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = VariableType.Serializer::class)
enum class VariableType(val raw: String) {
    STRING("STRING"),
    NUMBER("NUMBER"),
    BOOLEAN("BOOLEAN"),
    ENUM("ENUM"),
    SECRET("SECRET"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<VariableType>("VariableType", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = CouponKind.Serializer::class)
enum class CouponKind(val raw: String) {
    PERCENT("PERCENT"),
    FIXED("FIXED"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<CouponKind>("CouponKind", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = InvoiceState.Serializer::class)
enum class InvoiceState(val raw: String) {
    DRAFT("DRAFT"),
    OPEN("OPEN"),
    PAID("PAID"),
    VOID("VOID"),
    UNCOLLECTIBLE("UNCOLLECTIBLE"),
    REFUNDED("REFUNDED"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<InvoiceState>("InvoiceState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = PaymentState.Serializer::class)
enum class PaymentState(val raw: String) {
    PENDING("PENDING"),
    SUCCEEDED("SUCCEEDED"),
    FAILED("FAILED"),
    REFUNDED("REFUNDED"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<PaymentState>("PaymentState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = SubscriptionState.Serializer::class)
enum class SubscriptionState(val raw: String) {
    TRIALING("TRIALING"),
    ACTIVE("ACTIVE"),
    PAST_DUE("PAST_DUE"),
    CANCELED("CANCELED"),
    SUSPENDED("SUSPENDED"),
    EXPIRED("EXPIRED"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<SubscriptionState>("SubscriptionState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = CreditReason.Serializer::class)
enum class CreditReason(val raw: String) {
    ADMIN_GRANT("ADMIN_GRANT"),
    REFUND("REFUND"),
    GIFT_CARD("GIFT_CARD"),
    INVOICE_PAYMENT("INVOICE_PAYMENT"),
    ADJUSTMENT("ADJUSTMENT"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<CreditReason>("CreditReason", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = NodeState.Serializer::class)
enum class NodeState(val raw: String) {
    PROVISIONING("PROVISIONING"),
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    MAINTENANCE("MAINTENANCE"),
    DEGRADED("DEGRADED"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<NodeState>("NodeState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = AlertSeverity.Serializer::class)
enum class AlertSeverity(val raw: String) {
    INFO("INFO"),
    WARNING("WARNING"),
    CRITICAL("CRITICAL"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<AlertSeverity>("AlertSeverity", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = BackupState.Serializer::class)
enum class BackupState(val raw: String) {
    PENDING("PENDING"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<BackupState>("BackupState", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = ScheduleAction.Serializer::class)
enum class ScheduleAction(val raw: String) {
    COMMAND("COMMAND"),
    POWER("POWER"),
    BACKUP("BACKUP"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<ScheduleAction>("ScheduleAction", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable(with = DbEngine.Serializer::class)
enum class DbEngine(val raw: String) {
    MYSQL("MYSQL"),
    MARIADB("MARIADB"),
    UNKNOWN("UNKNOWN");

    internal object Serializer :
        PermissiveEnumSerializer<DbEngine>("DbEngine", entries.toTypedArray(), UNKNOWN, { it.raw })
}

/**
 * MFA method — lowercase case-name raws (exception to the SCREAMING_SNAKE rule).
 */
@Serializable(with = MFAMethod.Serializer::class)
enum class MFAMethod(val raw: String) {
    TOTP("totp"),
    RECOVERY("recovery"),
    WEBAUTHN("webauthn"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<MFAMethod>("MFAMethod", entries.toTypedArray(), UNKNOWN, { it.raw })
}

/** UI-only OS selector for node creation; sent as a raw String ("LINUX"/"WINDOWS"). */
enum class NodeOS(val raw: String) { LINUX("LINUX"), WINDOWS("WINDOWS") }

/** Power signal sent to `POST servers/{id}/power` — lowercase, body key `signal`. */
enum class PowerSignal(val raw: String) {
    START("start"), STOP("stop"), RESTART("restart"), KILL("kill")
}
