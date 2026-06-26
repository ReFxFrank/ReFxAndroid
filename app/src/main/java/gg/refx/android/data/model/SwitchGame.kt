package gg.refx.android.data.model

import kotlinx.serialization.Serializable

/** Switch-game template option (parity spec §5, `SwitchGameService.swift`). */
@Serializable
data class SwitchGameTemplate(
    val id: String,
    val name: String,
    val slug: String? = null,
    val author: String? = null,
    val description: String? = null,
)

@Serializable
data class SwitchGameRequest(
    val templateId: String,
    val keepData: Boolean = false,
)
