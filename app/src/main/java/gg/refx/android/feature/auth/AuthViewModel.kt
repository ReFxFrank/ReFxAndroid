package gg.refx.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gg.refx.android.core.network.toApiException
import gg.refx.android.data.model.MFAMethod
import gg.refx.android.data.repo.AuthRepository
import gg.refx.android.data.repo.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStage { Login, TwoFactor }

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val totp: String = "",
    val stage: AuthStage = AuthStage.Login,
    val loading: Boolean = false,
    val error: String? = null,
    val mfaToken: String? = null,
    val methods: List<MFAMethod> = emptyList(),
) {
    val canSubmitLogin: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !loading
    val canSubmitTotp: Boolean
        get() = totp.trim().length >= 6 && !loading && mfaToken != null
}

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onTotpChange(value: String) = _state.update { it.copy(totp = value.filter(Char::isDigit).take(8), error = null) }

    fun backToLogin() = _state.update {
        it.copy(stage = AuthStage.Login, totp = "", error = null, mfaToken = null, methods = emptyList())
    }

    fun login() {
        val current = _state.value
        if (!current.canSubmitLogin) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.login(current.email, current.password) }
                .onSuccess(::handleResult)
                .onFailure(::handleError)
        }
    }

    fun verifyTotp() {
        val current = _state.value
        val token = current.mfaToken
        if (!current.canSubmitTotp || token == null) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.verifyMfa(current.totp, token, MFAMethod.TOTP) }
                .onSuccess(::handleResult)
                .onFailure(::handleError)
        }
    }

    private fun handleResult(result: LoginResult) {
        when (result) {
            is LoginResult.Success -> {
                // SessionManager flips to SignedIn; RootContent swaps to the shell.
                _state.update { it.copy(loading = false) }
            }
            is LoginResult.MfaRequired -> _state.update {
                it.copy(
                    loading = false,
                    stage = AuthStage.TwoFactor,
                    mfaToken = result.mfaToken,
                    methods = result.methods,
                )
            }
        }
    }

    private fun handleError(t: Throwable) {
        _state.update { it.copy(loading = false, error = t.toApiException().message) }
    }
}
