package gg.refx.android.data.api

import gg.refx.android.core.network.Page
import gg.refx.android.data.model.CreateTicketRequest
import gg.refx.android.data.model.Ticket
import gg.refx.android.data.model.TicketDetail
import gg.refx.android.data.model.TicketReplyRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Support ticket endpoints (parity spec §5). */
interface SupportApi {

    @GET("support/tickets")
    suspend fun tickets(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("mine") mine: Boolean? = null,
        @Query("state") state: String? = null,
    ): Page<Ticket>

    @GET("support/tickets/{id}")
    suspend fun ticket(@Path("id") id: String): TicketDetail

    @POST("support/tickets")
    suspend fun create(@Body body: CreateTicketRequest): Ticket

    @POST("support/tickets/{id}/messages")
    suspend fun reply(@Path("id") id: String, @Body body: TicketReplyRequest)
}
