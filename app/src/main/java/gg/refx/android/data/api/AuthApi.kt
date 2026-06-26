package gg.refx.android.data.api

import gg.refx.android.data.model.Account
import gg.refx.android.data.model.LoginRequest
import gg.refx.android.data.model.LoginResponse
import gg.refx.android.data.model.LogoutRequest
import gg.refx.android.data.model.MfaVerifyRequest
import gg.refx.android.data.model.TokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Auth endpoints (parity spec §5, `AuthAPI.swift`). The bearer is added
 * automatically only when a token exists, so login/verify need no special handling.
 */
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/mfa/verify")
    suspend fun verifyMfa(@Body body: MfaVerifyRequest): TokenResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequest)

    @GET("auth/me")
    suspend fun me(): Account
}
