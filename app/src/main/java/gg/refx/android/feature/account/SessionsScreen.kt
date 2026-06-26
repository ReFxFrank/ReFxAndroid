package gg.refx.android.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.UserSession
import gg.refx.android.data.repo.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionsViewModel(private val repo: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<List<UserSession>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<UserSession>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.value == null) _state.value = LoadState.Loading
        viewModelScope.launch {
            runCatching { repo.sessions() }
                .onSuccess { _state.value = LoadState.Loaded(it) }
                .onFailure { t -> if (_state.value.value == null) _state.value = LoadState.Failed(t.toApiException().message) }
        }
    }

    fun revoke(id: String) {
        viewModelScope.launch {
            runCatching { repo.revokeSession(id) }.onSuccess { load() }
        }
    }
}

@Composable
fun SessionsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SessionsViewModel = viewModel(
        factory = viewModelFactory { initializer { SessionsViewModel(container.accountRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Active sessions", onBack = onBack)
        AsyncState(state = state, isEmpty = { it.isEmpty() }, emptyTitle = "No active sessions", onRetry = vm::load) { sessions ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sessions.forEach { s -> SessionRow(s, onRevoke = { vm.revoke(s.id) }) }
            }
        }
    }
}

@Composable
private fun SessionRow(session: UserSession, onRevoke: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(session.userAgent ?: "Unknown device", color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.bodyMedium)
                Text(session.ipAddress ?: "—", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (session.current) {
                StatusChip("This device", DesignTokens.AppSuccess)
            } else {
                TextButton(onClick = onRevoke) { Text("Revoke", color = DesignTokens.AppDestructive) }
            }
        }
    }
}
