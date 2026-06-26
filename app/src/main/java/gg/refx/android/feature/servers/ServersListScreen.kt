package gg.refx.android.feature.servers

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.Eyebrow
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.StatePill
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.WebLink
import gg.refx.android.data.model.Server
import gg.refx.android.data.model.StateColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private const val POLL_INTERVAL_MS = 12_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersListScreen(onOpenServer: (String) -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm: ServersListViewModel = viewModel(
        factory = viewModelFactory { initializer { ServersListViewModel(container.serversRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Periodic refresh while visible (parity spec §8: 12s poll). repeatOnLifecycle
    // pauses the loop when the app drops below STARTED (backgrounded) and resumes it
    // on return — "while visible", not merely "while composed".
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(POLL_INTERVAL_MS)
                vm.silentRefresh()
            }
        }
    }

    // Infinite scroll: load the next page as the last item approaches.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val total = state.state.value?.size ?: 0
                if (total > 0 && lastVisible >= total - 3) vm.loadNextPage()
            }
    }

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            AsyncState(
                state = state.state,
                isEmpty = { it.isEmpty() && state.query.isBlank() },
                emptyTitle = "No servers yet",
                emptyMessage = "Servers you own or help manage will appear here.",
                onRetry = vm::refresh,
            ) { servers ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Eyebrow("Your fleet", modifier = Modifier.padding(top = 8.dp))
                            Text(
                                text = "Servers",
                                style = MaterialTheme.typography.headlineMedium,
                                color = DesignTokens.AppForegroundStrong,
                            )
                            SearchField(query = state.query, onQueryChange = vm::onQueryChange)
                            if (state.attentionCount > 0) {
                                AttentionBanner(count = state.attentionCount)
                            }
                        }
                    }

                    items(servers, key = { it.id }) { server ->
                        ServerRow(server = server, onClick = { onOpenServer(server.id) })
                    }

                    if (state.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = DesignTokens.AppPrimary)
                            }
                        }
                    }
                }
            }
        }

        // New-server "+" gated by the purchasing flag (Play compliance §8): hidden on prod.
        if (container.purchasingEnabled) {
            FloatingActionButton(
                onClick = { WebLink.open(context, container.config.webOrigin) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                containerColor = DesignTokens.AppPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New server")
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Search servers") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
    )
}

@Composable
private fun AttentionBanner(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) {},
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$count server${if (count == 1) "" else "s"} need attention",
                color = DesignTokens.AppWarning,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ServerRow(server: Server, onClick: () -> Unit) {
    val (label, color) = StateColors.server(server.state)
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignTokens.AppForegroundStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = server.connectionString ?: server.gameName,
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignTokens.AppMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatePill(label = label, color = color)
        }
    }
}
