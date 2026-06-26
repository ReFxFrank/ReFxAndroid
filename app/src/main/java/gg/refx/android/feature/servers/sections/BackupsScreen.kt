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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import gg.refx.android.core.ui.WebLink
import gg.refx.android.data.model.Backup
import gg.refx.android.data.model.BackupState
import gg.refx.android.data.repo.BackupsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupsUiState(
    val backups: LoadState<List<Backup>> = LoadState.Loading,
    val busyId: String? = null,
    val creating: Boolean = false,
    val error: String? = null,
    val downloadUrl: String? = null,
)

class BackupsViewModel(private val serverId: String, private val repo: BackupsRepository) : ViewModel() {
    private val _state = MutableStateFlow(BackupsUiState())
    val state: StateFlow<BackupsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.backups.value == null) _state.update { it.copy(backups = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.list(serverId, 1) }
                .onSuccess { r -> _state.update { it.copy(backups = LoadState.Loaded(r.items)) } }
                .onFailure { t -> _state.update { if (it.backups.value == null) it.copy(backups = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun create(name: String) {
        _state.update { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.create(serverId, name) }
                .onSuccess { _state.update { it.copy(creating = false) }; load() }
                .onFailure { t -> _state.update { it.copy(creating = false, error = t.toApiException().message) } }
        }
    }

    fun restore(id: String) = mutate(id) { repo.restore(serverId, id) }
    fun delete(id: String) = mutate(id) { repo.delete(serverId, id) }

    private fun mutate(id: String, action: suspend () -> Unit) {
        _state.update { it.copy(busyId = id, error = null) }
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { _state.update { it.copy(busyId = null) }; load() }
                .onFailure { t -> _state.update { it.copy(busyId = null, error = t.toApiException().message) } }
        }
    }

    fun download(id: String) {
        viewModelScope.launch {
            runCatching { repo.downloadUrl(serverId, id) }
                .onSuccess { url -> _state.update { it.copy(downloadUrl = url) } }
                .onFailure { t -> _state.update { it.copy(error = t.toApiException().message) } }
        }
    }

    fun downloadOpened() = _state.update { it.copy(downloadUrl = null) }
}

@Composable
fun BackupsScreen(serverId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm: BackupsViewModel = viewModel(
        factory = viewModelFactory { initializer { BackupsViewModel(serverId, container.backupsRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.downloadUrl) { state.downloadUrl?.let { WebLink.open(context, it); vm.downloadOpened() } }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Backups", onBack = onBack, trailing = { TextButton(onClick = { showCreate = true }) { Text("Create") } })
        AsyncState(state = state.backups, isEmpty = { it.isEmpty() }, emptyTitle = "No backups", emptyMessage = "Create a backup to protect your server.", onRetry = vm::load) { backups ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }
                backups.forEach { backup ->
                    BackupRow(
                        backup = backup,
                        busy = state.busyId == backup.id,
                        onRestore = { confirmRestore = backup.id },
                        onDelete = { confirmDelete = backup.id },
                        onDownload = { vm.download(backup.id) },
                    )
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Create backup") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Name (optional)") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.create(name); name = ""; showCreate = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }
    confirmRestore?.let { id ->
        ConfirmDialog("Restore backup?", "This will overwrite the current server files.", "Restore", { vm.restore(id); confirmRestore = null }, { confirmRestore = null })
    }
    confirmDelete?.let { id ->
        ConfirmDialog("Delete backup?", "This can't be undone.", "Delete", { vm.delete(id); confirmDelete = null }, { confirmDelete = null })
    }
}

@Composable
private fun BackupRow(backup: Backup, busy: Boolean, onRestore: () -> Unit, onDelete: () -> Unit, onDownload: () -> Unit) {
    val (label, color) = when (backup.state) {
        BackupState.COMPLETED -> "Completed" to DesignTokens.AppSuccess
        BackupState.FAILED -> "Failed" to DesignTokens.AppDestructive
        BackupState.PENDING, BackupState.IN_PROGRESS -> "In progress" to DesignTokens.AppWarning
        BackupState.UNKNOWN -> "Unknown" to DesignTokens.AppMuted
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(backup.displayName, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                    backup.sizeLabel?.let { Text(it, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall) }
                }
                StatusChip(label, color)
            }
            if (backup.state == BackupState.COMPLETED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRestore, enabled = !busy) { Text("Restore") }
                    TextButton(onClick = onDownload, enabled = !busy) { Text("Download") }
                    TextButton(onClick = onDelete, enabled = !busy) { Text("Delete", color = DesignTokens.AppDestructive) }
                }
            }
        }
    }
}

@Composable
internal fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = DesignTokens.AppDestructive) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
