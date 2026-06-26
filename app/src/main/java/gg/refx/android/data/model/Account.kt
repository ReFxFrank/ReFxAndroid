package gg.refx.android.data.model

import gg.refx.android.core.network.Money
import kotlinx.serialization.Serializable

/**
 * Current user from `GET auth/me` (`CurrentUser`, parity spec §3). Identity, role,
 * store-credit balance, permissions and TOTP status.
 *
 * (The richer order profile from `GET account` — billing address, email-verified —
 * is a separate `OrderProfile` model added with the billing/checkout milestone.)
 */
@Serializable
data class Account(
    val id: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val globalRole: UserRole = UserRole.CUSTOMER,
    val avatarUrl: String? = null,
    val creditBalanceMinor: Long? = null,
    val permissions: List<String>? = null,
    // ISO-8601 timestamp string; non-null means TOTP is enabled.
    val totpEnabledAt: String? = null,
) {
    val displayName: String
        get() {
            val full = listOfNotNull(firstName, lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            return full.ifBlank { email }
        }

    val initials: String
        get() {
            val first = firstName?.firstOrNull()
            val last = lastName?.firstOrNull()
            return listOfNotNull(first, last).joinToString("").ifBlank {
                email.firstOrNull()?.uppercase() ?: "?"
            }
        }

    val isTotpEnabled: Boolean get() = totpEnabledAt != null

    /** Store credit (currency resolved by the billing milestone; defaults to USD). */
    fun creditBalance(currency: String = "USD"): Money? =
        Money.of(creditBalanceMinor, currency)
}
