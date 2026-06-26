package gg.refx.android.feature.staff

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import gg.refx.android.core.design.StatCard
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.AdminMetrics
import gg.refx.android.data.model.BillingSummary
import gg.refx.android.data.repo.StaffRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OverviewUiState(
    val metrics: LoadState<AdminMetrics> = LoadState.Loading,
    val billing: LoadState<BillingSummary> = LoadState.Loading,
)

class OverviewViewModel(private val repo: StaffRepository) : ViewModel() {
    private val _state = MutableStateFlow(OverviewUiState())
    val state: StateFlow<OverviewUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val m = async { runCatching { repo.metrics() } }
            val b = async { runCatching { repo.billingSummary() } }
            fun <T> Result<T>.toLoad(): LoadState<T> = fold({ LoadState.Loaded(it) }, { LoadState.Failed(it.toApiException().message) })
            _state.update { it.copy(metrics = m.await().toLoad(), billing = b.await().toLoad()) }
        }
    }
}

@Composable
fun OverviewScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: OverviewViewModel = viewModel(
        factory = viewModelFactory { initializer { OverviewViewModel(container.staffRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Overview", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncState(state = state.metrics, onRetry = vm::load, skeleton = { Text("Loading…", color = DesignTokens.AppMuted) }) { metrics ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Users", metrics.totals.users.toString(), modifier = Modifier.weight(1f))
                    StatCard("Servers", metrics.totals.servers.toString(), modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Nodes online", metrics.totals.nodesOnline.toString(), modifier = Modifier.weight(1f))
                    StatCard("Open tickets", metrics.totals.openTickets.toString(), modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Active subs", metrics.totals.activeSubscriptions.toString(), modifier = Modifier.weight(1f))
                    StatCard("MRR", metrics.totals.mrr.formatted, modifier = Modifier.weight(1f))
                }
            }

            AsyncState(state = state.billing, onRetry = vm::load, skeleton = { Text("Loading…", color = DesignTokens.AppMuted) }) { billing ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionHeader(title = "Billing")
                        SummaryRow("Revenue", billing.revenue.formatted)
                        SummaryRow("Outstanding", billing.outstanding.formatted)
                        SummaryRow("Open invoices", billing.openInvoices.toString())
                        SummaryRow("Paid invoices", billing.paidInvoices.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = DesignTokens.AppForeground, style = MaterialTheme.typography.bodyMedium)
    }
}
