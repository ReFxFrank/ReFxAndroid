package gg.refx.android.data.api

import gg.refx.android.data.model.ServerVariable
import gg.refx.android.data.model.StartupConfig
import gg.refx.android.data.model.UpdateVariableRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** Server settings: startup config, variables, reinstall (parity spec §5). */
interface ServerSettingsApi {

    @GET("servers/{id}/startup")
    suspend fun startup(@Path("id") id: String): StartupConfig

    @GET("servers/{id}/variables")
    suspend fun variables(@Path("id") id: String): List<ServerVariable>

    @PATCH("servers/{id}/variables")
    suspend fun updateVariable(@Path("id") id: String, @Body body: UpdateVariableRequest)

    @POST("servers/{id}/reinstall")
    suspend fun reinstall(@Path("id") id: String)
}
