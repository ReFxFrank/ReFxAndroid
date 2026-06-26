package gg.refx.android.feature.servers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.ui.graphics.vector.ImageVector
import gg.refx.android.data.model.Server

/**
 * Server-detail sections (parity spec §8, iOS `ServerSection`). [isApplicable]
 * gates game-conditional sections; [webPath] is the panel route for web link-outs
 * (note `switchGame` → "switch-game").
 */
enum class ServerSection(
    val label: String,
    val icon: ImageVector,
    val webPath: String,
) {
    CONSOLE("Console", Icons.Outlined.Terminal, "console"),
    FILES("Files", Icons.Outlined.Folder, "files"),
    DATABASES("Databases", Icons.Outlined.Storage, "databases"),
    BACKUPS("Backups", Icons.Outlined.Backup, "backups"),
    SCHEDULES("Schedules", Icons.Outlined.Schedule, "schedules"),
    MINECRAFT("Minecraft", Icons.Outlined.VideogameAsset, "minecraft"),
    MODS("Mods", Icons.Outlined.Extension, "mods"),
    MODPACKS("Modpacks", Icons.Outlined.Inventory2, "modpacks"),
    WORKSHOP("Workshop", Icons.Outlined.Build, "workshop"),
    VOICE("Voice", Icons.Outlined.Mic, "voice"),
    SWITCH_GAME("Switch game", Icons.Outlined.SwapHoriz, "switch-game"),
    UPGRADE("Upgrade", Icons.Outlined.Upgrade, "upgrade"),
    SETTINGS("Settings", Icons.Outlined.Settings, "settings");

    /** Sections with a native screen; the rest deep-link to the web panel. */
    val isNative: Boolean
        get() = this in setOf(CONSOLE, FILES, BACKUPS, DATABASES)

    fun isApplicable(server: Server): Boolean {
        val slug = server.template?.slug?.lowercase().orEmpty()
        val isVoice = slug.contains("teamspeak")
        val isMinecraft = slug.contains("minecraft")
        return when (this) {
            MINECRAFT, MODS, MODPACKS -> isMinecraft
            WORKSHOP -> server.template?.supportsWorkshop == true
            VOICE -> isVoice
            CONSOLE, SWITCH_GAME -> !isVoice
            else -> true
        }
    }

    companion object {
        fun applicableFor(server: Server): List<ServerSection> =
            entries.filter { it.isApplicable(server) }
    }
}
