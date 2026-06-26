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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import gg.refx.android.core.design.RefxSecondaryButton
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.design.StatusChip
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.AdminUserDetail
import gg.refx.android.data.model.CreditReason
import gg.refx.android.data.model.UserRole
import gg.refx.android.data.repo.StaffRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

data class AdminUserDetailUiState(
    val user: LoadState<AdminUserDetail> = LoadState.Loading,
    val busy: Boolean = false,
    val error: String? = null,
)

class AdminUserDetailViewModel(private val userId: String, private val repo: StaffRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminUserDetailUiState())
    val state: StateFlow<AdminUserDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(user = if (it.user.value == null) LoadState.Loading else it.user) }
        viewModelScope.launch {
            runCatching { repo.user(userId) }
                .onSuccess { d -> _state.update { it.copy(user = LoadState.Loaded(d)) } }
                .onFailure { t -> _state.update { if (it.user.value == null) it.copy(user = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun setState(state: String) = mutate { repo.setUserState(userId, state) }
    fun setRole(role: String) = mutate { repo.setUserRole(userId, role) }
    fun verifyEmail() = mutate { repo.verifyEmail(userId) }
    fun grantCredit(amountMinor: Long, reason: CreditReason, note: String?) =
        mutate { repo.grantCredit(userId, amountMinor, reason, note?.takeIf { it.isNotBlank() }) }

    private fun mutate(action: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { _state.update { it.copy(busy = false) }; load() }
                .onFailure { t -> _state.update { it.copy(busy = false, error = t.toApiException().message) } }
        }
    }
}

@Composable
fun AdminUserDetailScreen(userId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: AdminUserDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { AdminUserDetailViewModel(userId, container.staffRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showCredit by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = state.user.value?.displayName ?: "User", onBack = onBack)
        AsyncState(state = state.user, onRetry = vm::load) { user ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(user.email, color = DesignTokens.AppForegroundStrong, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            user.state?.let { StatusChip(it, userStateColor(it)) }
                            user.globalRole?.let { StatusChip(it.name, DesignTokens.AppPrimary) }
                            StatusChip(if (user.emailVerified) "Verified" else "Unverified", if (user.emailVerified) DesignTokens.AppSuccess else DesignTokens.AppWarning)
                        }
                        user.counts?.let { c ->
                            Text("${c.ownedServers ?: 0} servers · ${c.subscriptions ?: 0} subs · ${c.tickets ?: 0} tickets", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Account state
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(title = "Account state")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ACTIVE", "SUSPENDED", "BANNED").forEach { s ->
                                RefxSecondaryButton(text = s.lowercase().replaceFirstChar { it.uppercase() }, onClick = { vm.setState(s) }, enabled = !state.busy && user.state?.uppercase() != s)
                            }
                        }
                        if (!user.emailVerified) {
                            RefxSecondaryButton("Verify email", { vm.verifyEmail() }, enabled = !state.busy, fullWidth = true)
                        }
                    }
                }

                // Role
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(title = "Role")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(UserRole.CUSTOMER, UserRole.SUPPORT, UserRole.ADMIN, UserRole.OWNER).forEach { r ->
                                TextButton(onClick = { vm.setRole(r.raw) }, enabled = !state.busy && user.globalRole != r) {
                                    Text(r.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (user.globalRole == r) DesignTokens.AppPrimary else DesignTokens.AppMuted)
                                }
                            }
                        }
                    }
                }

                // Credit
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(title = "Store credit")
                        RefxSecondaryButton("Adjust credit", { showCredit = true }, enabled = !state.busy, fullWidth = true)
                    }
                }
            }
        }
    }

    if (showCredit) {
        AdjustCreditDialog(
            onDismiss = { showCredit = false },
            onSubmit = { minor, reason, note -> vm.grantCredit(minor, reason, note); showCredit = false },
        )
    }
}

@Composable
private fun AdjustCreditDialog(onDismiss: () -> Unit, onSubmit: (Long, CreditReason, String?) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf(CreditReason.ADMIN_GRANT) }
    val amountMinor = remember(amount) { (amount.toDoubleOrNull()?.times(100))?.roundToLong() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust store credit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Use a negative amount to deduct.", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount (e.g. 5.00 or -2.50)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(CreditReason.ADMIN_GRANT, CreditReason.REFUND, CreditReason.ADJUSTMENT).forEach { r ->
                        TextButton(onClick = { reason = r }) {
                            Text(r.name.lowercase().replace('_', ' '), color = if (reason == r) DesignTokens.AppPrimary else DesignTokens.AppMuted)
                        }
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = amountMinor != null && amountMinor != 0L,
                onClick = { amountMinor?.let { onSubmit(it, reason, note) } },
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
