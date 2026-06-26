package gg.refx.android.feature.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.AdminAlert
import gg.refx.android.data.model.AlertSeverity
import gg.refx.android.feature.servers.sections.ConfirmDialog
import gg.refx.android.data.repo.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertsUiState(
    val alerts: LoadState<List<AdminAlert>> = LoadState.Loading,
    val busyId: String? = null,
    val error: String? = null,
)

class AlertsViewModel(private val repo: StaffRepository) : ViewModel() {
    private val _state = MutableStateFlow(AlertsUiState())
    val state: StateFlow<AlertsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.alerts.value == null) _state.update { it.copy(alerts = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.alerts() }
                .onSuccess { l -> _state.update { it.copy(alerts = LoadState.Loaded(l)) } }
                .onFailure { t -> _state.update { if (it.alerts.value == null) it.copy(alerts = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun create(severity: AlertSeverity, title: String, body: String) {
        viewModelScope.launch {
            runCatching { repo.createAlert(severity.raw, title, body, true) }
                .onSuccess { load() }
                .onFailure { t -> _state.update { it.copy(error = t.toApiException().message) } }
        }
    }

    fun toggle(id: String, active: Boolean) = mutate(id) { repo.setAlertActive(id, active) }
    fun delete(id: String) = mutate(id) { repo.deleteAlert(id) }

    private fun mutate(id: String, action: suspend () -> Unit) {
        _state.update { it.copy(busyId = id, error = null) }
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { _state.update { it.copy(busyId = null) }; load() }
                .onFailure { t -> _state.update { it.copy(busyId = null, error = t.toApiException().message) } }
        }
    }
}

@Composable
fun AlertsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: AlertsViewModel = viewModel(factory = viewModelFactory { initializer { AlertsViewModel(container.staffRepository) } })
    val state by vm.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(AlertSeverity.INFO) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Platform alerts", onBack = onBack, trailing = { TextButton(onClick = { showCreate = true }) { Text("New") } })
        AsyncState(state = state.alerts, isEmpty = { it.isEmpty() }, emptyTitle = "No alerts", emptyMessage = "Banner alerts shown to all users appear here.", onRetry = vm::load) { alerts ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }
                alerts.forEach { alert ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(alert.title, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                StatusChip(alert.severity?.name ?: "INFO", severityColor(alert.severity))
                            }
                            Text(alert.body, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Switch(checked = alert.isActive, onCheckedChange = { vm.toggle(alert.id, it) }, enabled = state.busyId != alert.id)
                                Text("Active", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                TextButton(onClick = { confirmDelete = alert.id }, enabled = state.busyId != alert.id) { Text("Delete", color = DesignTokens.AppDestructive) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New alert") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(body, { body = it }, label = { Text("Body") })
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(AlertSeverity.INFO, AlertSeverity.WARNING, AlertSeverity.CRITICAL).forEach { s ->
                            TextButton(onClick = { severity = s }) {
                                Text(s.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (severity == s) DesignTokens.AppPrimary else DesignTokens.AppMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(enabled = title.isNotBlank() && body.isNotBlank(), onClick = { vm.create(severity, title, body); title = ""; body = ""; showCreate = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }
    confirmDelete?.let { id ->
        ConfirmDialog("Delete alert?", "This removes the banner for all users.", "Delete", { vm.delete(id); confirmDelete = null }, { confirmDelete = null })
    }
}

internal fun severityColor(severity: AlertSeverity?) = when (severity) {
    AlertSeverity.CRITICAL -> DesignTokens.AppDestructive
    AlertSeverity.WARNING -> DesignTokens.AppWarning
    AlertSeverity.INFO -> DesignTokens.AppPrimary
    else -> DesignTokens.AppMuted
}
