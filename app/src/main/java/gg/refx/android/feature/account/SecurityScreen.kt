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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.GlassCard
import gg.refx.android.core.design.RefxDestructiveButton
import gg.refx.android.core.design.RefxPrimaryButton
import gg.refx.android.core.design.RefxSecondaryButton
import gg.refx.android.core.design.SectionHeader
import gg.refx.android.core.ui.AsyncState
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.model.ApiKey
import gg.refx.android.data.model.CreatedApiKey
import gg.refx.android.data.model.TotpEnrollment
import gg.refx.android.data.repo.AccountRepository
import gg.refx.android.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityUiState(
    val totpEnabled: Boolean = false,
    val enrollment: TotpEnrollment? = null,
    val verifyCode: String = "",
    val recoveryCodes: List<String>? = null,
    val apiKeys: LoadState<List<ApiKey>> = LoadState.Loading,
    val createdKey: CreatedApiKey? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

class SecurityViewModel(
    private val repo: AccountRepository,
    session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityUiState(totpEnabled = session.account?.isTotpEnabled == true))
    val state: StateFlow<SecurityUiState> = _state.asStateFlow()

    init { loadKeys() }

    fun loadKeys() {
        viewModelScope.launch {
            runCatching { repo.apiKeys() }
                .onSuccess { keys -> _state.update { it.copy(apiKeys = LoadState.Loaded(keys)) } }
                .onFailure { t -> _state.update { if (it.apiKeys.value == null) it.copy(apiKeys = LoadState.Failed(t.toApiException().message)) else it } }
        }
    }

    fun enroll() {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.enrollTotp() }
                .onSuccess { e -> _state.update { it.copy(busy = false, enrollment = e) } }
                .onFailure { t -> _state.update { it.copy(busy = false, error = t.toApiException().message) } }
        }
    }

    fun onVerifyCode(v: String) = _state.update { it.copy(verifyCode = v.filter(Char::isDigit).take(8), error = null) }

    fun verify() {
        val code = _state.value.verifyCode
        if (code.length < 6) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.verifyTotp(code) }
                .onSuccess { rc -> _state.update { it.copy(busy = false, totpEnabled = true, enrollment = null, verifyCode = "", recoveryCodes = rc.codes) } }
                .onFailure { t -> _state.update { it.copy(busy = false, error = t.toApiException().message) } }
        }
    }

    fun disable() {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.disableTotp() }
                .onSuccess { _state.update { it.copy(busy = false, totpEnabled = false, recoveryCodes = null) } }
                .onFailure { t -> _state.update { it.copy(busy = false, error = t.toApiException().message) } }
        }
    }

    fun createKey(name: String) {
        viewModelScope.launch {
            runCatching { repo.createApiKey(name, emptyList()) }
                .onSuccess { created -> _state.update { it.copy(createdKey = created) }; loadKeys() }
                .onFailure { t -> _state.update { it.copy(error = t.toApiException().message) } }
        }
    }

    fun revokeKey(id: String) {
        viewModelScope.launch { runCatching { repo.revokeApiKey(id) }.onSuccess { loadKeys() } }
    }

    fun dismissCreatedKey() = _state.update { it.copy(createdKey = null) }
    fun dismissRecovery() = _state.update { it.copy(recoveryCodes = null) }
}

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val clipboard = LocalClipboardManager.current
    val vm: SecurityViewModel = viewModel(
        factory = viewModelFactory { initializer { SecurityViewModel(container.accountRepository, container.session) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var newKeyName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Security", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.error?.let { Text(it, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall) }

            // Two-factor
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Two-factor authentication")
                    when {
                        state.totpEnabled && state.enrollment == null -> {
                            Text("2FA is enabled.", color = DesignTokens.AppSuccess, style = MaterialTheme.typography.bodyMedium)
                            RefxDestructiveButton("Disable 2FA", vm::disable, loading = state.busy, fullWidth = true)
                        }
                        state.enrollment != null -> {
                            val e = state.enrollment!!
                            Text("Add this secret to your authenticator app, then enter the 6-digit code.", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                            e.secret?.let { secret ->
                                Text(secret, fontFamily = FontFamily.Monospace, color = DesignTokens.AppForegroundStrong, modifier = Modifier.fillMaxWidth())
                                RefxSecondaryButton("Copy secret", { clipboard.setText(AnnotatedString(secret)) }, fullWidth = true)
                            }
                            OutlinedTextField(state.verifyCode, vm::onVerifyCode, label = { Text("Authentication code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            RefxPrimaryButton("Verify & enable", vm::verify, enabled = state.verifyCode.length >= 6 && !state.busy, loading = state.busy, fullWidth = true)
                        }
                        else -> {
                            Text("Protect your account with an authenticator app.", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                            RefxPrimaryButton("Enable 2FA", vm::enroll, loading = state.busy, fullWidth = true)
                        }
                    }
                }
            }

            // API keys
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "API keys")
                    AsyncState(
                        state = state.apiKeys,
                        isEmpty = { it.isEmpty() },
                        emptyTitle = "No API keys",
                        onRetry = vm::loadKeys,
                        skeleton = { Text("Loading…", color = DesignTokens.AppMuted) },
                    ) { keys ->
                        keys.forEach { key ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(key.name, color = DesignTokens.AppForeground, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { vm.revokeKey(key.id) }) { Text("Revoke", color = DesignTokens.AppDestructive) }
                            }
                        }
                    }
                    RefxSecondaryButton("Create API key", { showCreate = true }, fullWidth = true)
                }
            }
        }
    }

    // Create-key dialog
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New API key") },
            text = {
                OutlinedTextField(newKeyName, { newKeyName = it }, label = { Text("Name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newKeyName.isNotBlank()) { vm.createKey(newKeyName); newKeyName = ""; showCreate = false }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }

    // One-time secret reveal
    state.createdKey?.let { created ->
        AlertDialog(
            onDismissRequest = vm::dismissCreatedKey,
            title = { Text("API key created") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copy this now — it can't be retrieved later.", color = DesignTokens.AppWarning, style = MaterialTheme.typography.bodySmall)
                    Text(created.plaintext ?: "(no secret returned)", fontFamily = FontFamily.Monospace, color = DesignTokens.AppForegroundStrong)
                }
            },
            confirmButton = {
                TextButton(onClick = { created.plaintext?.let { clipboard.setText(AnnotatedString(it)) }; vm.dismissCreatedKey() }) { Text("Copy & close") }
            },
        )
    }

    // Recovery codes after enabling 2FA
    state.recoveryCodes?.let { codes ->
        AlertDialog(
            onDismissRequest = vm::dismissRecovery,
            title = { Text("Recovery codes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Store these safely. Each can be used once.", color = DesignTokens.AppMuted, style = MaterialTheme.typography.bodySmall)
                    codes.forEach { Text(it, fontFamily = FontFamily.Monospace, color = DesignTokens.AppForegroundStrong) }
                }
            },
            confirmButton = {
                TextButton(onClick = { clipboard.setText(AnnotatedString(codes.joinToString("\n"))); vm.dismissRecovery() }) { Text("Copy & close") }
            },
        )
    }
}
