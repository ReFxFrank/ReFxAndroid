package gg.refx.android.data.model

import gg.refx.android.core.network.InstantIso8601Serializer
import gg.refx.android.core.network.Money
import gg.refx.android.core.network.PermissiveEnumSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Server upgrade / resize models (parity spec §3/§5, `ServerUpgradeModels.swift`).
 * Field shapes are inferred from the endpoint contract and should be reconciled
 * against the panel DTO; decoded permissively.
 */
@Serializable
data class UpgradeOptions(
    val currentTierId: String? = null,
    val tiers: List<UpgradeTier> = emptyList(),
)

@Serializable
data class UpgradeTier(
    val id: String,
    val name: String,
    val cpuCores: Double = 0.0,
    val memoryMb: Int = 0,
    val diskMb: Int = 0,
    val priceMinor: Long? = null,
    val currency: String? = null,
) {
    val price: Money? get() = Money.of(priceMinor, currency)
    val specs: String get() = "${cpuCores} vCPU · ${memoryMb} MB RAM · ${diskMb} MB disk"
}

/** Request body for upgrade preview/apply. */
@Serializable
data class UpgradeServerDTO(
    val hardwareTierId: String,
)

@Serializable
data class UpgradePreview(
    val dueTodayMinor: Long = 0,
    val currency: String = "USD",
    val prorationMinor: Long? = null,
    val nextInvoiceMinor: Long? = null,
) {
    val dueToday: Money get() = Money(dueTodayMinor, currency)
}

@Serializable(with = PlanChangeStatus.Serializer::class)
enum class PlanChangeStatus(val raw: String) {
    APPLIED("applied"),
    SCHEDULED("scheduled"),
    INVOICED("invoiced"),
    UNKNOWN("unknown");

    internal object Serializer :
        PermissiveEnumSerializer<PlanChangeStatus>("PlanChangeStatus", entries.toTypedArray(), UNKNOWN, { it.raw })
}

@Serializable
data class PlanChangeResult(
    val status: PlanChangeStatus = PlanChangeStatus.UNKNOWN,
    val invoiceId: String? = null,
    @Serializable(with = InstantIso8601Serializer::class) val effectiveAt: Instant? = null,
)

@Serializable
data class CancelPlanChangeResult(
    val canceled: Boolean = true,
)
