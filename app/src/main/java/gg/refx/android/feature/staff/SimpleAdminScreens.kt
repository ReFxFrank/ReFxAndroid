package gg.refx.android.feature.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import gg.refx.android.data.repo.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Generic read-only admin list backed by a suspend loader. */
class SimpleAdminListViewModel<T>(private val loader: suspend () -> List<T>) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<List<T>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<T>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.value == null) _state.value = LoadState.Loading
        viewModelScope.launch {
            runCatching { loader() }
                .onSuccess { _state.value = LoadState.Loaded(it) }
                .onFailure { t -> if (_state.value.value == null) _state.value = LoadState.Failed(t.toApiException().message) }
        }
    }
}

@Composable
private fun <T> SimpleAdminList(
    title: String,
    emptyTitle: String,
    loader: suspend () -> List<T>,
    onBack: () -> Unit,
    key: (T) -> Any,
    row: @Composable (T) -> Unit,
) {
    val vm: SimpleAdminListViewModel<T> = viewModel(factory = viewModelFactory { initializer { SimpleAdminListViewModel(loader) } })
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = title, onBack = onBack)
        AsyncState(state = state, isEmpty = { it.isEmpty() }, emptyTitle = emptyTitle, onRetry = vm::load) { items ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { key(it) }) { item -> GlassCard(modifier = Modifier.fillMaxWidth()) { row(item) } }
            }
        }
    }
}

@Composable
fun CouponsScreen(onBack: () -> Unit) {
    val repo = LocalAppContainer.current.staffRepository
    SimpleAdminList("Coupons", "No coupons", { repo.coupons() }, onBack, { it.id }) { coupon ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(coupon.code, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                Text("${coupon.valueLabel} · ${coupon.timesRedeemed} used", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
            }
            StatusChip(if (coupon.isActive) "Active" else "Inactive", if (coupon.isActive) DesignTokens.AppSuccess else DesignTokens.AppMuted)
        }
    }
}

@Composable
fun GiftCardsScreen(onBack: () -> Unit) {
    val repo = LocalAppContainer.current.staffRepository
    SimpleAdminList("Gift cards", "No gift cards", { repo.giftCards() }, onBack, { it.id }) { card ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(card.code, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                Text("${card.balance.formatted} of ${card.initialBalance.formatted}", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
            }
            StatusChip(if (card.isActive) "Active" else "Inactive", if (card.isActive) DesignTokens.AppSuccess else DesignTokens.AppMuted)
        }
    }
}

@Composable
fun LocationsScreen(onBack: () -> Unit) {
    val repo = LocalAppContainer.current.staffRepository
    SimpleAdminList("Locations", "No locations", { repo.locations() }, onBack, { it.id }) { region ->
        Column {
            Text(region.name, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
            Text(listOfNotNull(region.code, region.country).joinToString(" · "), color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RolesScreen(onBack: () -> Unit) {
    val repo = LocalAppContainer.current.staffRepository
    SimpleAdminList("Roles", "No roles", { repo.roles() }, onBack, { it.id }) { role ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(role.name, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                Text("${role.permissions.size} permissions · ${role.count?.users ?: 0} users", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (role.isSystem) StatusChip("System", DesignTokens.AppMuted)
        }
    }
}
