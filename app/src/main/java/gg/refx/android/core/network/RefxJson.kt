package gg.refx.android.core.network

import kotlinx.serialization.json.Json

/**
 * The single JSON configuration used across the app.
 *
 * - `ignoreUnknownKeys` — backend additions never crash decoding (§4).
 * - `coerceInputValues` — null/absent for non-null fields fall back to defaults.
 * - `explicitNulls = false` — don't emit nulls we don't set.
 * - `isLenient` — tolerate minor server quirks.
 *
 * Field names are used verbatim (camelCase, no snake_case conversion) — §4.
 */
val RefxJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    isLenient = true
    encodeDefaults = true
}
