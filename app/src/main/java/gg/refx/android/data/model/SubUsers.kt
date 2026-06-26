package gg.refx.android.data.model

import kotlinx.serialization.Serializable

/** Sub-user grant (parity spec §3, `SubUser.swift`). */
@Serializable
data class SubUser(
    val id: String,
    val state: String? = null,
    val permissions: List<String> = emptyList(),
    val user: SubUserAccount? = null,
) {
    val email: String get() = user?.email ?: id
}

@Serializable
data class SubUserAccount(
    val id: String,
    val email: String? = null,
)

@Serializable
data class CreateSubUserRequest(
    val email: String,
    val permissions: List<String>,
)

@Serializable
data class UpdateSubUserRequest(
    val permissions: List<String>,
)

/**
 * Canonical per-server permission strings (parity spec §4, `Permission.swift`),
 * grouped for the grant editor. The role wildcard is `"*"`.
 */
object ServerPermissionCatalog {
    data class Group(val title: String, val permissions: List<String>)

    val groups: List<Group> = listOf(
        Group("Control", listOf("control.start", "control.stop", "control.restart", "control.power", "control.reinstall", "control.resize")),
        Group("Console", listOf("console.read", "console.command")),
        Group("Files", listOf("files.read", "files.write", "files.archive", "files.delete", "files.sftp")),
        Group("Backups", listOf("backup.read", "backup.create", "backup.restore", "backup.download", "backup.delete")),
        Group("Allocations", listOf("allocation.read")),
        Group("Schedules", listOf("schedule.read", "schedule.create", "schedule.update", "schedule.delete")),
        Group("Sub-users", listOf("subuser.read")),
        Group("Startup", listOf("startup.update")),
        Group("Settings", listOf("settings.read", "settings.update")),
    )

    val all: List<String> get() = groups.flatMap { it.permissions }

    fun label(permission: String): String =
        permission.substringAfter('.').replace('_', ' ').replaceFirstChar { it.uppercase() }
}
