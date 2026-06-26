package gg.refx.android.data.repo

import gg.refx.android.core.network.apiCall
import gg.refx.android.data.api.SchedulesApi
import gg.refx.android.data.api.ServerSettingsApi
import gg.refx.android.data.api.SubUsersApi
import gg.refx.android.data.model.CreateScheduleRequest
import gg.refx.android.data.model.CreateSubUserRequest
import gg.refx.android.data.model.Schedule
import gg.refx.android.data.model.ServerVariable
import gg.refx.android.data.model.StartupConfig
import gg.refx.android.data.model.SubUser
import gg.refx.android.data.model.UpdateScheduleRequest
import gg.refx.android.data.model.UpdateSubUserRequest
import gg.refx.android.data.model.UpdateVariableRequest

class SubUsersRepository(private val apiProvider: () -> SubUsersApi) {
    suspend fun list(id: String): List<SubUser> = apiCall { apiProvider().list(id) }
    suspend fun create(id: String, email: String, permissions: List<String>) =
        apiCall { apiProvider().create(id, CreateSubUserRequest(email.trim(), permissions)) }
    suspend fun update(id: String, suid: String, permissions: List<String>) =
        apiCall { apiProvider().update(id, suid, UpdateSubUserRequest(permissions)) }
    suspend fun delete(id: String, suid: String) = apiCall { apiProvider().delete(id, suid) }
}

class SchedulesRepository(private val apiProvider: () -> SchedulesApi) {
    suspend fun list(id: String): List<Schedule> = apiCall { apiProvider().list(id) }
    suspend fun create(id: String, body: CreateScheduleRequest) = apiCall { apiProvider().create(id, body) }
    suspend fun setActive(id: String, sid: String, active: Boolean) =
        apiCall { apiProvider().update(id, sid, UpdateScheduleRequest(active)) }
    suspend fun run(id: String, sid: String) = apiCall { apiProvider().run(id, sid) }
    suspend fun delete(id: String, sid: String) = apiCall { apiProvider().delete(id, sid) }
}

class ServerSettingsRepository(private val apiProvider: () -> ServerSettingsApi) {
    suspend fun startup(id: String): StartupConfig = apiCall { apiProvider().startup(id) }
    suspend fun variables(id: String): List<ServerVariable> = apiCall { apiProvider().variables(id) }
    suspend fun updateVariable(id: String, envName: String, value: String) =
        apiCall { apiProvider().updateVariable(id, UpdateVariableRequest(envName, value)) }
    suspend fun reinstall(id: String) = apiCall { apiProvider().reinstall(id) }
}
