package gg.refx.android.data.model

import gg.refx.android.core.network.Money
import kotlinx.serialization.Serializable

/**
 * The current user + order profile from `GET account` (§3.5): identity, role,
 * email-verified flag, billing address, and store-credit balance.
 *
 * NOTE: optional fields mirror the iOS struct's optionality; unknown JSON keys are
 * ignored. Reconcile exact field names against the panel `account` DTO.
 */
@Serializable
data class Account(
    val id: String,
    val email: String,
    val name: String? = null,
    val role: UserRole = UserRole.USER,
    val emailVerified: Boolean = false,
    // Store credit as integer minor units + ISO currency.
    val creditBalanceMinor: Long? = null,
    val creditCurrency: String? = null,
    val billingAddress: BillingAddress? = null,
) {
    val creditBalance: Money?
        get() = Money.of(creditBalanceMinor, creditCurrency)

    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: email
}

@Serializable
data class BillingAddress(
    val line1: String? = null,
    val line2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
)
