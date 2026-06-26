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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.DbEngine
import gg.refx.android.data.model.ServerDatabase
import gg.refx.android.data.repo.DatabasesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DatabasesUiState(
    val databases: LoadState<List<ServerDatabase>> = LoadState.Loading,
    val busyId: String? = null,
    val error: String? = null,
    val rotatedPassword: String? = null,
)

class DatabasesViewModel(private val serverId: String, private val repo: DatabasesRepository) : ViewModel() {
    private val _state = MutableStateFlow(DatabasesUiState())
    val state: StateFlow<DatabasesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.databases.value == null) _state.update { it.copy(databases = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.list(serverId) }
                .onSuccess { dbs -> _state.update { it.copy(databases = LoadState.Loaded(dbs)) } }
                .onFailure { t -> _state.update { if (it.databases.value == null) it.copy(databases = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun create(engine: DbEngine, name: String) {
        viewModelScope.launch {
            runCatching { repo.create(serverId, engine.raw, name, null) }
                .onSuccess { load() }
                .onFailure { t -> _state.update { it.copy(error = t.toApiException().message) } }
        }
    }

    fun rotate(id: String) {
        _state.update { it.copy(busyId = id, error = null) }
        viewModelScope.launch {
            runCatching { repo.rotate(serverId, id) }
                .onSuccess { pw -> _state.update { it.copy(busyId = null, rotatedPassword = pw.password) } }
                .onFailure { t -> _state.update { it.copy(busyId = null, error = t.toApiException().message) } }
        }
    }

    fun delete(id: String) {
        _state.update { it.copy(busyId = id, error = null) }
        viewModelScope.launch {
            runCatching { repo.delete(serverId, id) }
                .onSuccess { _state.update { it.copy(busyId = null) }; load() }
                .onFailure { t -> _state.update { it.copy(busyId = null, error = t.toApiException().message) } }
        }
    }

    fun dismissPassword() = _state.update { it.copy(rotatedPassword = null) }
}

@Composable
fun DatabasesScreen(serverId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val clipboard = LocalClipboardManager.current
    val vm: DatabasesViewModel = viewModel(
        factory = viewModelFactory { initializer { DatabasesViewModel(serverId, container.databasesRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var engine by remember { mutableStateOf(DbEngine.MYSQL) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Databases", onBack = onBack, trailing = { TextButton(onClick = { showCreate = true }) { Text("Create") } })
        AsyncState(state = state.databases, isEmpty = { it.isEmpty() }, emptyTitle = "No databases", emptyMessage = "Create a database for your server.", onRetry = vm::load) { dbs ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }
                dbs.forEach { db ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(db.displayName, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                                    db.connectionHost?.let { Text(it, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                                }
                                StatusChip(db.engine.name, DesignTokens.AppAccentText)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { vm.rotate(db.id) }, enabled = state.busyId != db.id) { Text("Rotate password") }
                                TextButton(onClick = { confirmDelete = db.id }, enabled = state.busyId != db.id) { Text("Delete", color = DesignTokens.AppDestructive) }
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
            title = { Text("Create database") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(DbEngine.MYSQL, DbEngine.MARIADB).forEach { e ->
                            val selected = engine == e
                            TextButton(onClick = { engine = e }) {
                                Text(e.name, color = if (selected) DesignTokens.AppPrimary else DesignTokens.AppMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { vm.create(engine, name); name = ""; showCreate = false } }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }

    confirmDelete?.let { id ->
        ConfirmDialog("Delete database?", "This permanently deletes the database and its data.", "Delete", { vm.delete(id); confirmDelete = null }, { confirmDelete = null })
    }

    state.rotatedPassword?.let { pw ->
        AlertDialog(
            onDismissRequest = vm::dismissPassword,
            title = { Text("New password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copy this now — it can't be retrieved later.", color = DesignTokens.AppWarning, style = MaterialTheme.typography.bodySmall)
                    Text(pw, fontFamily = FontFamily.Monospace, color = DesignTokens.AppForegroundStrong)
                }
            },
            confirmButton = { TextButton(onClick = { clipboard.setText(AnnotatedString(pw)); vm.dismissPassword() }) { Text("Copy & close") } },
        )
    }
}
