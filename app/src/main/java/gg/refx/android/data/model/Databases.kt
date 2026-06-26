package gg.refx.android.data.model

import kotlinx.serialization.Serializable

/** Server database (parity spec §3, `ServerDatabase.swift`). */
@Serializable
data class ServerDatabase(
    val id: String,
    val name: String? = null,
    val engine: DbEngine = DbEngine.UNKNOWN,
    val host: String? = null,
    val port: Int? = null,
    val username: String? = null,
    val remoteAccess: String? = null,
) {
    val displayName: String get() = name ?: id
    val connectionHost: String?
        get() = if (host != null && port != null) "$host:$port" else host
}

/** Returned by the rotate endpoint — the new password to show once. */
@Serializable
data class DatabasePassword(
    val password: String,
)

@Serializable
data class CreateDatabaseRequest(
    val engine: String,
    val name: String,
    val remoteAccess: String? = null,
)
