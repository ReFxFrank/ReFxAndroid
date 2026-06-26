package gg.refx.android.data.model

import gg.refx.android.core.network.InstantIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.Instant

/** Support ticket models (parity spec §3, `Ticket.swift`). */

@Serializable
data class Ticket(
    val id: String,
    val number: Int = 0,
    val subject: String,
    val state: TicketState = TicketState.UNKNOWN,
    val priority: TicketPriority = TicketPriority.UNKNOWN,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    @Serializable(with = InstantIso8601Serializer::class) val updatedAt: Instant? = null,
)

@Serializable
data class TicketDetail(
    val id: String,
    val number: Int = 0,
    val subject: String,
    val state: TicketState = TicketState.UNKNOWN,
    val priority: TicketPriority = TicketPriority.UNKNOWN,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    val messages: List<TicketMessage> = emptyList(),
)

@Serializable
data class TicketMessage(
    val id: String,
    val body: String,
    val isInternal: Boolean? = null,
    @Serializable(with = InstantIso8601Serializer::class) val createdAt: Instant? = null,
    val author: TicketAuthor? = null,
)

@Serializable
data class TicketAuthor(
    val id: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val globalRole: UserRole? = null,
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { email ?: "User" }
    val isStaff: Boolean get() = globalRole?.isStaff == true
}

@Serializable
data class CreateTicketRequest(
    val subject: String,
    val body: String,
    val priority: String? = null,
)

@Serializable
data class TicketReplyRequest(
    val body: String,
)
