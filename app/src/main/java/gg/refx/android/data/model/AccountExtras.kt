package gg.refx.android.data.model

import gg.refx.android.core.network.InstantIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Account security / notifications models (parity spec §3/§5). Some of these
 * (UserSession, ApiKey, TotpEnrollment) are not field-enumerated in the spec; the
 * shapes below are decoded permissively and should be reconciled against the panel
 * DTOs — unknown keys are ignored and all fields are optional where plausible.
 */

@Serializable
data class AppNotification(
    val id: String,
    val title: String,
    val body: String? = null,
    @Serializable(with = InstantIso8601Serializer::class) val readAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
) {
    val isRead: Boolean get() = readAt != null
}

@Serializable
data class UnreadCount(val unread: Int = 0)

@Serializable
data class UserSession(
    val id: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val current: Boolean = false,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val lastSeenAt: Instant? = null,
)

@Serializable
data class ApiKey(
    val id: String,
    val name: String,
    val scopes: List<String> = emptyList(),
    @Serializable(with = InstantIso8601Serializer::class) val lastUsedAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
)

/** Returned once on creation; carries the secret that is never retrievable again. */
@Serializable
data class CreatedApiKey(
    val id: String,
    val name: String,
    val key: String? = null,
    val secret: String? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
) {
    /** The one-time secret to copy (server may name it `key` or `secret`). */
    val plaintext: String? get() = secret ?: key
}

@Serializable
data class CreateApiKeyRequest(
    val name: String,
    val scopes: List<String> = emptyList(),
)

@Serializable
data class TotpEnrollment(
    val secret: String? = null,
    val otpauthUrl: String? = null,
    val qrCodeUrl: String? = null,
)

@Serializable
data class TotpVerifyRequest(val code: String)

@Serializable
data class RecoveryCodes(val codes: List<String> = emptyList())

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)
