package gg.refx.android.data.api

import gg.refx.android.data.model.CreateScheduleRequest
import gg.refx.android.data.model.Schedule
import gg.refx.android.data.model.UpdateScheduleRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** Server schedules (parity spec §5, `SchedulesService.swift`). */
interface SchedulesApi {

    @GET("servers/{id}/schedules")
    suspend fun list(@Path("id") id: String): List<Schedule>

    @POST("servers/{id}/schedules")
    suspend fun create(@Path("id") id: String, @Body body: CreateScheduleRequest)

    @PATCH("servers/{id}/schedules/{sid}")
    suspend fun update(@Path("id") id: String, @Path("sid") sid: String, @Body body: UpdateScheduleRequest)

    @POST("servers/{id}/schedules/{sid}/run")
    suspend fun run(@Path("id") id: String, @Path("sid") sid: String)

    @DELETE("servers/{id}/schedules/{sid}")
    suspend fun delete(@Path("id") id: String, @Path("sid") sid: String)
}
