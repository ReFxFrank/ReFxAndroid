package gg.refx.android.feature.staff

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.data.model.AdminCreateServerBody
import gg.refx.android.data.model.AdminGameTemplate
import gg.refx.android.data.model.AdminUser
import gg.refx.android.data.model.NodeAdmin
import gg.refx.android.data.repo.StaffRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateServerUiState(
    val templates: List<AdminGameTemplate> = emptyList(),
    val nodes: List<NodeAdmin> = emptyList(),
    val owners: List<AdminUser> = emptyList(),
    val name: String = "",
    val ownerQuery: String = "",
    val selectedTemplateId: String? = null,
    val selectedNodeId: String? = null,
    val selectedOwnerId: String? = null,
    val cpuCores: String = "",
    val memoryMb: String = "",
    val diskMb: String = "",
    val slots: String = "",
    val swapMb: String = "",
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
    val createdOnNode: String? = null,
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && selectedTemplateId != null && selectedNodeId != null &&
            selectedOwnerId != null && !submitting
}

class CreateServerViewModel(private val repo: StaffRepository) : ViewModel() {
    private val _state = MutableStateFlow(CreateServerUiState())
    val state: StateFlow<CreateServerUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val t = async { runCatching { repo.templates() }.getOrDefault(emptyList()) }
            val n = async { runCatching { repo.nodes(1).items }.getOrDefault(emptyList()) }
            val u = async { runCatching { repo.users(1).items }.getOrDefault(emptyList()) }
            _state.update { it.copy(templates = t.await(), nodes = n.await(), owners = u.await(), loading = false) }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v) }
    fun onOwnerQuery(v: String) {
        _state.update { it.copy(ownerQuery = v) }
        viewModelScope.launch {
            val owners = runCatching { repo.users(1, query = v).items }.getOrDefault(emptyList())
            _state.update { it.copy(owners = owners) }
        }
    }
    fun selectTemplate(id: String) = _state.update { it.copy(selectedTemplateId = id) }
    fun selectNode(id: String) = _state.update { it.copy(selectedNodeId = id) }
    fun selectOwner(id: String) = _state.update { it.copy(selectedOwnerId = id) }
    fun onCpu(v: String) = _state.update { it.copy(cpuCores = v) }
    fun onMem(v: String) = _state.update { it.copy(memoryMb = v) }
    fun onDisk(v: String) = _state.update { it.copy(diskMb = v) }
    fun onSlots(v: String) = _state.update { it.copy(slots = v) }
    fun onSwap(v: String) = _state.update { it.copy(swapMb = v) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val body = AdminCreateServerBody(
                name = s.name.trim(),
                ownerId = s.selectedOwnerId!!,
                nodeId = s.selectedNodeId!!,
                templateId = s.selectedTemplateId!!,
                cpuCores = s.cpuCores.toDoubleOrNull(),
                memoryMb = s.memoryMb.toIntOrNull(),
                diskMb = s.diskMb.toIntOrNull(),
                slots = s.slots.toIntOrNull(),
                swapMb = s.swapMb.toIntOrNull(),
            )
            runCatching { repo.createServer(body) }
                .onSuccess {
                    val nodeName = s.nodes.firstOrNull { it.id == s.selectedNodeId }?.name ?: "the node"
                    _state.update { it.copy(submitting = false, createdOnNode = nodeName) }
                }
                .onFailure { t -> _state.update { it.copy(submitting = false, error = t.toApiException().message) } }
        }
    }
}

@Composable
fun AdminCreateServerScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: CreateServerViewModel = viewModel(
        factory = viewModelFactory { initializer { CreateServerViewModel(container.staffRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdOnNode) { if (state.createdOnNode != null) onBack() }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Create server", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }

            OutlinedTextField(state.name, vm::onName, label = { Text("Server name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            PickerCard("Template") {
                state.templates.forEach { t ->
                    PickRow(t.name, t.slug, state.selectedTemplateId == t.id) { vm.selectTemplate(t.id) }
                }
            }
            PickerCard("Node") {
                state.nodes.forEach { n ->
                    PickRow(n.name, n.fqdn ?: n.state.name, state.selectedNodeId == n.id) { vm.selectNode(n.id) }
                }
            }
            PickerCard("Owner") {
                OutlinedTextField(state.ownerQuery, vm::onOwnerQuery, label = { Text("Search users") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                state.owners.take(8).forEach { u ->
                    PickRow(u.displayName, u.email, state.selectedOwnerId == u.id) { vm.selectOwner(u.id) }
                }
            }

            PickerCard("Resources (optional)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(state.cpuCores, vm::onCpu, label = { Text("vCPU") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(state.memoryMb, vm::onMem, label = { Text("RAM MB") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(state.diskMb, vm::onDisk, label = { Text("Disk MB") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(state.slots, vm::onSlots, label = { Text("Slots") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(state.swapMb, vm::onSwap, label = { Text("Swap MB") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            RefxPrimaryButton("Create server", vm::submit, enabled = state.canSubmit, loading = state.submitting, fullWidth = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PickerCard(title: String, content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = title)
            content()
        }
    }
}

@Composable
private fun PickRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp)) {
        Text(title, color = if (selected) DesignTokens.AppPrimary else DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
    }
}
