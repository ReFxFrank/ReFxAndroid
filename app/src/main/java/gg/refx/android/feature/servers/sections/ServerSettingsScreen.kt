package gg.refx.android.feature.servers.sections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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
import gg.refx.android.core.design.RefxSecondaryButton
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.ServerVariable
import gg.refx.android.data.model.StartupConfig
import gg.refx.android.data.repo.ServerSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val startup: LoadState<StartupConfig> = LoadState.Loading,
    val savingVar: String? = null,
    val reinstalling: Boolean = false,
    val error: String? = null,
)

class ServerSettingsViewModel(private val serverId: String, private val repo: ServerSettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.startup.value == null) _state.update { it.copy(startup = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.startup(serverId) }
                .onSuccess { cfg -> _state.update { it.copy(startup = LoadState.Loaded(cfg)) } }
                .onFailure { t -> _state.update { if (it.startup.value == null) it.copy(startup = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun saveVariable(envName: String, value: String) {
        _state.update { it.copy(savingVar = envName, error = null) }
        viewModelScope.launch {
            runCatching { repo.updateVariable(serverId, envName, value) }
                .onSuccess { _state.update { it.copy(savingVar = null) }; load() }
                .onFailure { t -> _state.update { it.copy(savingVar = null, error = t.toApiException().message) } }
        }
    }

    fun reinstall() {
        if (_state.value.reinstalling) return
        _state.update { it.copy(reinstalling = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.reinstall(serverId) }
                .onSuccess { _state.update { it.copy(reinstalling = false) } }
                .onFailure { t -> _state.update { it.copy(reinstalling = false, error = t.toApiException().message) } }
        }
    }
}

@Composable
fun ServerSettingsScreen(serverId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: ServerSettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { ServerSettingsViewModel(serverId, container.serverSettingsRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmReinstall by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Settings", onBack = onBack)
        AsyncState(state = state.startup, onRetry = vm::load) { cfg ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }

                (cfg.command ?: cfg.rawStartup)?.let { command ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionHeader(title = "Startup command")
                            Text(command, color = DesignTokens.AppForeground, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                if (cfg.variables.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(title = "Startup variables")
                            cfg.variables.forEach { v ->
                                VariableEditor(
                                    variable = v,
                                    saving = state.savingVar == v.envName,
                                    onSave = { value -> vm.saveVariable(v.envName, value) },
                                )
                            }
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(title = "Reinstall")
                        Text("Reinstalling wipes and reinstalls the server software. Files in the data directory are preserved where the egg supports it.", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                        RefxDestructiveButton("Reinstall server", { confirmReinstall = true }, loading = state.reinstalling, fullWidth = true)
                    }
                }
            }
        }
    }

    if (confirmReinstall) {
        ConfirmDialog("Reinstall server?", "This reinstalls the server software and may take several minutes.", "Reinstall", { vm.reinstall(); confirmReinstall = false }, { confirmReinstall = false })
    }
}

@Composable
private fun VariableEditor(variable: ServerVariable, saving: Boolean, onSave: (String) -> Unit) {
    var value by remember(variable.envName, variable.value) { mutableStateOf(variable.value ?: variable.defaultValue ?: "") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(variable.label, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.bodyMedium)
        variable.description?.let { Text(it, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall) }
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            enabled = variable.userEditable && !saving,
            modifier = Modifier.fillMaxWidth(),
        )
        if (variable.userEditable) {
            RefxSecondaryButton(text = if (saving) "Saving…" else "Save", onClick = { onSave(value) }, enabled = !saving)
        }
    }
}
