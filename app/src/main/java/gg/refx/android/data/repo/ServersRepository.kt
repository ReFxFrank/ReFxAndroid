package gg.refx.android.data.repo

import gg.refx.android.core.network.PaginatedResult
import gg.refx.android.core.network.apiCall
import gg.refx.android.core.network.toPaginatedResult
import gg.refx.android.data.api.ServersApi
import gg.refx.android.data.model.CommandRequest
import gg.refx.android.data.model.LiveStats
import gg.refx.android.data.model.PowerRequest
import gg.refx.android.data.model.PowerSignal
import gg.refx.android.data.model.Server

/** Customer server domain. API supplied via provider (survives origin changes). */
class ServersRepository(
    private val apiProvider: () -> ServersApi,
) {
    suspend fun list(page: Int, pageSize: Int = 25, query: String? = null): PaginatedResult<Server> =
        apiCall { apiProvider().list(page, pageSize, query?.takeIf { it.isNotBlank() }).toPaginatedResult() }

    suspend fun get(id: String): Server = apiCall { apiProvider().get(id) }

    suspend fun power(id: String, signal: PowerSignal) =
        apiCall { apiProvider().power(id, PowerRequest(signal.raw)) }

    suspend fun command(id: String, command: String) =
        apiCall { apiProvider().command(id, CommandRequest(command)) }

    suspend fun stats(id: String): LiveStats = apiCall { apiProvider().stats(id) }
}
