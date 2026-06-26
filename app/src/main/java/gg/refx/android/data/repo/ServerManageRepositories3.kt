package gg.refx.android.data.repo

import gg.refx.android.core.network.apiCall
import gg.refx.android.data.api.SwitchGameApi
import gg.refx.android.data.api.UpgradeApi
import gg.refx.android.data.model.CancelPlanChangeResult
import gg.refx.android.data.model.PlanChangeResult
import gg.refx.android.data.model.SwitchGameRequest
import gg.refx.android.data.model.SwitchGameTemplate
import gg.refx.android.data.model.UpgradeOptions
import gg.refx.android.data.model.UpgradePreview
import gg.refx.android.data.model.UpgradeServerDTO

class UpgradeRepository(private val apiProvider: () -> UpgradeApi) {
    suspend fun options(id: String): UpgradeOptions = apiCall { apiProvider().options(id) }
    suspend fun preview(id: String, tierId: String): UpgradePreview =
        apiCall { apiProvider().preview(id, UpgradeServerDTO(tierId)) }
    suspend fun apply(id: String, tierId: String): PlanChangeResult =
        apiCall { apiProvider().apply(id, UpgradeServerDTO(tierId)) }
    suspend fun cancelScheduled(id: String): CancelPlanChangeResult =
        apiCall { apiProvider().cancelScheduled(id) }
}

class SwitchGameRepository(private val apiProvider: () -> SwitchGameApi) {
    suspend fun templates(id: String): List<SwitchGameTemplate> = apiCall { apiProvider().templates(id) }
    suspend fun switch(id: String, templateId: String, keepData: Boolean) =
        apiCall { apiProvider().switch(id, SwitchGameRequest(templateId, keepData)) }
}
