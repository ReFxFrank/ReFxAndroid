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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
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
import gg.refx.android.core.design.RefxPrimaryButton
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.data.model.CreateNodeBody
import gg.refx.android.data.model.NodeOS
import gg.refx.android.data.model.Region
import gg.refx.android.data.repo.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddNodeUiState(
    val regions: List<Region> = emptyList(),
    val name: String = "",
    val fqdn: String = "",
    val selectedRegionId: String? = null,
    val os: NodeOS = NodeOS.LINUX,
    val cpuCores: String = "",
    val memoryMb: String = "",
    val diskMb: String = "",
    val portStart: String = "",
    val portEnd: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
    val bootstrapToken: String? = null,
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && fqdn.isNotBlank() && selectedRegionId != null &&
            cpuCores.toIntOrNull() != null && memoryMb.toIntOrNull() != null && diskMb.toIntOrNull() != null &&
            portStart.toIntOrNull() != null && portEnd.toIntOrNull() != null && !submitting
}

class AddNodeViewModel(private val repo: StaffRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddNodeUiState())
    val state: StateFlow<AddNodeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val regions = runCatching { repo.locations() }.getOrDefault(emptyList())
            _state.update { it.copy(regions = regions) }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v) }
    fun onFqdn(v: String) = _state.update { it.copy(fqdn = v) }
    fun selectRegion(id: String) = _state.update { it.copy(selectedRegionId = id) }
    fun setOs(os: NodeOS) = _state.update { it.copy(os = os) }
    fun onCpu(v: String) = _state.update { it.copy(cpuCores = v) }
    fun onMem(v: String) = _state.update { it.copy(memoryMb = v) }
    fun onDisk(v: String) = _state.update { it.copy(diskMb = v) }
    fun onPortStart(v: String) = _state.update { it.copy(portStart = v) }
    fun onPortEnd(v: String) = _state.update { it.copy(portEnd = v) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val body = CreateNodeBody(
                name = s.name.trim(), fqdn = s.fqdn.trim(), regionId = s.selectedRegionId!!, os = s.os.raw,
                cpuCores = s.cpuCores.toInt(), memoryMb = s.memoryMb.toInt(), diskMb = s.diskMb.toInt(),
                allocationPortStart = s.portStart.toInt(), allocationPortEnd = s.portEnd.toInt(),
            )
            runCatching { repo.createNode(body) }
                .onSuccess { result -> _state.update { it.copy(submitting = false, bootstrapToken = result.bootstrapToken) } }
                .onFailure { t -> _state.update { it.copy(submitting = false, error = t.toApiException().message) } }
        }
    }
}

@Composable
fun AddNodeScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val clipboard = LocalClipboardManager.current
    val vm: AddNodeViewModel = viewModel(factory = viewModelFactory { initializer { AddNodeViewModel(container.staffRepository) } })
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Add node", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }
            OutlinedTextField(state.name, vm::onName, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.fqdn, vm::onFqdn, label = { Text("FQDN") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(title = "Region")
                    state.regions.forEach { r ->
                        Text(
                            "${r.name} (${r.code})",
                            color = if (state.selectedRegionId == r.id) DesignTokens.AppPrimary else DesignTokens.AppForeground,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                                .selectable(selected = state.selectedRegionId == r.id, role = Role.RadioButton) { vm.selectRegion(r.id) }
                                .padding(vertical = 14.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(NodeOS.LINUX, NodeOS.WINDOWS).forEach { os ->
                    TextButton(onClick = { vm.setOs(os) }) {
                        Text(os.name, color = if (state.os == os) DesignTokens.AppPrimary else DesignTokens.AppMuted)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(state.cpuCores, vm::onCpu, label = { Text("vCPU") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(state.memoryMb, vm::onMem, label = { Text("RAM MB") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(state.diskMb, vm::onDisk, label = { Text("Disk MB") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(state.portStart, vm::onPortStart, label = { Text("Port start") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(state.portEnd, vm::onPortEnd, label = { Text("Port end") }, singleLine = true, modifier = Modifier.weight(1f))
            }

            RefxPrimaryButton("Create node", vm::submit, enabled = state.canSubmit, loading = state.submitting, fullWidth = true, modifier = Modifier.fillMaxWidth())
        }
    }

    // One-time bootstrap token reveal — can't be retrieved later (§5). Block scrim/
    // back dismissal so an accidental tap can't navigate away before copying.
    state.bootstrapToken?.let { token ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Node created") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shown once — it can't be retrieved later. Use it to bootstrap the node agent.", color = DesignTokens.AppWarning, style = MaterialTheme.typography.bodySmall)
                    Text(token, fontFamily = FontFamily.Monospace, color = DesignTokens.AppForegroundStrong)
                }
            },
            confirmButton = { TextButton(onClick = { clipboard.setText(AnnotatedString(token)); onBack() }) { Text("Copy & close") } },
            dismissButton = { TextButton(onClick = onBack) { Text("Close") } },
        )
    }
}
