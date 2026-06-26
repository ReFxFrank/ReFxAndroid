package gg.refx.android.data.model

import kotlinx.serialization.Serializable

/** Body for `POST account/push-tokens` (§3.5, §7). */
@Serializable
data class PushTokenRequest(
    val token: String,
    val platform: String = "android",
)
