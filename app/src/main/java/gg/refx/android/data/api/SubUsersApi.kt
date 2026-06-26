package gg.refx.android.data.api

import gg.refx.android.data.model.CreateSubUserRequest
import gg.refx.android.data.model.SubUser
import gg.refx.android.data.model.UpdateSubUserRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** Server sub-users (parity spec §5, `SubUsersService.swift`). */
interface SubUsersApi {

    @GET("servers/{id}/sub-users")
    suspend fun list(@Path("id") id: String): List<SubUser>

    @POST("servers/{id}/sub-users")
    suspend fun create(@Path("id") id: String, @Body body: CreateSubUserRequest)

    @PATCH("servers/{id}/sub-users/{suid}")
    suspend fun update(@Path("id") id: String, @Path("suid") suid: String, @Body body: UpdateSubUserRequest)

    @DELETE("servers/{id}/sub-users/{suid}")
    suspend fun delete(@Path("id") id: String, @Path("suid") suid: String)
}
