package gg.refx.android.data.api

import gg.refx.android.data.model.CancelPlanChangeResult
import gg.refx.android.data.model.PlanChangeResult
import gg.refx.android.data.model.UpgradeOptions
import gg.refx.android.data.model.UpgradePreview
import gg.refx.android.data.model.UpgradeServerDTO
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Server upgrade / resize (parity spec §5, `ServersServiceUpgrade.swift`). */
interface UpgradeApi {

    @GET("servers/{id}/upgrade/options")
    suspend fun options(@Path("id") id: String): UpgradeOptions

    @POST("servers/{id}/upgrade/preview")
    suspend fun preview(@Path("id") id: String, @Body body: UpgradeServerDTO): UpgradePreview

    @POST("servers/{id}/upgrade")
    suspend fun apply(@Path("id") id: String, @Body body: UpgradeServerDTO): PlanChangeResult

    @DELETE("servers/{id}/upgrade")
    suspend fun cancelScheduled(@Path("id") id: String): CancelPlanChangeResult
}
