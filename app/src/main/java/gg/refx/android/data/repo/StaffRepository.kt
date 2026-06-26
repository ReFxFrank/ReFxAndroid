package gg.refx.android.data.repo

import gg.refx.android.core.network.PaginatedResult
import gg.refx.android.core.network.apiCall
import gg.refx.android.core.network.toPaginatedResult
import gg.refx.android.data.api.StaffApi
import gg.refx.android.data.model.AdminAlert
import gg.refx.android.data.model.AdminCreateServerBody
import gg.refx.android.data.model.AdminGameTemplate
import gg.refx.android.data.model.AdminMetrics
import gg.refx.android.data.model.AdminUser
import gg.refx.android.data.model.AdminUserDetail
import gg.refx.android.data.model.AuditEntry
import gg.refx.android.data.model.BillingSummary
import gg.refx.android.data.model.Coupon
import gg.refx.android.data.model.CreateAlertRequest
import gg.refx.android.data.model.CreateNodeBody
import gg.refx.android.data.model.CreateNodeResult
import gg.refx.android.data.model.CreditReason
import gg.refx.android.data.model.GiftCard
import gg.refx.android.data.model.GrantCreditBody
import gg.refx.android.data.model.NodeAdmin
import gg.refx.android.data.model.NodePing
import gg.refx.android.data.model.Region
import gg.refx.android.data.model.Role
import gg.refx.android.data.model.Server
import gg.refx.android.data.model.UpdateAlertRequest
import gg.refx.android.data.model.UpdateUserRoleRequest
import gg.refx.android.data.model.UpdateUserStateRequest

class StaffRepository(private val apiProvider: () -> StaffApi) {

    // Overview
    suspend fun metrics(): AdminMetrics = apiCall { apiProvider().metrics() }
    suspend fun billingSummary(): BillingSummary = apiCall { apiProvider().billingSummary() }

    // Servers
    suspend fun servers(page: Int, pageSize: Int = 25, query: String? = null): PaginatedResult<Server> =
        apiCall { apiProvider().servers(page, pageSize, query?.takeIf { it.isNotBlank() }).toPaginatedResult() }
    suspend fun createServer(body: AdminCreateServerBody): Server = apiCall { apiProvider().createServer(body) }
    suspend fun deleteServer(id: String) = apiCall { apiProvider().deleteServer(id) }

    // Users
    suspend fun users(page: Int, pageSize: Int = 25, query: String? = null): PaginatedResult<AdminUser> =
        apiCall { apiProvider().users(page, pageSize, query?.takeIf { it.isNotBlank() }).toPaginatedResult() }
    suspend fun user(id: String): AdminUserDetail = apiCall { apiProvider().user(id) }
    suspend fun setUserState(id: String, state: String) = apiCall { apiProvider().setUserState(id, UpdateUserStateRequest(state)) }
    suspend fun setUserRole(id: String, role: String) = apiCall { apiProvider().setUserRole(id, UpdateUserRoleRequest(role)) }
    suspend fun verifyEmail(id: String) = apiCall { apiProvider().verifyEmail(id) }
    suspend fun grantCredit(id: String, amountMinor: Long, reason: CreditReason?, note: String?) =
        apiCall { apiProvider().grantCredit(id, GrantCreditBody(amountMinor, reason, note)) }

    // Nodes
    suspend fun nodes(page: Int, pageSize: Int = 100): PaginatedResult<NodeAdmin> =
        apiCall { apiProvider().nodes(page, pageSize).toPaginatedResult() }
    suspend fun node(id: String): NodeAdmin = apiCall { apiProvider().node(id) }
    suspend fun createNode(body: CreateNodeBody): CreateNodeResult = apiCall { apiProvider().createNode(body) }
    suspend fun pingNode(id: String): NodePing = apiCall { apiProvider().pingNode(id) }
    suspend fun restartAgent(id: String) = apiCall { apiProvider().restartAgent(id) }
    suspend fun updateAgent(id: String) = apiCall { apiProvider().updateAgent(id) }
    suspend fun clearSteamCache(id: String) = apiCall { apiProvider().clearSteamCache(id) }

    // Catalog / config
    suspend fun locations(): List<Region> = apiCall { apiProvider().locations() }
    suspend fun templates(): List<AdminGameTemplate> = apiCall { apiProvider().templates() }
    suspend fun coupons(): List<Coupon> = apiCall { apiProvider().coupons() }
    suspend fun giftCards(): List<GiftCard> = apiCall { apiProvider().giftCards() }
    suspend fun roles(): List<Role> = apiCall { apiProvider().roles() }

    // Audit + alerts
    suspend fun auditLogs(page: Int, pageSize: Int = 40): PaginatedResult<AuditEntry> =
        apiCall { apiProvider().auditLogs(page, pageSize).toPaginatedResult() }
    suspend fun alerts(): List<AdminAlert> = apiCall { apiProvider().alerts() }
    suspend fun createAlert(severity: String, title: String, body: String, isActive: Boolean) =
        apiCall { apiProvider().createAlert(CreateAlertRequest(severity, title.trim(), body.trim(), isActive)) }
    suspend fun setAlertActive(id: String, active: Boolean) = apiCall { apiProvider().updateAlert(id, UpdateAlertRequest(active)) }
    suspend fun deleteAlert(id: String) = apiCall { apiProvider().deleteAlert(id) }
}
