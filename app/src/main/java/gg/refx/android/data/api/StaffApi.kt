package gg.refx.android.data.api

import gg.refx.android.core.network.Page
import gg.refx.android.data.model.AdminAlert
import gg.refx.android.data.model.AdminCreateServerBody
import gg.refx.android.data.model.AdminGameTemplate
import gg.refx.android.data.model.AdminMetrics
import gg.refx.android.data.model.AdminPermissionCatalog
import gg.refx.android.data.model.AdminUser
import gg.refx.android.data.model.AdminUserDetail
import gg.refx.android.data.model.AgentLatest
import gg.refx.android.data.model.AuditEntry
import gg.refx.android.data.model.BillingSummary
import gg.refx.android.data.model.Coupon
import gg.refx.android.data.model.CreateAlertRequest
import gg.refx.android.data.model.CreateNodeBody
import gg.refx.android.data.model.CreateNodeResult
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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Staff/Admin endpoints (parity spec §5; `StaffService.swift` + `StaffServiceConfig.swift`
 * are the checklist). Role + permission gated server-side.
 */
interface StaffApi {

    // Overview
    @GET("admin/metrics")
    suspend fun metrics(): AdminMetrics

    @GET("admin/billing/summary")
    suspend fun billingSummary(): BillingSummary

    // Servers
    @GET("admin/servers")
    suspend fun servers(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("q") q: String? = null,
    ): Page<Server>

    @POST("admin/servers")
    suspend fun createServer(@Body body: AdminCreateServerBody): Server

    @DELETE("admin/servers/{id}")
    suspend fun deleteServer(@Path("id") id: String)

    // Users
    @GET("admin/users")
    suspend fun users(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("q") q: String? = null,
    ): Page<AdminUser>

    @GET("admin/users/{id}")
    suspend fun user(@Path("id") id: String): AdminUserDetail

    @PATCH("admin/users/{id}")
    suspend fun setUserState(@Path("id") id: String, @Body body: UpdateUserStateRequest)

    @PATCH("admin/users/{id}/role")
    suspend fun setUserRole(@Path("id") id: String, @Body body: UpdateUserRoleRequest)

    @POST("admin/users/{id}/verify-email")
    suspend fun verifyEmail(@Path("id") id: String)

    @POST("admin/users/{id}/credit")
    suspend fun grantCredit(@Path("id") id: String, @Body body: GrantCreditBody)

    // Nodes
    @GET("admin/nodes")
    suspend fun nodes(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Page<NodeAdmin>

    @GET("admin/nodes/{id}")
    suspend fun node(@Path("id") id: String): NodeAdmin

    @POST("admin/nodes")
    suspend fun createNode(@Body body: CreateNodeBody): CreateNodeResult

    @GET("admin/nodes/{id}/ping")
    suspend fun pingNode(@Path("id") id: String): NodePing

    @POST("admin/nodes/{id}/restart-agent")
    suspend fun restartAgent(@Path("id") id: String)

    @POST("admin/nodes/{id}/update-agent")
    suspend fun updateAgent(@Path("id") id: String)

    @POST("admin/nodes/{id}/steam-cache/clear")
    suspend fun clearSteamCache(@Path("id") id: String)

    @GET("admin/nodes/agent-latest")
    suspend fun agentLatest(): AgentLatest

    // Catalog / config (read-heavy)
    @GET("admin/locations")
    suspend fun locations(): List<Region>

    @GET("admin/templates")
    suspend fun templates(): List<AdminGameTemplate>

    @GET("admin/coupons")
    suspend fun coupons(): List<Coupon>

    @GET("admin/gift-cards")
    suspend fun giftCards(): List<GiftCard>

    @GET("admin/roles")
    suspend fun roles(): List<Role>

    @GET("admin/roles/permissions")
    suspend fun permissionCatalog(): AdminPermissionCatalog

    // Audit + alerts
    @GET("admin/audit-logs")
    suspend fun auditLogs(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Page<AuditEntry>

    @GET("admin/alerts")
    suspend fun alerts(): List<AdminAlert>

    @POST("admin/alerts")
    suspend fun createAlert(@Body body: CreateAlertRequest)

    @PATCH("admin/alerts/{id}")
    suspend fun updateAlert(@Path("id") id: String, @Body body: UpdateAlertRequest)

    @DELETE("admin/alerts/{id}")
    suspend fun deleteAlert(@Path("id") id: String)
}
