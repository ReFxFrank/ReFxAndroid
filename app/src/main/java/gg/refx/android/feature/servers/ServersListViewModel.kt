package gg.refx.android.feature.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.Server
import gg.refx.android.data.model.ServerState
import gg.refx.android.data.repo.ServersRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServersListUiState(
    val state: LoadState<List<Server>> = LoadState.Idle,
    val query: String = "",
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
) {
    /** Servers that need attention (suspended / crashed / pending payment). */
    val attentionCount: Int
        get() = state.value?.count {
            it.isSuspended ||
                it.state == ServerState.CRASHED ||
                it.state == ServerState.PENDING_PAYMENT
        } ?: 0
}

/**
 * Servers list (parity spec §8): paginated `GET servers?page&pageSize=25[&q]`,
 * infinite scroll while `page < totalPages`, debounced search, pull-to-refresh,
 * and a 12s periodic refresh while the screen is visible (driven by the screen).
 *
 * Concurrency: every reset bumps an [epoch]; in-flight page loads whose epoch is
 * stale are discarded, and the load-more job is cancelled on reset — so a refresh
 * and an infinite-scroll append can never interleave. Appends are de-duplicated by
 * id (the list is rendered with `key = { it.id }`, so duplicates would crash).
 */
class ServersListViewModel(private val repo: ServersRepository) : ViewModel() {

    private val _state = MutableStateFlow(ServersListUiState())
    val state: StateFlow<ServersListUiState> = _state.asStateFlow()

    private var page = 1
    private val accumulated = mutableListOf<Server>()
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var epoch = 0

    init {
        reload(showLoading = true)
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            reload(showLoading = true)
        }
    }

    /** Pull-to-refresh: reload from page 1. */
    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        reload(showLoading = false)
    }

    /** Periodic silent poll: re-fetch the currently-loaded pages without a spinner
     *  and without collapsing infinite-scroll progress. */
    fun silentRefresh() {
        val s = _state.value
        if (s.isLoadingMore || s.state.value == null) return
        val myEpoch = ++epoch
        loadMoreJob?.cancel()
        val pages = page.coerceAtLeast(1)
        viewModelScope.launch {
            val collected = mutableListOf<Server>()
            var lastHasMore = false
            val ok = runCatching {
                for (p in 1..pages) {
                    val result = repo.list(p, query = s.query.takeIf { it.isNotBlank() }.orEmpty())
                    collected += result.items
                    lastHasMore = result.hasMore
                }
            }.isSuccess
            if (!ok || myEpoch != epoch) return@launch
            page = pages
            replaceAll(collected)
            _state.update { it.copy(state = LoadState.Loaded(accumulated.toList()), hasMore = lastHasMore) }
        }
    }

    fun loadNextPage() {
        val s = _state.value
        if (!s.hasMore || s.isLoadingMore || s.state is LoadState.Loading) return
        val myEpoch = epoch
        _state.update { it.copy(isLoadingMore = true) }
        loadMoreJob = viewModelScope.launch {
            runCatching { repo.list(page + 1, query = s.query) }
                .onSuccess { result ->
                    if (myEpoch == epoch) {
                        page += 1
                        addDistinct(result.items)
                        _state.update {
                            it.copy(
                                state = LoadState.Loaded(accumulated.toList()),
                                hasMore = result.hasMore,
                                isLoadingMore = false,
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoadingMore = false) }
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingMore = false) } }
        }
    }

    private fun reload(showLoading: Boolean) {
        val myEpoch = ++epoch
        loadMoreJob?.cancel()
        page = 1
        if (showLoading && _state.value.state.value == null) {
            _state.update { it.copy(state = LoadState.Loading) }
        }
        viewModelScope.launch {
            runCatching { repo.list(1, query = _state.value.query) }
                .onSuccess { result ->
                    if (myEpoch != epoch) {
                        _state.update { it.copy(isRefreshing = false) }
                        return@onSuccess
                    }
                    page = 1
                    replaceAll(result.items)
                    _state.update {
                        it.copy(
                            state = LoadState.Loaded(accumulated.toList()),
                            hasMore = result.hasMore,
                            isRefreshing = false,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            state = if (it.state.value == null) {
                                LoadState.Failed(t.toApiException().message)
                            } else {
                                it.state
                            },
                            isRefreshing = false,
                        )
                    }
                }
        }
    }

    private fun replaceAll(items: List<Server>) {
        accumulated.clear()
        addDistinct(items)
    }

    /** Append only ids not already present (the list key is the server id). */
    private fun addDistinct(items: List<Server>) {
        val seen = accumulated.mapTo(HashSet()) { it.id }
        for (item in items) {
            if (seen.add(item.id)) accumulated += item
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
