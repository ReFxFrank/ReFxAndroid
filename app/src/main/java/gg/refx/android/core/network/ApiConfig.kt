package gg.refx.android.core.network

/**
 * Resolved origins for the app. Origins are overridable at runtime from Settings
 * (persisted in DataStore) — these are non-secret origins, never tokens (§3.1).
 */
data class ApiConfig(
    val apiOrigin: String,
    val webOrigin: String,
) {
    /** REST base = origin + `/api/v1`. */
    val restBaseUrl: String get() = apiOrigin.trimEnd('/') + "/api/v1/"

    /** Socket origin == API origin; live console namespace path `/ws/console`. */
    val socketOrigin: String get() = apiOrigin.trimEnd('/')
    val consoleNamespace: String get() = "/ws/console"

    fun webUrl(path: String): String = webOrigin.trimEnd('/') + "/" + path.trimStart('/')
}
