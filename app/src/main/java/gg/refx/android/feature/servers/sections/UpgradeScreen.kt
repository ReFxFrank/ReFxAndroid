package gg.refx.android.feature.servers.sections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.RefxPrimaryButton
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.PlanChangeResult
import gg.refx.android.data.model.PlanChangeStatus
import gg.refx.android.data.model.UpgradeOptions
import gg.refx.android.data.model.UpgradePreview
import gg.refx.android.data.model.UpgradeTier
import gg.refx.android.data.repo.UpgradeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpgradeUiState(
    val options: LoadState<UpgradeOptions> = LoadState.Loading,
    val selectedTierId: String? = null,
    val preview: UpgradePreview? = null,
    val previewing: Boolean = false,
    val applying: Boolean = false,
    val result: PlanChangeResult? = null,
    val error: String? = null,
)

class UpgradeViewModel(private val serverId: String, private val repo: UpgradeRepository) : ViewModel() {
    private val _state = MutableStateFlow(UpgradeUiState())
    val state: StateFlow<UpgradeUiState> = _state.asStateFlow()

    private var previewJob: Job? = null

    init { load() }

    fun load() {
        if (_state.value.options.value == null) _state.update { it.copy(options = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.options(serverId) }
                .onSuccess { o -> _state.update { it.copy(options = LoadState.Loaded(o), selectedTierId = o.currentTierId) } }
                .onFailure { t -> _state.update { if (it.options.value == null) it.copy(options = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun selectTier(tierId: String) {
        previewJob?.cancel()
        _state.update { it.copy(selectedTierId = tierId, preview = null, previewing = true, error = null) }
        previewJob = viewModelScope.launch {
            runCatching { repo.preview(serverId, tierId) }
                // Ignore stale previews for a tier the user has since changed away from.
                .onSuccess { p -> _state.update { if (it.selectedTierId == tierId) it.copy(previewing = false, preview = p) else it } }
                .onFailure { t -> _state.update { if (it.selectedTierId == tierId) it.copy(previewing = false, error = t.toApiException().message) else it } }
        }
    }

    fun apply() {
        val tierId = _state.value.selectedTierId ?: return
        if (_state.value.applying) return
        _state.update { it.copy(applying = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.apply(serverId, tierId) }
                .onSuccess { r -> _state.update { it.copy(applying = false, result = r) } }
                .onFailure { t -> _state.update { it.copy(applying = false, error = t.toApiException().message) } }
        }
    }

    fun cancelScheduled() {
        if (_state.value.applying) return
        _state.update { it.copy(applying = true, error = null, result = null) }
        viewModelScope.launch {
            runCatching { repo.cancelScheduled(serverId) }
                .onSuccess { _state.update { it.copy(applying = false) }; load() }
                .onFailure { t -> _state.update { it.copy(applying = false, error = t.toApiException().message) } }
        }
    }

    fun dismissResult() = _state.update { it.copy(result = null) }
}

@Composable
fun UpgradeScreen(serverId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: UpgradeViewModel = viewModel(
        factory = viewModelFactory { initializer { UpgradeViewModel(serverId, container.upgradeRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Upgrade / resize", onBack = onBack)
        AsyncState(state = state.options, isEmpty = { it.tiers.isEmpty() }, emptyTitle = "No upgrade options", onRetry = vm::load) { options ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }
                options.tiers.forEach { tier ->
                    TierRow(
                        tier = tier,
                        selected = state.selectedTierId == tier.id,
                        isCurrent = options.currentTierId == tier.id,
                        onSelect = { vm.selectTier(tier.id) },
                    )
                }

                state.preview?.let { preview ->
                    PreviewCard(preview)
                }

                val canApply = state.selectedTierId != null &&
                    state.selectedTierId != options.currentTierId &&
                    !state.previewing && !state.applying
                RefxPrimaryButton(
                    text = "Confirm plan change",
                    onClick = vm::apply,
                    enabled = canApply,
                    loading = state.applying,
                    fullWidth = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    state.result?.let { result ->
        val (label, color) = when (result.status) {
            PlanChangeStatus.APPLIED -> "Applied" to DesignTokens.AppSuccess
            PlanChangeStatus.SCHEDULED -> "Scheduled" to DesignTokens.AppWarning
            PlanChangeStatus.INVOICED -> "Invoiced" to DesignTokens.AppAccentText
            PlanChangeStatus.UNKNOWN -> "Submitted" to DesignTokens.AppMuted
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = vm::dismissResult,
            title = { Text("Plan change") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(label, color)
                    Text(
                        when (result.status) {
                            PlanChangeStatus.APPLIED -> "Your new plan is active."
                            PlanChangeStatus.SCHEDULED -> "Your plan change is scheduled."
                            PlanChangeStatus.INVOICED -> "An invoice was created to complete the change."
                            PlanChangeStatus.UNKNOWN -> "Your request was submitted."
                        },
                        color = DesignTokens.AppMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { vm.dismissResult(); onBack() }) { Text("Done") } },
            dismissButton = {
                if (result.status == PlanChangeStatus.SCHEDULED) {
                    androidx.compose.material3.TextButton(onClick = vm::cancelScheduled) {
                        Text("Cancel scheduled change", color = DesignTokens.AppDestructive)
                    }
                }
            },
        )
    }
}

@Composable
private fun TierRow(tier: UpgradeTier, selected: Boolean, isCurrent: Boolean, onSelect: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(enabled = !isCurrent, onClick = onSelect)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tier.name, color = if (selected) DesignTokens.AppPrimary else DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                Text(tier.specs, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                tier.price?.let { Text("${it.formatted}", color = DesignTokens.AppAccentText, style = MaterialTheme.typography.bodySmall) }
            }
            if (isCurrent) StatusChip("Current", DesignTokens.AppSuccess)
            else if (selected) StatusChip("Selected", DesignTokens.AppPrimary)
        }
    }
}

@Composable
private fun PreviewCard(preview: UpgradePreview) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Due today", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodyMedium)
            Text(preview.dueToday.formatted, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
        }
    }
}
