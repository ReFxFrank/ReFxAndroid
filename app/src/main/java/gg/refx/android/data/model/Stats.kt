package gg.refx.android.data.model

import kotlinx.serialization.Serializable

/**
 * Live resource stats from `GET servers/{id}/stats` (parity spec §3, `Stats.swift`).
 */
@Serializable
data class LiveStats(
    val state: ServerState? = null,
    val cpuPct: Double = 0.0,
    val memUsedMb: Double = 0.0,
    val memTotalMb: Double? = null,
    val diskUsedMb: Double = 0.0,
    val netRxBytes: Double = 0.0,
    val netTxBytes: Double = 0.0,
    val players: Int? = null,
    val uptimeMs: Double? = null,
)

/**
 * Stats pushed over the console socket `stats` event (no `memTotalMb`).
 * Merged into [LiveStats] by the detail view-model.
 */
@Serializable
data class StatsFrame(
    val serverId: String? = null,
    val cpuPct: Double = 0.0,
    val memUsedMb: Double = 0.0,
    val diskUsedMb: Double = 0.0,
    val netRxBytes: Double = 0.0,
    val netTxBytes: Double = 0.0,
    val state: ServerState? = null,
    val players: Int? = null,
)
