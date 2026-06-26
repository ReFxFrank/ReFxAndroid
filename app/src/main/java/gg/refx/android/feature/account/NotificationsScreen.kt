package gg.refx.android.feature.account

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.messaging.FirebaseMessaging
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.ManageRow
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.AppNotification
import gg.refx.android.data.repo.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class NotificationsUiState(
    val feed: LoadState<List<AppNotification>> = LoadState.Loading,
    val fcmToken: String? = null,
)

class NotificationsViewModel(private val repo: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        load()
        fetchToken()
    }

    fun load() {
        if (_state.value.feed.value == null) _state.update { it.copy(feed = LoadState.Loading) }
        viewModelScope.launch {
            runCatching { repo.notifications() }
                .onSuccess { list -> _state.update { it.copy(feed = LoadState.Loaded(list)) } }
                .onFailure { t -> _state.update { if (it.feed.value == null) it.copy(feed = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch { runCatching { repo.markRead(id) }.onSuccess { load() } }
    }

    fun markAllRead() {
        viewModelScope.launch { runCatching { repo.markAllRead() }.onSuccess { load() } }
    }

    private fun fetchToken() {
        viewModelScope.launch {
            val token = runCatching {
                suspendCancellableCoroutine { cont ->
                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }.getOrNull()
            _state.update { it.copy(fcmToken = token) }
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val vm: NotificationsViewModel = viewModel(
        factory = viewModelFactory { initializer { NotificationsViewModel(container.accountRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    fun isPostNotificationsGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    // Re-read on resume so returning from system settings reflects the new value
    // (the synchronous read alone never recomposes when the OS permission changes).
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember { mutableStateOf(isPostNotificationsGranted()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionGranted = isPostNotificationsGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(
            title = "Notifications",
            onBack = onBack,
            trailing = { TextButton(onClick = vm::markAllRead) { Text("Mark all read") } },
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Push diagnostics
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionHeader(title = "Push")
                    ManageRow(title = "Permission", trailing = {
                        Text(if (permissionGranted) "Granted" else "Not granted", color = if (permissionGranted) DesignTokens.AppSuccess else DesignTokens.AppWarning, style = MaterialTheme.typography.bodySmall)
                    })
                    ManageRow(title = "Device token", subtitle = state.fcmToken?.take(24)?.plus("…") ?: "Unavailable (no Firebase config)")
                    ManageRow(title = "Copy token", trailing = {
                        TextButton(onClick = { state.fcmToken?.let { clipboard.setText(AnnotatedString(it)) } }, enabled = state.fcmToken != null) { Text("Copy") }
                    })
                    ManageRow(title = "Re-register", trailing = {
                        TextButton(onClick = { state.fcmToken?.let { container.pushRegistrar.onNewToken(it) } }, enabled = state.fcmToken != null) { Text("Sync") }
                    })
                }
            }

            // Feed
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Recent")
                    AsyncState(
                        state = state.feed,
                        isEmpty = { it.isEmpty() },
                        emptyTitle = "No notifications",
                        onRetry = vm::load,
                        skeleton = { Text("Loading…", color = DesignTokens.AppMuted) },
                    ) { feed ->
                        feed.forEach { n -> NotificationRow(n, onClick = { if (!n.isRead) vm.markRead(n.id) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    ManageRow(
        title = n.title,
        subtitle = n.body,
        modifier = Modifier.clickable { onClick() },
        trailing = {
            if (!n.isRead) {
                Text("•", color = DesignTokens.AppPrimary, style = MaterialTheme.typography.titleLarge)
            }
        },
    )
}
