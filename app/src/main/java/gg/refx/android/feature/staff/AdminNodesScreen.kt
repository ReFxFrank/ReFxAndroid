package gg.refx.android.feature.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.design.StatePill
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.NodeAdmin
import gg.refx.android.data.model.NodePing
import gg.refx.android.data.model.StateColors
import gg.refx.android.data.repo.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminNodesViewModel(private val repo: StaffRepository) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<List<NodeAdmin>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<NodeAdmin>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.value == null) _state.value = LoadState.Loading
        viewModelScope.launch {
            runCatching { repo.nodes(1).items }
                .onSuccess { _state.value = LoadState.Loaded(it) }
                .onFailure { t -> if (_state.value.value == null) _state.value = LoadState.Failed(t.toApiException().message) }
        }
    }
}

@Composable
fun AdminNodesScreen(onBack: () -> Unit, onOpenNode: (String) -> Unit, onAddNode: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: AdminNodesViewModel = viewModel(factory = viewModelFactory { initializer { AdminNodesViewModel(container.staffRepository) } })
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Nodes", onBack = onBack, trailing = { TextButton(onClick = onAddNode) { Icon(Icons.Filled.Add, contentDescription = "Add node") } })
        AsyncState(state = state, isEmpty = { it.isEmpty() }, emptyTitle = "No nodes", onRetry = vm::load) { nodes ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(nodes, key = { it.id }) { node ->
                    val (label, color) = StateColors.node(node.state)
                    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onOpenNode(node.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(node.name, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                                Text(node.fqdn ?: node.region?.name ?: "—", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            StatePill(label = label, color = color)
                        }
                    }
                }
            }
        }
    }
}

// ── Node detail (ping / restart / update / steam-cache) ─────────────────────

data class NodeDetailUiState(
    val node: LoadState<NodeAdmin> = LoadState.Loading,
    val ping: NodePing? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class NodeDetailViewModel(private val nodeId: String, private val repo: StaffRepository) : ViewModel() {
    private val _state = MutableStateFlow(NodeDetailUiState())
    val state: StateFlow<NodeDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(node = if (it.node.value == null) LoadState.Loading else it.node) }
        viewModelScope.launch {
            runCatching { repo.node(nodeId) }
                .onSuccess { n -> _state.update { it.copy(node = LoadState.Loaded(n)) } }
                .onFailure { t -> _state.update { if (it.node.value == null) it.copy(node = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun ping() = action("Pinging…") { _state.update { it.copy(ping = repo.pingNode(nodeId)) } }
    fun restartAgent() = action("Agent restart requested") { repo.restartAgent(nodeId) }
    fun updateAgent() = action("Agent update requested") { repo.updateAgent(nodeId) }
    fun clearSteamCache() = action("Steam cache cleared") { repo.clearSteamCache(nodeId) }

    private fun action(successMessage: String, block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _state.update { it.copy(busy = false, message = successMessage) } }
                .onFailure { t -> _state.update { it.copy(busy = false, error = t.toApiException().message) } }
        }
    }
}

@Composable
fun AdminNodeDetailScreen(nodeId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: NodeDetailViewModel = viewModel(factory = viewModelFactory { initializer { NodeDetailViewModel(nodeId, container.staffRepository) } })
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = state.node.value?.name ?: "Node", onBack = onBack)
        AsyncState(state = state.node, onRetry = vm::load) { node ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }
                state.message?.let { Text(it, color = DesignTokens.AppSuccess, style = MaterialTheme.typography.bodySmall) }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val (label, color) = StateColors.node(node.state)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(node.fqdn ?: node.name, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatePill(label = label, color = color)
                        }
                        node.agentVersion?.let { Text("Agent $it", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall) }
                        state.ping?.let { p ->
                            Text(
                                if (p.reachable) "Reachable${p.ms?.let { " · ${it.toInt()}ms" } ?: ""}" else "Unreachable",
                                color = if (p.reachable) DesignTokens.AppSuccess else DesignTokens.AppDestructive,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(title = "Actions")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = vm::ping, enabled = !state.busy) { Text("Ping") }
                            TextButton(onClick = vm::restartAgent, enabled = !state.busy) { Text("Restart agent") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = vm::updateAgent, enabled = !state.busy) { Text("Update agent") }
                            TextButton(onClick = vm::clearSteamCache, enabled = !state.busy) { Text("Clear Steam cache") }
                        }
                    }
                }
            }
        }
    }
}
