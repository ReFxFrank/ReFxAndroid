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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import gg.refx.android.core.design.RoleBadge
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.AdminUser
import gg.refx.android.data.repo.StaffRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUsersUiState(
    val users: LoadState<List<AdminUser>> = LoadState.Loading,
    val query: String = "",
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
)

class AdminUsersViewModel(private val repo: StaffRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminUsersUiState())
    val state: StateFlow<AdminUsersUiState> = _state.asStateFlow()

    private var page = 1
    private val accumulated = mutableListOf<AdminUser>()
    private var searchJob: Job? = null

    init { load() }

    fun onQueryChange(v: String) {
        _state.update { it.copy(query = v) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(350); load() }
    }

    fun load() {
        page = 1
        if (_state.value.users.value == null) _state.update { it.copy(users = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.users(1, query = _state.value.query) }
                .onSuccess { r ->
                    page = 1; accumulated.clear(); addDistinct(r.items)
                    _state.update { it.copy(users = LoadState.Loaded(accumulated.toList()), hasMore = r.hasMore) }
                }
                .onFailure { t -> _state.update { if (it.users.value == null) it.copy(users = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun loadNextPage() {
        val s = _state.value
        if (!s.hasMore || s.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            runCatching { repo.users(page + 1, query = s.query) }
                .onSuccess { r ->
                    page += 1; addDistinct(r.items)
                    _state.update { it.copy(users = LoadState.Loaded(accumulated.toList()), hasMore = r.hasMore, isLoadingMore = false) }
                }
                .onFailure { _state.update { it.copy(isLoadingMore = false) } }
        }
    }

    private fun addDistinct(items: List<AdminUser>) {
        val seen = accumulated.mapTo(HashSet()) { it.id }
        for (item in items) if (seen.add(item.id)) accumulated += item
    }
}

@Composable
fun AdminUsersScreen(onBack: () -> Unit, onOpenUser: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: AdminUsersViewModel = viewModel(
        factory = viewModelFactory { initializer { AdminUsersViewModel(container.staffRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last -> val total = state.users.value?.size ?: 0; if (total > 0 && last >= total - 3) vm.loadNextPage() }
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Users", onBack = onBack)
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            placeholder = { Text("Search users") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        )
        AsyncState(state = state.users, isEmpty = { it.isEmpty() && state.query.isBlank() }, emptyTitle = "No users", onRetry = vm::load) { users ->
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(users, key = { it.id }) { user ->
                    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onOpenUser(user.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(user.displayName, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                                Text(user.email, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            user.state?.let { StatusChip(it, userStateColor(it)) }
                            user.globalRole?.let { RoleBadge(it.name) }
                        }
                    }
                }
            }
        }
    }
}

internal fun userStateColor(state: String) = when (state.uppercase()) {
    "ACTIVE" -> DesignTokens.AppSuccess
    "SUSPENDED" -> DesignTokens.AppWarning
    "BANNED" -> DesignTokens.AppDestructive
    else -> DesignTokens.AppMuted
}
