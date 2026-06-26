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
import androidx.compose.material3.Checkbox
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
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.ServerPermissionCatalog
import gg.refx.android.data.model.SubUser
import gg.refx.android.data.repo.SubUsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubUserEdit(val id: String?, val email: String, val permissions: Set<String>, val saving: Boolean = false)

data class SubUsersUiState(
    val subUsers: LoadState<List<SubUser>> = LoadState.Loading,
    val editing: SubUserEdit? = null,
    val busyId: String? = null,
    val error: String? = null,
)

class SubUsersViewModel(private val serverId: String, private val repo: SubUsersRepository) : ViewModel() {
    private val _state = MutableStateFlow(SubUsersUiState())
    val state: StateFlow<SubUsersUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.subUsers.value == null) _state.update { it.copy(subUsers = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.list(serverId) }
                .onSuccess { list -> _state.update { it.copy(subUsers = LoadState.Loaded(list)) } }
                .onFailure { t -> _state.update { if (it.subUsers.value == null) it.copy(subUsers = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun startAdd() = _state.update { it.copy(editing = SubUserEdit(null, "", emptySet())) }
    fun startEdit(su: SubUser) = _state.update { it.copy(editing = SubUserEdit(su.id, su.email, su.permissions.toSet())) }
    fun cancelEdit() = _state.update { it.copy(editing = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(editing = it.editing?.copy(email = v)) }

    fun togglePermission(p: String) = _state.update { s ->
        val e = s.editing ?: return@update s
        val perms = if (p in e.permissions) e.permissions - p else e.permissions + p
        s.copy(editing = e.copy(permissions = perms))
    }

    fun save() {
        val e = _state.value.editing ?: return
        if (e.id == null && e.email.isBlank()) return
        _state.update { it.copy(editing = e.copy(saving = true), error = null) }
        viewModelScope.launch {
            val result = runCatching {
                if (e.id == null) repo.create(serverId, e.email, e.permissions.toList())
                else repo.update(serverId, e.id, e.permissions.toList())
            }
            result.onSuccess { _state.update { it.copy(editing = null) }; load() }
                .onFailure { t -> _state.update { it.copy(editing = e.copy(saving = false), error = t.toApiException().message) } }
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
}

@Composable
fun SubUsersScreen(serverId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SubUsersViewModel = viewModel(
        factory = viewModelFactory { initializer { SubUsersViewModel(serverId, container.subUsersRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    state.editing?.let { edit ->
        SubUserEditor(edit = edit, error = state.error, onEmailChange = vm::onEmailChange, onToggle = vm::togglePermission, onSave = vm::save, onCancel = vm::cancelEdit)
        return
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Sub-users", onBack = onBack, trailing = { TextButton(onClick = vm::startAdd) { Text("Add") } })
        AsyncState(state = state.subUsers, isEmpty = { it.isEmpty() }, emptyTitle = "No sub-users", emptyMessage = "Invite others to help manage this server.", onRetry = vm::load) { list ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }
                list.forEach { su ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(su.email, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                                Text("${su.permissions.size} permissions", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { vm.startEdit(su) }, enabled = state.busyId != su.id) { Text("Edit") }
                            TextButton(onClick = { confirmDelete = su.id }, enabled = state.busyId != su.id) { Text("Remove", color = DesignTokens.AppDestructive) }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { id ->
        ConfirmDialog("Remove sub-user?", "They will lose access to this server.", "Remove", { vm.delete(id); confirmDelete = null }, { confirmDelete = null })
    }
}

@Composable
private fun SubUserEditor(
    edit: SubUserEdit,
    error: String?,
    onEmailChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = if (edit.id == null) "Add sub-user" else "Edit permissions", onBack = onCancel)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }
            if (edit.id == null) {
                OutlinedTextField(edit.email, onEmailChange, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            } else {
                Text(edit.email, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
            }
            ServerPermissionCatalog.groups.forEach { group ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SectionHeader(title = group.title)
                        group.permissions.forEach { perm ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Checkbox(checked = perm in edit.permissions, onCheckedChange = { onToggle(perm) })
                                Text(ServerPermissionCatalog.label(perm), color = DesignTokens.AppForeground, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        RefxPrimaryButton(
            text = if (edit.id == null) "Invite" else "Save permissions",
            onClick = onSave,
            enabled = (edit.id != null || edit.email.isNotBlank()) && !edit.saving,
            loading = edit.saving,
            fullWidth = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
