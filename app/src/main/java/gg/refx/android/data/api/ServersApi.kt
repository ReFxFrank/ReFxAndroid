package gg.refx.android.data.api

import gg.refx.android.core.network.Page
import gg.refx.android.data.model.CommandRequest
import gg.refx.android.data.model.LiveStats
import gg.refx.android.data.model.PowerRequest
import gg.refx.android.data.model.Server
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Customer server endpoints (parity spec §5, `ServersService.swift`). */
interface ServersApi {

    @GET("servers")
    suspend fun list(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("q") q: String? = null,
    ): Page<Server>

    @GET("servers/{id}")
    suspend fun get(@Path("id") id: String): Server

    @POST("servers/{id}/power")
    suspend fun power(@Path("id") id: String, @Body body: PowerRequest)

    @POST("servers/{id}/command")
    suspend fun command(@Path("id") id: String, @Body body: CommandRequest)

    @GET("servers/{id}/stats")
    suspend fun stats(@Path("id") id: String): LiveStats
}
