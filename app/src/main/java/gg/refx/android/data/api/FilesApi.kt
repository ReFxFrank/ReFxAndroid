package gg.refx.android.data.api

import gg.refx.android.data.model.DeletePathsRequest
import gg.refx.android.data.model.FileContent
import gg.refx.android.data.model.FileEntry
import gg.refx.android.data.model.MkdirRequest
import gg.refx.android.data.model.RenameRequest
import gg.refx.android.data.model.SignedUrl
import gg.refx.android.data.model.WriteFileRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Server file management (parity spec §5, `FilesService.swift`). */
interface FilesApi {

    @GET("servers/{id}/files/list")
    suspend fun list(@Path("id") id: String, @Query("path") path: String): List<FileEntry>

    @GET("servers/{id}/files/contents")
    suspend fun contents(@Path("id") id: String, @Query("path") path: String): FileContent

    @POST("servers/{id}/files/write")
    suspend fun write(@Path("id") id: String, @Body body: WriteFileRequest)

    @POST("servers/{id}/files/mkdir")
    suspend fun mkdir(@Path("id") id: String, @Body body: MkdirRequest)

    @POST("servers/{id}/files/rename")
    suspend fun rename(@Path("id") id: String, @Body body: RenameRequest)

    @POST("servers/{id}/files/delete")
    suspend fun delete(@Path("id") id: String, @Body body: DeletePathsRequest)

    @GET("servers/{id}/files/download-url")
    suspend fun downloadUrl(@Path("id") id: String, @Query("path") path: String): SignedUrl
}
