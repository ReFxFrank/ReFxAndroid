package gg.refx.android.data.api

import gg.refx.android.data.model.Account
import gg.refx.android.data.model.ApiKey
import gg.refx.android.data.model.AppNotification
import gg.refx.android.data.model.ChangePasswordRequest
import gg.refx.android.data.model.CreateApiKeyRequest
import gg.refx.android.data.model.CreatedApiKey
import gg.refx.android.data.model.PushTokenRequest
import gg.refx.android.data.model.RecoveryCodes
import gg.refx.android.data.model.TotpEnrollment
import gg.refx.android.data.model.TotpVerifyRequest
import gg.refx.android.data.model.UnreadCount
import gg.refx.android.data.model.UserSession
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** Account / security / notifications endpoints (parity spec §5). */
interface AccountApi {

    @GET("account")
    suspend fun getAccount(): Account

    @PATCH("account")
    suspend fun updateAccount(@Body body: Map<String, String>): Account

    // Notifications feed
    @GET("account/notifications")
    suspend fun notifications(): List<AppNotification>

    @GET("account/notifications/unread-count")
    suspend fun unreadCount(): UnreadCount

    @POST("account/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String)

    @POST("account/notifications/read-all")
    suspend fun markAllRead()

    // Sessions
    @GET("account/sessions")
    suspend fun sessions(): List<UserSession>

    @DELETE("account/sessions/{id}")
    suspend fun revokeSession(@Path("id") id: String)

    // Password
    @POST("account/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    // Push tokens (§7)
    @POST("account/push-tokens")
    suspend fun registerPushToken(@Body body: PushTokenRequest)

    @DELETE("account/push-tokens/{token}")
    suspend fun deletePushToken(@Path("token") token: String)

    // API keys
    @GET("account/api-keys")
    suspend fun apiKeys(): List<ApiKey>

    @POST("account/api-keys")
    suspend fun createApiKey(@Body body: CreateApiKeyRequest): CreatedApiKey

    @DELETE("account/api-keys/{id}")
    suspend fun revokeApiKey(@Path("id") id: String)

    // TOTP (2FA)
    @POST("auth/mfa/totp/enroll")
    suspend fun enrollTotp(): TotpEnrollment

    @POST("auth/mfa/totp/verify")
    suspend fun verifyTotp(@Body body: TotpVerifyRequest): RecoveryCodes

    @DELETE("auth/mfa/totp")
    suspend fun disableTotp()
}
