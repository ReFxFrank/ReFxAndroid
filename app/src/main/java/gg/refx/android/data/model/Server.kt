package gg.refx.android.data.model

import gg.refx.android.core.network.InstantIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * A customer server (parity spec §3, `Server.swift`). Optionals mirror the iOS
 * struct; unknown JSON keys are ignored.
 */
@Serializable
data class Server(
    val id: String,
    val shortId: String,
    val name: String,
    val description: String? = null,
    val state: ServerState = ServerState.UNKNOWN,
    val cpuCores: Double? = null,
    val memoryMb: Int? = null,
    val diskMb: Int? = null,
    val slots: Int? = null,
    @Serializable(with = InstantIso8601Serializer::class)
    val suspendedAt: Instant? = null,
    val template: GameTemplateRef? = null,
    val node: NodeRef? = null,
    val primaryAllocation: Allocation? = null,
) {
    val gameName: String get() = template?.name ?: template?.slug ?: "Server"
    val connectionString: String? get() = primaryAllocation?.connectionString
    val isSuspended: Boolean get() = state == ServerState.SUSPENDED || suspendedAt != null
}

@Serializable
data class GameTemplateRef(
    val id: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val supportsWorkshop: Boolean? = null,
)

@Serializable
data class NodeRef(
    val name: String? = null,
    val fqdn: String? = null,
)

@Serializable
data class Allocation(
    val id: String,
    val ip: String,
    val port: Int,
    val alias: String? = null,
    val isPrimary: Boolean = false,
) {
    /** `alias ?? ip` + `:port` (iOS `Allocation.connectionString`). */
    val connectionString: String get() = "${alias ?: ip}:$port"
}

/** Body for `POST servers/{id}/power` — key is `signal`, lowercase value. */
@Serializable
data class PowerRequest(val signal: String)

/** Body for `POST servers/{id}/command`. */
@Serializable
data class CommandRequest(val command: String)
