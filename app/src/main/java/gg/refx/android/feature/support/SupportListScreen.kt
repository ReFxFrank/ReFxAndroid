package gg.refx.android.feature.support

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.Eyebrow
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.data.model.Ticket
import gg.refx.android.data.model.TicketPriority
import gg.refx.android.data.model.TicketState
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportListScreen(onOpenTicket: (String) -> Unit, onCreate: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SupportListViewModel = viewModel(
        factory = viewModelFactory { initializer { SupportListViewModel(container.supportRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last ->
                val total = state.state.value?.size ?: 0
                if (total > 0 && last >= total - 3) vm.loadNextPage()
            }
    }

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
            AsyncState(
                state = state.state,
                isEmpty = { it.isEmpty() },
                emptyTitle = "No tickets",
                emptyMessage = "Need help? Open a ticket and our team will respond.",
                onRetry = vm::load,
            ) { tickets ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Eyebrow("Help", modifier = Modifier.padding(top = 8.dp))
                            Text("Support", style = MaterialTheme.typography.headlineMedium, color = DesignTokens.AppForegroundStrong)
                        }
                    }
                    items(tickets, key = { it.id }) { ticket ->
                        TicketRow(ticket = ticket, onClick = { onOpenTicket(ticket.id) })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreate,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = DesignTokens.AppPrimary,
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "New ticket")
        }
    }
}

@Composable
private fun TicketRow(ticket: Ticket, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ticket.subject,
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignTokens.AppForegroundStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("#${ticket.number}", style = MaterialTheme.typography.bodySmall, color = DesignTokens.AppMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val (sLabel, sColor) = ticketStateColor(ticket.state)
                StatusChip(text = sLabel, color = sColor)
                val (pLabel, pColor) = ticketPriorityColor(ticket.priority)
                StatusChip(text = pLabel, color = pColor)
            }
        }
    }
}

private fun ticketStateColor(state: TicketState) = when (state) {
    TicketState.OPEN, TicketState.PENDING_AGENT -> "Open" to DesignTokens.AppPrimary
    TicketState.PENDING_CUSTOMER -> "Awaiting you" to DesignTokens.AppWarning
    TicketState.RESOLVED -> "Resolved" to DesignTokens.AppSuccess
    TicketState.CLOSED, TicketState.ARCHIVED -> "Closed" to DesignTokens.AppMuted
    TicketState.UNKNOWN -> "—" to DesignTokens.AppMuted
}

private fun ticketPriorityColor(priority: TicketPriority) = when (priority) {
    TicketPriority.URGENT -> "Urgent" to DesignTokens.AppDestructive
    TicketPriority.HIGH -> "High" to DesignTokens.AppWarning
    TicketPriority.NORMAL -> "Normal" to DesignTokens.AppMuted
    TicketPriority.LOW -> "Low" to DesignTokens.AppMuted
    TicketPriority.UNKNOWN -> "—" to DesignTokens.AppMuted
}
