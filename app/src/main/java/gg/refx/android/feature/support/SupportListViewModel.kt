package gg.refx.android.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.Ticket
import gg.refx.android.data.repo.SupportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupportListUiState(
    val state: LoadState<List<Ticket>> = LoadState.Idle,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
)

class SupportListViewModel(private val repo: SupportRepository) : ViewModel() {

    private val _state = MutableStateFlow(SupportListUiState())
    val state: StateFlow<SupportListUiState> = _state.asStateFlow()

    private var page = 1
    private val accumulated = mutableListOf<Ticket>()
    private var loadMoreJob: Job? = null
    private var epoch = 0

    init { load() }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        load()
    }

    fun loadNextPage() {
        val s = _state.value
        if (!s.hasMore || s.isLoadingMore || s.state is LoadState.Loading) return
        val gen = epoch
        _state.update { it.copy(isLoadingMore = true) }
        loadMoreJob = viewModelScope.launch {
            runCatching { repo.tickets(page + 1, mine = true) }
                .onSuccess { result ->
                    // Drop a load-more that a concurrent refresh/reload superseded.
                    if (gen != epoch) { _state.update { it.copy(isLoadingMore = false) }; return@onSuccess }
                    page += 1
                    addDistinct(result.items)
                    _state.update {
                        it.copy(state = LoadState.Loaded(accumulated.toList()), hasMore = result.hasMore, isLoadingMore = false)
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingMore = false) } }
        }
    }

    fun load() {
        // Bump the generation and cancel any in-flight load-more so a stale page
        // can't resume and corrupt the freshly-reset list (matches sibling VMs).
        val gen = ++epoch
        loadMoreJob?.cancel()
        page = 1
        if (_state.value.state.value == null) _state.update { it.copy(state = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.tickets(1, mine = true) }
                .onSuccess { result ->
                    if (gen != epoch) { _state.update { it.copy(isRefreshing = false) }; return@onSuccess }
                    page = 1
                    accumulated.clear()
                    addDistinct(result.items)
                    _state.update {
                        it.copy(state = LoadState.Loaded(accumulated.toList()), hasMore = result.hasMore, isRefreshing = false, isLoadingMore = false)
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            state = if (it.state.value == null) LoadState.Failed(t.toApiException().message) else it.state,
                            isRefreshing = false,
                        )
                    }
                }
        }
    }

    private fun addDistinct(items: List<Ticket>) {
        val seen = accumulated.mapTo(HashSet()) { it.id }
        for (item in items) if (seen.add(item.id)) accumulated += item
    }
}
