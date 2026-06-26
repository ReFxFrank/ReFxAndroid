package gg.refx.android.data.model

import gg.refx.android.core.network.InstantIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.Instant

/** Server schedule + tasks (parity spec §3/§5, `Schedule.swift`). */
@Serializable
data class Schedule(
    val id: String,
    val name: String,
    val cron: String? = null,
    val onlyWhenOnline: Boolean = false,
    val isActive: Boolean = true,
    val tasks: List<ScheduleTask> = emptyList(),
    @Serializable(with = InstantIso8601Serializer::class) val nextRunAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val lastRunAt: Instant? = null,
)

@Serializable
data class ScheduleTask(
    val id: String? = null,
    val action: ScheduleAction = ScheduleAction.UNKNOWN,
    val payload: String? = null,
    val timeOffset: Int? = null,
)

@Serializable
data class ScheduleTaskRequest(
    val action: String,
    val payload: String? = null,
)

@Serializable
data class CreateScheduleRequest(
    val name: String,
    val cron: String,
    val onlyWhenOnline: Boolean = false,
    val isActive: Boolean = true,
    val tasks: List<ScheduleTaskRequest> = emptyList(),
)

@Serializable
data class UpdateScheduleRequest(
    val isActive: Boolean,
)
