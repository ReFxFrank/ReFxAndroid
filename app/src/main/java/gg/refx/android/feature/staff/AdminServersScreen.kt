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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.snapshotFlow
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
import gg.refx.android.core.design.StatePill
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.feature.servers.sections.ConfirmDialog
import gg.refx.android.data.model.Server
import gg.refx.android.data.model.StateColors
import gg.refx.android.data.repo.StaffRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminServersUiState(
    val servers: LoadState<List<Server>> = LoadState.Loading,
    val query: String = "",
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val busyId: String? = null,
    val error: String? = null,
)

class AdminServersViewModel(private val repo: StaffRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminServersUiState())
    val state: StateFlow<AdminServersUiState> = _state.asStateFlow()

    private var page = 1
    private val accumulated = mutableListOf<Server>()
    private var searchJob: Job? = null

    init { load() }

    fun onQueryChange(v: String) {
        _state.update { it.copy(query = v) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(350); load() }
    }

    fun load() {
        page = 1
        if (_state.value.servers.value == null) _state.update { it.copy(servers = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.servers(1, query = _state.value.query) }
                .onSuccess { r -> page = 1; accumulated.clear(); addDistinct(r.items); _state.update { it.copy(servers = LoadState.Loaded(accumulated.toList()), hasMore = r.hasMore) } }
                .onFailure { t -> _state.update { if (it.servers.value == null) it.copy(servers = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun loadNextPage() {
        val s = _state.value
        if (!s.hasMore || s.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            runCatching { repo.servers(page + 1, query = s.query) }
                .onSuccess { r -> page += 1; addDistinct(r.items); _state.update { it.copy(servers = LoadState.Loaded(accumulated.toList()), hasMore = r.hasMore, isLoadingMore = false) } }
                .onFailure { _state.update { it.copy(isLoadingMore = false) } }
        }
    }

    fun delete(id: String) {
        _state.update { it.copy(busyId = id, error = null) }
        viewModelScope.launch {
            runCatching { repo.deleteServer(id) }
                .onSuccess { _state.update { it.copy(busyId = null) }; load() }
                .onFailure { t -> _state.update { it.copy(busyId = null, error = t.toApiException().message) } }
        }
    }

    private fun addDistinct(items: List<Server>) {
        val seen = accumulated.mapTo(HashSet()) { it.id }
        for (item in items) if (seen.add(item.id)) accumulated += item
    }
}

@Composable
fun AdminServersScreen(onBack: () -> Unit, onCreate: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: AdminServersViewModel = viewModel(
        factory = viewModelFactory { initializer { AdminServersViewModel(container.staffRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last -> val total = state.servers.value?.size ?: 0; if (total > 0 && last >= total - 3) vm.loadNextPage() }
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Servers", onBack = onBack, trailing = { TextButton(onClick = onCreate) { Icon(Icons.Filled.Add, contentDescription = "Create server") } })
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            placeholder = { Text("Search servers") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        )
        state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp)) }
        AsyncState(state = state.servers, isEmpty = { it.isEmpty() && state.query.isBlank() }, emptyTitle = "No servers", onRetry = vm::load) { servers ->
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(servers, key = { it.id }) { server ->
                    val (label, color) = StateColors.server(server.state)
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(server.name, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                                Text(server.node?.name ?: server.gameName, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            StatePill(label = label, color = color)
                            TextButton(onClick = { confirmDelete = server.id }, enabled = state.busyId != server.id) { Text("Delete", color = DesignTokens.AppDestructive) }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { id ->
        ConfirmDialog("Delete server?", "This permanently deletes the server.", "Delete", { vm.delete(id); confirmDelete = null }, { confirmDelete = null })
    }
}
