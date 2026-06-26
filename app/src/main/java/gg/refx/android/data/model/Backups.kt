package gg.refx.android.data.model

import gg.refx.android.core.network.InstantIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Server backup (parity spec §3, `Backup.swift`). Fields beyond the documented
 * [BackupState] are decoded permissively (optional) and should be reconciled with
 * the panel DTO.
 */
@Serializable
data class Backup(
    val id: String,
    val name: String? = null,
    val state: BackupState = BackupState.UNKNOWN,
    val bytes: Long? = null,
    val checksum: String? = null,
    val isLocked: Boolean = false,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val completedAt: Instant? = null,
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: id
    val sizeLabel: String?
        get() = bytes?.let { b ->
            when {
                b >= 1_073_741_824 -> "%.2f GB".format(b / 1_073_741_824.0)
                b >= 1_048_576 -> "%.1f MB".format(b / 1_048_576.0)
                b >= 1024 -> "%.0f KB".format(b / 1024.0)
                else -> "$b B"
            }
        }
}

@Serializable
data class CreateBackupRequest(val name: String)
