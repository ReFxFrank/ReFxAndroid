package gg.refx.android.data.model

import gg.refx.android.core.network.InstantIso8601Serializer
import gg.refx.android.core.network.Money
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Customer billing models (parity spec §3, `CustomerBillingModels.swift`). Money
 * fields are integer minor units + currency; computed `Money` accessors format them.
 */

@Serializable
data class Invoice(
    val id: String,
    val number: String,
    val userId: String? = null,
    val subscriptionId: String? = null,
    val state: InvoiceState = InvoiceState.UNKNOWN,
    val currency: String,
    val subtotalMinor: Long = 0,
    val discountMinor: Long = 0,
    val couponCode: String? = null,
    val taxMinor: Long = 0,
    val totalMinor: Long = 0,
    val amountPaidMinor: Long = 0,
    val taxType: String? = null,
    val taxRatePct: Double? = null,
    @Serializable(with = InstantIso8601Serializer::class) val dueAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val paidAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    val lineItems: List<InvoiceLineItem>? = null,
    val payments: List<InvoicePayment>? = null,
) {
    val total: Money get() = Money(totalMinor, currency)
    val subtotal: Money get() = Money(subtotalMinor, currency)
    val discount: Money get() = Money(discountMinor, currency)
    val tax: Money get() = Money(taxMinor, currency)
    val amountPaid: Money get() = Money(amountPaidMinor, currency)

    /** Amount still owed, clamped to >= 0 (mirrors iOS outstanding). */
    val outstanding: Money get() = Money((totalMinor - amountPaidMinor).coerceAtLeast(0), currency)
    val isPaid: Boolean get() = state == InvoiceState.PAID || outstanding.minorUnits == 0L
}

@Serializable
data class InvoiceLineItem(
    val id: String,
    val invoiceId: String? = null,
    val description: String,
    val quantity: Int = 1,
    val unitMinor: Long = 0,
    val amountMinor: Long = 0,
)

@Serializable
data class InvoicePayment(
    val id: String,
    val invoiceId: String? = null,
    val gateway: String,
    val gatewayRef: String? = null,
    val amountMinor: Long = 0,
    val currency: String,
    val state: PaymentState = PaymentState.UNKNOWN,
    val failureReason: String? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
) {
    val amount: Money get() = Money(amountMinor, currency)
}

@Serializable
data class SubscriptionListItem(
    val id: String,
    val productId: String? = null,
    val priceId: String? = null,
    val interval: BillingInterval = BillingInterval.UNKNOWN,
    val slots: Int = 1,
    val state: SubscriptionState = SubscriptionState.UNKNOWN,
    @Serializable(with = InstantIso8601Serializer::class) val currentPeriodStart: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val currentPeriodEnd: Instant? = null,
    val cancelAtPeriodEnd: Boolean = false,
    val autoRenew: Boolean = true,
    val gateway: String? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    val product: ProductRef? = null,
    val hardwareTier: TierRef? = null,
    val servers: List<ServerRef> = emptyList(),
    val renewalAmountMinor: Long = 0,
    val currency: String = "USD",
) {
    val renewalAmount: Money get() = Money(renewalAmountMinor, currency)
    val productName: String get() = product?.name ?: "Subscription"
    val canResume: Boolean get() = cancelAtPeriodEnd && state != SubscriptionState.EXPIRED
}

@Serializable
data class ProductRef(
    val id: String,
    val name: String,
    val type: ProductType = ProductType.UNKNOWN,
    val billingModel: BillingModel = BillingModel.UNKNOWN,
    val perSlot: Boolean = false,
)

@Serializable
data class TierRef(
    val id: String,
    val name: String,
    val cpuCores: Double = 0.0,
    val memoryMb: Int = 0,
    val diskMb: Int = 0,
)

@Serializable
data class ServerRef(
    val id: String,
    val shortId: String,
    val name: String,
    // Raw string state (not the ServerState enum), per the iOS model.
    val state: String? = null,
)

@Serializable
data class Subscription(
    val id: String,
    val userId: String? = null,
    val productId: String? = null,
    val priceId: String? = null,
    val hardwareTierId: String? = null,
    val interval: BillingInterval = BillingInterval.UNKNOWN,
    val slots: Int = 1,
    val state: SubscriptionState = SubscriptionState.UNKNOWN,
    @Serializable(with = InstantIso8601Serializer::class) val currentPeriodStart: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val currentPeriodEnd: Instant? = null,
    val cancelAtPeriodEnd: Boolean = false,
    val autoRenew: Boolean = true,
    val gateway: String? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val updatedAt: Instant? = null,
)

@Serializable
data class PaymentMethod(
    val id: String,
    val userId: String? = null,
    val gateway: String,
    val gatewayRef: String? = null,
    val brand: String? = null,
    val last4: String? = null,
    val expMonth: Int? = null,
    val expYear: Int? = null,
    val isDefault: Boolean = false,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
) {
    val display: String
        get() = when {
            brand != null && last4 != null -> "$brand •••• $last4"
            last4 != null -> "•••• $last4"
            else -> gateway.replaceFirstChar { it.uppercase() }
        }
}

@Serializable
data class CreditBalance(
    val balanceMinor: Long = 0,
    val transactions: List<CreditTransaction> = emptyList(),
    val currency: String = "USD",
) {
    val balance: Money get() = Money(balanceMinor, currency)
}

@Serializable
data class CreditTransaction(
    val id: String,
    val userId: String? = null,
    val amountMinor: Long = 0,
    val reason: CreditReason = CreditReason.UNKNOWN,
    val note: String? = null,
    val invoiceId: String? = null,
    val actorId: String? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    val currency: String = "USD",
) {
    val isCredit: Boolean get() = amountMinor >= 0
    val amount: Money get() = Money(amountMinor, currency)
    /** "+$5.00" / "-$2.50" style label. */
    val signedLabel: String get() = (if (isCredit) "+" else "-") + Money(kotlin.math.abs(amountMinor), currency).formatted
}

@Serializable
data class BillingConfig(
    val stripe: GatewayState = GatewayState(),
    val paypal: GatewayState = GatewayState(),
)

@Serializable
data class GatewayState(
    val configured: Boolean = false,
    val publishableKey: String? = null,
)

@Serializable
data class PayInvoiceResult(
    val paid: Boolean = false,
    val checkoutUrl: String? = null,
    val reason: String? = null,
)

@Serializable
data class PayPalCaptureResult(
    val paid: Boolean = false,
)
