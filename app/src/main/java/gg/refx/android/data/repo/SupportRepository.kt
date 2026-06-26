package gg.refx.android.data.repo

import gg.refx.android.core.network.PaginatedResult
import gg.refx.android.core.network.apiCall
import gg.refx.android.core.network.toPaginatedResult
import gg.refx.android.data.api.SupportApi
import gg.refx.android.data.model.CreateTicketRequest
import gg.refx.android.data.model.Ticket
import gg.refx.android.data.model.TicketDetail
import gg.refx.android.data.model.TicketReplyRequest

class SupportRepository(private val apiProvider: () -> SupportApi) {

    suspend fun tickets(page: Int, pageSize: Int = 25, mine: Boolean? = null, state: String? = null): PaginatedResult<Ticket> =
        apiCall { apiProvider().tickets(page, pageSize, mine, state).toPaginatedResult() }

    suspend fun ticket(id: String): TicketDetail = apiCall { apiProvider().ticket(id) }

    suspend fun create(subject: String, body: String, priority: String? = null): Ticket =
        apiCall { apiProvider().create(CreateTicketRequest(subject.trim(), body.trim(), priority)) }

    suspend fun reply(id: String, body: String) =
        apiCall { apiProvider().reply(id, TicketReplyRequest(body.trim())) }
}
