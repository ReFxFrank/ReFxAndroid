package gg.refx.android.data.repo

import gg.refx.android.core.network.PaginatedResult
import gg.refx.android.core.network.apiCall
import gg.refx.android.core.network.toPaginatedResult
import gg.refx.android.data.api.BackupsApi
import gg.refx.android.data.api.DatabasesApi
import gg.refx.android.data.api.FilesApi
import gg.refx.android.data.model.Backup
import gg.refx.android.data.model.CreateBackupRequest
import gg.refx.android.data.model.CreateDatabaseRequest
import gg.refx.android.data.model.DatabasePassword
import gg.refx.android.data.model.DeletePathsRequest
import gg.refx.android.data.model.FileContent
import gg.refx.android.data.model.FileEntry
import gg.refx.android.data.model.MkdirRequest
import gg.refx.android.data.model.RenameRequest
import gg.refx.android.data.model.ServerDatabase
import gg.refx.android.data.model.WriteFileRequest

class FilesRepository(private val apiProvider: () -> FilesApi) {
    suspend fun list(id: String, path: String): List<FileEntry> = apiCall { apiProvider().list(id, path) }
    suspend fun read(id: String, path: String): FileContent = apiCall { apiProvider().contents(id, path) }
    suspend fun write(id: String, path: String, content: String) = apiCall { apiProvider().write(id, WriteFileRequest(path, content)) }
    suspend fun mkdir(id: String, path: String) = apiCall { apiProvider().mkdir(id, MkdirRequest(path)) }
    suspend fun rename(id: String, from: String, to: String) = apiCall { apiProvider().rename(id, RenameRequest(from, to)) }
    suspend fun delete(id: String, paths: List<String>) = apiCall { apiProvider().delete(id, DeletePathsRequest(paths)) }
    suspend fun downloadUrl(id: String, path: String): String = apiCall { apiProvider().downloadUrl(id, path).url }
}

class BackupsRepository(private val apiProvider: () -> BackupsApi) {
    suspend fun list(id: String, page: Int, pageSize: Int = 25): PaginatedResult<Backup> =
        apiCall { apiProvider().list(id, page, pageSize).toPaginatedResult() }
    suspend fun create(id: String, name: String) = apiCall { apiProvider().create(id, CreateBackupRequest(name.trim())) }
    suspend fun restore(id: String, backupId: String) = apiCall { apiProvider().restore(id, backupId) }
    suspend fun delete(id: String, backupId: String) = apiCall { apiProvider().delete(id, backupId) }
    suspend fun downloadUrl(id: String, backupId: String): String = apiCall { apiProvider().downloadUrl(id, backupId).url }
}

class DatabasesRepository(private val apiProvider: () -> DatabasesApi) {
    suspend fun list(id: String): List<ServerDatabase> = apiCall { apiProvider().list(id) }
    suspend fun create(id: String, engine: String, name: String, remoteAccess: String?): ServerDatabase =
        apiCall { apiProvider().create(id, CreateDatabaseRequest(engine, name.trim(), remoteAccess)) }
    suspend fun delete(id: String, dbId: String) = apiCall { apiProvider().delete(id, dbId) }
    suspend fun rotate(id: String, dbId: String): DatabasePassword = apiCall { apiProvider().rotate(id, dbId) }
}
