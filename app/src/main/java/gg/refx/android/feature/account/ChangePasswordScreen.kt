package gg.refx.android.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gg.refx.android.app.LocalAppContainer
import gg.refx.android.core.network.toApiException
import gg.refx.android.core.design.DesignTokens
import gg.refx.android.core.design.RefxPrimaryButton
import gg.refx.android.core.ui.DetailTopBar
import gg.refx.android.data.repo.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordUiState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
) {
    val canSubmit: Boolean
        get() = current.isNotBlank() && new.length >= 8 && new == confirm && !submitting
}

class ChangePasswordViewModel(private val repo: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onCurrent(v: String) = _state.update { it.copy(current = v, error = null) }
    fun onNew(v: String) = _state.update { it.copy(new = v, error = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v, error = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.changePassword(s.current, s.new) }
                .onSuccess { _state.update { it.copy(submitting = false, success = true) } }
                .onFailure { t -> _state.update { it.copy(submitting = false, error = t.toApiException().message) } }
        }
    }
}

@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: ChangePasswordViewModel = viewModel(
        factory = viewModelFactory { initializer { ChangePasswordViewModel(container.accountRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) { if (state.success) onBack() }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(title = "Change password", onBack = onBack)
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(state.current, vm::onCurrent, label = { Text("Current password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.new, vm::onNew, label = { Text("New password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.confirm, vm::onConfirm, label = { Text("Confirm new password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            if (state.error != null) {
                Text(state.error!!, color = DesignTokens.AppDestructive, style = MaterialTheme.typography.bodySmall)
            }
            RefxPrimaryButton("Update password", vm::submit, enabled = state.canSubmit, loading = state.submitting, fullWidth = true, modifier = Modifier.fillMaxWidth())
        }
    }
}
