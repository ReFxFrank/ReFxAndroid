package gg.refx.android.data.api

import gg.refx.android.core.network.Page
import gg.refx.android.data.model.Backup
import gg.refx.android.data.model.CreateBackupRequest
import gg.refx.android.data.model.SignedUrl
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Server backups (parity spec §5, `BackupsService.swift`). */
interface BackupsApi {

    @GET("servers/{id}/backups")
    suspend fun list(
        @Path("id") id: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Page<Backup>

    @POST("servers/{id}/backups")
    suspend fun create(@Path("id") id: String, @Body body: CreateBackupRequest)

    @POST("servers/{id}/backups/{backupId}/restore")
    suspend fun restore(@Path("id") id: String, @Path("backupId") backupId: String)

    @DELETE("servers/{id}/backups/{backupId}")
    suspend fun delete(@Path("id") id: String, @Path("backupId") backupId: String)

    @GET("servers/{id}/backups/{backupId}/download")
    suspend fun downloadUrl(@Path("id") id: String, @Path("backupId") backupId: String): SignedUrl
}
