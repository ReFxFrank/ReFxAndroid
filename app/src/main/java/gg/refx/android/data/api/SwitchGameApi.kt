package gg.refx.android.data.api

import gg.refx.android.data.model.SwitchGameRequest
import gg.refx.android.data.model.SwitchGameTemplate
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Switch-game (parity spec §5, `SwitchGameService.swift`). */
interface SwitchGameApi {

    @GET("servers/{id}/switch-game/templates")
    suspend fun templates(@Path("id") id: String): List<SwitchGameTemplate>

    @POST("servers/{id}/switch-game")
    suspend fun switch(@Path("id") id: String, @Body body: SwitchGameRequest)
}
