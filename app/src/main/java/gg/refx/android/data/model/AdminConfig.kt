package gg.refx.android.data.model

import gg.refx.android.core.network.InstantIso8601Serializer
import gg.refx.android.core.network.Money
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Admin catalog/config models (parity spec §3, `AdminConfigModels.swift`). Grouped
 * together to mirror the iOS file. Permissive decoding throughout.
 */

@Serializable
data class Region(
    val id: String,
    val code: String,
    val name: String,
    val country: String? = null,
)

@Serializable
data class Coupon(
    val id: String,
    val code: String,
    val description: String? = null,
    val kind: CouponKind = CouponKind.UNKNOWN,
    val value: Int = 0,
    val currency: String = "USD",
    val minSubtotalMinor: Int? = null,
    val maxRedemptions: Int? = null,
    val timesRedeemed: Int = 0,
    val maxPerUser: Int? = null,
    @Serializable(with = InstantIso8601Serializer::class) val startsAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val expiresAt: Instant? = null,
    val isActive: Boolean = true,
    @SerialName("_count") val count: CouponCount? = null,
) {
    /** "25%" for percent, formatted money for fixed. */
    val valueLabel: String
        get() = when (kind) {
            CouponKind.PERCENT -> "$value%"
            CouponKind.FIXED -> Money(value.toLong(), currency).formatted
            CouponKind.UNKNOWN -> value.toString()
        }
}

@Serializable
data class CouponCount(val redemptions: Int = 0)

@Serializable
data class GiftCard(
    val id: String,
    val code: String,
    val initialBalanceMinor: Long = 0,
    val balanceMinor: Long = 0,
    val currency: String = "USD",
    val isActive: Boolean = true,
    @Serializable(with = InstantIso8601Serializer::class) val expiresAt: Instant? = null,
    val note: String? = null,
) {
    val balance: Money get() = Money(balanceMinor, currency)
    val initialBalance: Money get() = Money(initialBalanceMinor, currency)
}

@Serializable
data class Role(
    val id: String,
    val key: String,
    val name: String,
    val description: String? = null,
    val isSystem: Boolean = false,
    val permissions: List<String> = emptyList(),
    @SerialName("_count") val count: RoleCount? = null,
)

@Serializable
data class RoleCount(val users: Int = 0)

@Serializable
data class AdminPermissionCatalog(
    val wildcard: String = "*",
    val permissions: List<String> = emptyList(),
)

@Serializable
data class GameCategory(
    val id: String,
    val name: String,
    val slug: String,
    val iconUrl: String? = null,
)

@Serializable
data class AdminGameTemplate(
    val id: String,
    val categoryId: String? = null,
    val category: GameCategory? = null,
    val name: String,
    val slug: String,
    val author: String = "",
    val description: String? = null,
    val version: Int = 1,
    val deployMethods: List<DeployMethod> = emptyList(),
    val supportsLinux: Boolean = true,
    val supportsWindows: Boolean = false,
    val steamAppId: Int? = null,
    val startupCommand: String = "",
    val recCpuCores: Double = 0.0,
    val recMemoryMb: Int = 0,
    val recDiskMb: Int = 0,
    val isPublished: Boolean = false,
    val featured: Boolean = false,
    val sortOrder: Int = 0,
    val tags: List<String>? = null,
    val variables: List<TemplateVariable>? = null,
)

@Serializable
data class TemplateVariable(
    val id: String,
    val templateId: String? = null,
    val envName: String,
    val displayName: String,
    val description: String? = null,
    val type: VariableType = VariableType.UNKNOWN,
    val defaultValue: String? = null,
    val userEditable: Boolean = true,
    val userViewable: Boolean = true,
    val sortOrder: Int = 0,
)
