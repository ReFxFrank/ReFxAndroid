package gg.refx.android.data.api

import gg.refx.android.data.model.CreateDatabaseRequest
import gg.refx.android.data.model.DatabasePassword
import gg.refx.android.data.model.ServerDatabase
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Server databases (parity spec §5, `DatabasesService.swift`). */
interface DatabasesApi {

    @GET("servers/{id}/databases")
    suspend fun list(@Path("id") id: String): List<ServerDatabase>

    @POST("servers/{id}/databases")
    suspend fun create(@Path("id") id: String, @Body body: CreateDatabaseRequest): ServerDatabase

    @DELETE("servers/{id}/databases/{dbId}")
    suspend fun delete(@Path("id") id: String, @Path("dbId") dbId: String)

    @POST("servers/{id}/databases/{dbId}/rotate")
    suspend fun rotate(@Path("id") id: String, @Path("dbId") dbId: String): DatabasePassword
}
