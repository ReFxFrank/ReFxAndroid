package gg.refx.android.data.repo

import gg.refx.android.core.network.apiCall
import gg.refx.android.data.api.AccountApi
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

class AccountRepository(private val apiProvider: () -> AccountApi) {

    // Notifications
    suspend fun notifications(): List<AppNotification> = apiCall { apiProvider().notifications() }
    suspend fun unreadCount(): UnreadCount = apiCall { apiProvider().unreadCount() }
    suspend fun markRead(id: String) = apiCall { apiProvider().markRead(id) }
    suspend fun markAllRead() = apiCall { apiProvider().markAllRead() }

    // Sessions
    suspend fun sessions(): List<UserSession> = apiCall { apiProvider().sessions() }
    suspend fun revokeSession(id: String) = apiCall { apiProvider().revokeSession(id) }

    // Password
    suspend fun changePassword(current: String, new: String) =
        apiCall { apiProvider().changePassword(ChangePasswordRequest(current, new)) }

    // API keys
    suspend fun apiKeys(): List<ApiKey> = apiCall { apiProvider().apiKeys() }
    suspend fun createApiKey(name: String, scopes: List<String>): CreatedApiKey =
        apiCall { apiProvider().createApiKey(CreateApiKeyRequest(name.trim(), scopes)) }
    suspend fun revokeApiKey(id: String) = apiCall { apiProvider().revokeApiKey(id) }

    // TOTP (2FA)
    suspend fun enrollTotp(): TotpEnrollment = apiCall { apiProvider().enrollTotp() }
    suspend fun verifyTotp(code: String): RecoveryCodes =
        apiCall { apiProvider().verifyTotp(TotpVerifyRequest(code.trim())) }
    suspend fun disableTotp() = apiCall { apiProvider().disableTotp() }

    // Push tokens (§7) — best-effort; callers ignore failures on sign-out.
    suspend fun registerPushToken(token: String) =
        apiProvider().registerPushToken(PushTokenRequest(token = token, platform = "android"))
    suspend fun unregisterPushToken(token: String) =
        apiProvider().deletePushToken(token)
}
