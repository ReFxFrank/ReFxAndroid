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
 */
class ServersListViewModel(private val repo: ServersRepository) : ViewModel() {

    private val _state = MutableStateFlow(ServersListUiState())
    val state: StateFlow<ServersListUiState> = _state.asStateFlow()

    private var page = 1
    private val accumulated = mutableListOf<Server>()
    private var searchJob: Job? = null

    init {
        load(reset = true, showLoading = true)
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load(reset = true, showLoading = true)
        }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        load(reset = true, showLoading = false)
    }

    /** Silent periodic refresh (no spinner) — driven by the screen every 12s. */
    fun silentRefresh() {
        if (_state.value.isLoadingMore) return
        load(reset = true, showLoading = false)
    }

    fun loadNextPage() {
        val s = _state.value
        if (!s.hasMore || s.isLoadingMore || s.state is LoadState.Loading) return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            runCatching { repo.list(page + 1, query = s.query) }
                .onSuccess { result ->
                    page += 1
                    accumulated += result.items
                    _state.update {
                        it.copy(
                            state = LoadState.Loaded(accumulated.toList()),
                            hasMore = result.hasMore,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingMore = false) } }
        }
    }

    private fun load(reset: Boolean, showLoading: Boolean) {
        if (reset) page = 1
        if (showLoading && _state.value.state.value == null) {
            _state.update { it.copy(state = LoadState.Loading) }
        }
        viewModelScope.launch {
            runCatching { repo.list(1, query = _state.value.query) }
                .onSuccess { result ->
                    page = 1
                    accumulated.clear()
                    accumulated += result.items
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

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
