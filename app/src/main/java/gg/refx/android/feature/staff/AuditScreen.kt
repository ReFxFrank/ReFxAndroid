package gg.refx.android.feature.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
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
import gg.refx.android.data.model.AuditEntry
import gg.refx.android.data.repo.StaffRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuditUiState(
    val entries: LoadState<List<AuditEntry>> = LoadState.Loading,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
)

class AuditViewModel(private val repo: StaffRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuditUiState())
    val state: StateFlow<AuditUiState> = _state.asStateFlow()
    private var page = 1
    private val accumulated = mutableListOf<AuditEntry>()
    private var loadMoreJob: Job? = null
    private var epoch = 0

    init { load() }

    fun load() {
        val gen = ++epoch
        loadMoreJob?.cancel()
        page = 1
        if (_state.value.entries.value == null) _state.update { it.copy(entries = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.auditLogs(1) }
                .onSuccess { r ->
                    if (gen != epoch) return@onSuccess
                    page = 1; accumulated.clear(); accumulated += r.items
                    _state.update { it.copy(entries = LoadState.Loaded(accumulated.toList()), hasMore = r.hasMore, isLoadingMore = false) }
                }
                .onFailure { t -> _state.update { if (it.entries.value == null) it.copy(entries = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun loadNextPage() {
        val s = _state.value
        if (!s.hasMore || s.isLoadingMore) return
        val gen = epoch
        _state.update { it.copy(isLoadingMore = true) }
        loadMoreJob = viewModelScope.launch {
            runCatching { repo.auditLogs(page + 1) }
                .onSuccess { r ->
                    if (gen != epoch) { _state.update { it.copy(isLoadingMore = false) }; return@onSuccess }
                    page += 1; accumulated += r.items
                    _state.update { it.copy(entries = LoadState.Loaded(accumulated.toList()), hasMore = r.hasMore, isLoadingMore = false) }
                }
                .onFailure { _state.update { it.copy(isLoadingMore = false) } }
        }
    }
}

@Composable
fun AuditScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: AuditViewModel = viewModel(factory = viewModelFactory { initializer { AuditViewModel(container.staffRepository) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last -> val total = state.entries.value?.size ?: 0; if (total > 0 && last >= total - 3) vm.loadNextPage() }
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Audit log", onBack = onBack)
        AsyncState(state = state.entries, isEmpty = { it.isEmpty() }, emptyTitle = "No audit entries", onRetry = vm::load) { entries ->
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.id }) { entry ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(entry.action, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                            Text(
                                listOfNotNull(entry.targetType?.let { "$it ${entry.targetId ?: ""}".trim() }, entry.ip).joinToString(" · "),
                                color = DesignTokens.AppMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
