package gg.refx.android.feature.servers.sections

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
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.CreateScheduleRequest
import gg.refx.android.data.model.Schedule
import gg.refx.android.data.model.ScheduleAction
import gg.refx.android.data.model.ScheduleTaskRequest
import gg.refx.android.data.repo.SchedulesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SchedulesUiState(
    val schedules: LoadState<List<Schedule>> = LoadState.Loading,
    val busyId: String? = null,
    val creating: Boolean = false,
    val error: String? = null,
)

class SchedulesViewModel(private val serverId: String, private val repo: SchedulesRepository) : ViewModel() {
    private val _state = MutableStateFlow(SchedulesUiState())
    val state: StateFlow<SchedulesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.schedules.value == null) _state.update { it.copy(schedules = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.list(serverId) }
                .onSuccess { list -> _state.update { it.copy(schedules = LoadState.Loaded(list)) } }
                .onFailure { t -> _state.update { if (it.schedules.value == null) it.copy(schedules = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun toggle(id: String, active: Boolean) = mutate(id) { repo.setActive(serverId, id, active) }
    fun run(id: String) = mutate(id) { repo.run(serverId, id) }
    fun delete(id: String) = mutate(id) { repo.delete(serverId, id) }

    fun create(name: String, cron: String, onlyWhenOnline: Boolean, action: ScheduleAction, payload: String) {
        if (_state.value.creating) return
        _state.update { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repo.create(
                    serverId,
                    CreateScheduleRequest(
                        name = name.trim(),
                        cron = cron.trim(),
                        onlyWhenOnline = onlyWhenOnline,
                        // A schedule needs at least one task to do anything (§5).
                        tasks = listOf(ScheduleTaskRequest(action = action.raw, payload = payload.trim().ifBlank { null })),
                    ),
                )
            }
                .onSuccess { _state.update { it.copy(creating = false) }; load() }
                .onFailure { t -> _state.update { it.copy(creating = false, error = t.toApiException().message) } }
        }
    }

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
fun SchedulesScreen(serverId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SchedulesViewModel = viewModel(
        factory = viewModelFactory { initializer { SchedulesViewModel(serverId, container.schedulesRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var cron by remember { mutableStateOf("0 * * * *") }
    var onlyWhenOnline by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf(ScheduleAction.COMMAND) }
    var payload by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Schedules", onBack = onBack, trailing = { TextButton(onClick = { showCreate = true }) { Text("New") } })
        AsyncState(state = state.schedules, isEmpty = { it.isEmpty() }, emptyTitle = "No schedules", emptyMessage = "Automate restarts, backups and commands with cron schedules.", onRetry = vm::load) { list ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }
                list.forEach { schedule ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(schedule.name, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                                    schedule.cron?.let { Text(it, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                                }
                                Switch(checked = schedule.isActive, onCheckedChange = { vm.toggle(schedule.id, it) }, enabled = state.busyId != schedule.id)
                            }
                            Text("${schedule.tasks.size} task${if (schedule.tasks.size == 1) "" else "s"}", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { vm.run(schedule.id) }, enabled = state.busyId != schedule.id) { Text("Run now") }
                                TextButton(onClick = { confirmDelete = schedule.id }, enabled = state.busyId != schedule.id) { Text("Delete", color = DesignTokens.AppDestructive) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        val payloadLabel = when (action) {
            ScheduleAction.COMMAND -> "Command"
            ScheduleAction.POWER -> "Power signal (start/stop/restart/kill)"
            ScheduleAction.BACKUP -> "Backup name (optional)"
            ScheduleAction.UNKNOWN -> "Payload"
        }
        val payloadRequired = action == ScheduleAction.COMMAND || action == ScheduleAction.POWER
        val canCreate = name.isNotBlank() && cron.isNotBlank() && (!payloadRequired || payload.isNotBlank())
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New schedule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(cron, { cron = it }, label = { Text("Cron expression") }, singleLine = true)
                    Text("Task", color = DesignTokens.AppMuted, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(ScheduleAction.COMMAND, ScheduleAction.POWER, ScheduleAction.BACKUP).forEach { a ->
                            TextButton(onClick = { action = a }) {
                                Text(a.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (action == a) DesignTokens.AppPrimary else DesignTokens.AppMuted)
                            }
                        }
                    }
                    OutlinedTextField(payload, { payload = it }, label = { Text(payloadLabel) }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = onlyWhenOnline, onCheckedChange = { onlyWhenOnline = it })
                        Text("Only when the server is online", color = DesignTokens.AppForeground, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canCreate && !state.creating,
                    onClick = {
                        vm.create(name, cron, onlyWhenOnline, action, payload)
                        name = ""; payload = ""; showCreate = false
                    },
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }
    confirmDelete?.let { id ->
        ConfirmDialog("Delete schedule?", "This can't be undone.", "Delete", { vm.delete(id); confirmDelete = null }, { confirmDelete = null })
    }
}
