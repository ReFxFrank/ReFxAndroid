package gg.refx.android.data.model

import kotlinx.serialization.Serializable

/**
 * Startup configuration (parity spec §3/§5, `ServerSettings.swift`). Variable PATCH
 * shape ({envName, value}) should be reconciled against the panel DTO.
 */
@Serializable
data class StartupConfig(
    val command: String? = null,
    val rawStartup: String? = null,
    val dockerImage: String? = null,
    val variables: List<ServerVariable> = emptyList(),
)

@Serializable
data class ServerVariable(
    val envName: String,
    val displayName: String? = null,
    val description: String? = null,
    val value: String? = null,
    val defaultValue: String? = null,
    val userEditable: Boolean = true,
    val rules: String? = null,
) {
    val label: String get() = displayName?.takeIf { it.isNotBlank() } ?: envName
}

@Serializable
data class UpdateVariableRequest(
    val envName: String,
    val value: String,
)
