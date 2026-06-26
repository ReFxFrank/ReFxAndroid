package gg.refx.android.data.api

import gg.refx.android.data.model.Account
import gg.refx.android.data.model.PushTokenRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** Account / current-user endpoints (§3.5). */
interface AccountApi {

    @GET("account")
    suspend fun getAccount(): Account

    @PATCH("account")
    suspend fun updateAccount(@Body body: Map<String, String>): Account

    @POST("account/push-tokens")
    suspend fun registerPushToken(@Body body: PushTokenRequest)

    @DELETE("account/push-tokens/{token}")
    suspend fun deletePushToken(@Path("token") token: String)
}
