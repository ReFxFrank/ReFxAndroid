package gg.refx.android.data.api

import gg.refx.android.data.model.LoginRequest
import gg.refx.android.data.model.LoginResponse
import gg.refx.android.data.model.TwoFactorVerifyRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Auth endpoints (§3.5). Responses are envelope-unwrapped to the declared type.
 * The bearer is added automatically only when a token exists, so unauthenticated
 * login needs no special handling.
 */
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/2fa/verify")
    suspend fun verifyTotp(@Body body: TwoFactorVerifyRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout()
}
