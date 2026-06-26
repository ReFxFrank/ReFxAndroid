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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.Ticket
import gg.refx.android.data.repo.SupportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QueueUiState(
    val tickets: LoadState<List<Ticket>> = LoadState.Loading,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
)

/** Staff support queue — all tickets (mine=false), reusing the support API. */
class QueueViewModel(private val repo: SupportRepository) : ViewModel() {
    private val _state = MutableStateFlow(QueueUiState())
    val state: StateFlow<QueueUiState> = _state.asStateFlow()
    private var page = 1
    private val accumulated = mutableListOf<Ticket>()

    init { load() }

    fun load() {
        page = 1
        if (_state.value.tickets.value == null) _state.update { it.copy(tickets = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.tickets(1, mine = false) }
                .onSuccess { r -> page = 1; accumulated.clear(); addDistinct(r.items); _state.update { it.copy(tickets = LoadState.Loaded(accumulated.toList()), hasMore = r.hasMore) } }
                .onFailure { t -> _state.update { if (it.tickets.value == null) it.copy(tickets = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun loadNextPage() {
        val s = _state.value
        if (!s.hasMore || s.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            runCatching { repo.tickets(page + 1, mine = false) }
                .onSuccess { r -> page += 1; addDistinct(r.items); _state.update { it.copy(tickets = LoadState.Loaded(accumulated.toList()), hasMore = r.hasMore, isLoadingMore = false) } }
                .onFailure { _state.update { it.copy(isLoadingMore = false) } }
        }
    }

    private fun addDistinct(items: List<Ticket>) {
        val seen = accumulated.mapTo(HashSet()) { it.id }
        for (item in items) if (seen.add(item.id)) accumulated += item
    }
}

@Composable
fun QueueScreen(onBack: () -> Unit, onOpenTicket: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: QueueViewModel = viewModel(factory = viewModelFactory { initializer { QueueViewModel(container.supportRepository) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last -> val total = state.tickets.value?.size ?: 0; if (total > 0 && last >= total - 3) vm.loadNextPage() }
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Support queue", onBack = onBack)
        AsyncState(state = state.tickets, isEmpty = { it.isEmpty() }, emptyTitle = "Queue is clear", emptyMessage = "No open tickets right now.", onRetry = vm::load) { tickets ->
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tickets, key = { it.id }) { ticket ->
                    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onOpenTicket(ticket.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ticket.subject, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                                Text("#${ticket.number}", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            StatusChip(ticket.state.name.replace('_', ' '), DesignTokens.AppAccentText)
                        }
                    }
                }
            }
        }
    }
}
