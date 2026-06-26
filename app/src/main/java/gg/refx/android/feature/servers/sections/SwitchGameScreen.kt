package gg.refx.android.feature.servers.sections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.RefxDestructiveButton
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.SwitchGameTemplate
import gg.refx.android.data.repo.SwitchGameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SwitchGameUiState(
    val templates: LoadState<List<SwitchGameTemplate>> = LoadState.Loading,
    val selectedId: String? = null,
    val keepData: Boolean = true,
    val switching: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

class SwitchGameViewModel(private val serverId: String, private val repo: SwitchGameRepository) : ViewModel() {
    private val _state = MutableStateFlow(SwitchGameUiState())
    val state: StateFlow<SwitchGameUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.templates.value == null) _state.update { it.copy(templates = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.templates(serverId) }
                .onSuccess { t -> _state.update { it.copy(templates = LoadState.Loaded(t)) } }
                .onFailure { e -> _state.update { if (it.templates.value == null) it.copy(templates = LoadState.Failed(e.toApiException().message)) else it } }
        }
    }

    fun select(id: String) = _state.update { it.copy(selectedId = id) }
    fun setKeepData(keep: Boolean) = _state.update { it.copy(keepData = keep) }

    fun switch() {
        val id = _state.value.selectedId ?: return
        if (_state.value.switching) return
        _state.update { it.copy(switching = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.switch(serverId, id, _state.value.keepData) }
                .onSuccess { _state.update { it.copy(switching = false, done = true) } }
                .onFailure { t -> _state.update { it.copy(switching = false, error = t.toApiException().message) } }
        }
    }
}

@Composable
fun SwitchGameScreen(serverId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SwitchGameViewModel = viewModel(
        factory = viewModelFactory { initializer { SwitchGameViewModel(serverId, container.switchGameRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) { if (state.done) onBack() }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Switch game", onBack = onBack)
        AsyncState(state = state.templates, isEmpty = { it.isEmpty() }, emptyTitle = "No games available", onRetry = vm::load) { templates ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }
                Text("Switching reinstalls the server with a new game. This is destructive.", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                templates.forEach { template ->
                    GlassCard(modifier = Modifier.fillMaxWidth().selectable(selected = state.selectedId == template.id, role = Role.RadioButton) { vm.select(template.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(template.name, color = if (state.selectedId == template.id) DesignTokens.AppPrimary else DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                                template.description?.let { Text(it, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall) }
                            }
                            if (state.selectedId == template.id) StatusChip("Selected", DesignTokens.AppPrimary)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Keep existing files where possible", color = DesignTokens.AppForeground, style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = state.keepData, onCheckedChange = vm::setKeepData)
                }
                RefxDestructiveButton(
                    text = "Switch game",
                    onClick = { confirm = true },
                    enabled = state.selectedId != null && !state.switching,
                    loading = state.switching,
                    fullWidth = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirm) {
        ConfirmDialog("Switch game?", "This reinstalls the server with the selected game and may erase data.", "Switch", { vm.switch(); confirm = false }, { confirm = false })
    }
}
